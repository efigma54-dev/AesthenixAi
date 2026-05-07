package com.aicode.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

/**
 * GitHub App authentication service.
 *
 * Flow:
 *   1. Load RSA private key (.pem) from file or env var
 *   2. Sign a short-lived JWT (10 min) with App ID as issuer
 *   3. Exchange JWT for an installation access token (1 hour)
 *   4. Cache installation token; refresh before expiry
 *
 * This token is used for:
 *   - POST /repos/{owner}/{repo}/check-runs  (Checks tab)
 *   - POST /repos/{owner}/{repo}/check-runs/{id} (update)
 *   - All annotation APIs
 *
 * Personal token (PAT) is still used for:
 *   - PR comments (issues API)
 *   - Fetching raw file content
 */
@Service
public class GitHubAppAuthService {

    private static final Logger log = LoggerFactory.getLogger(GitHubAppAuthService.class);
    private static final String GH_API = "https://api.github.com";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper       = new ObjectMapper();

    // Installation token cache: installationId → CachedToken
    // Bounded at 500 entries; tokens expire after 55 min (GitHub issues 1-hour tokens)
    private final Cache<Long, CachedToken> tokenCache = Caffeine.newBuilder()
        .maximumSize(500)
        .expireAfterWrite(Duration.ofMinutes(55))
        .build();

    // Installation ID cache: "owner/repo" → installationId
    // Bounded at 1000 repos; IDs are stable so 6-hour TTL is safe
    private final Cache<String, Long> installationIdCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(Duration.ofHours(6))
        .build();

    @Value("${github.app-id:}")
    private String appId;

    @Value("${github.private-key:}")
    private String privateKeyPem;          // inline PEM from env var

    @Value("${github.private-key-path:}")
    private String privateKeyPath;         // path to .pem file

    private PrivateKey privateKey;
    private boolean    appEnabled = false;

    @PostConstruct
    public void init() {
        if (appId == null || appId.isBlank()) {
            log.info("GitHubAppAuthService: no App ID configured — GitHub App auth disabled");
            return;
        }
        try {
            privateKey = loadPrivateKey();
            appEnabled = true;
            log.info("GitHubAppAuthService: initialized — appId={}", appId);
        } catch (Exception e) {
            log.warn("GitHubAppAuthService: failed to load private key — App auth disabled: {}", e.getMessage());
        }
    }

    public boolean isEnabled() { return appEnabled; }

    // ── JWT generation ─────────────────────────────────────────

    /**
     * Generates a signed JWT valid for 9 minutes (GitHub max is 10).
     * Used to authenticate as the GitHub App itself.
     */
    public String generateJwt() {
        if (!appEnabled) throw new IllegalStateException("GitHub App not configured");

        Instant now = Instant.now();
        return Jwts.builder()
            .issuer(appId)
            .issuedAt(Date.from(now.minusSeconds(60)))   // 60s clock skew buffer
            .expiration(Date.from(now.plusSeconds(540)))  // 9 minutes
            .signWith(privateKey)
            .compact();
    }

    // ── Installation token ─────────────────────────────────────

    /**
     * Returns a valid installation access token for the given installation ID.
     * Caches tokens and refreshes them 5 minutes before expiry.
     */
    public String getInstallationToken(long installationId) {
        CachedToken cached = tokenCache.getIfPresent(installationId);
        if (cached != null && cached.isValid()) {
            log.debug("Using cached installation token for installation {}", installationId);
            return cached.token;
        }

        log.info("Fetching new installation token for installation {}", installationId);
        String token = fetchInstallationToken(installationId);
        tokenCache.put(installationId, new CachedToken(token, Instant.now().plusSeconds(55 * 60)));
        return token;
    }

    /**
     * Returns a valid installation token for the repo.
     * On 401, invalidates both caches and retries once — handles token rotation.
     */
    public String getInstallationTokenForRepo(String owner, String repo) {
        try {
            long installationId = resolveInstallationId(owner, repo);
            return getInstallationToken(installationId);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("401 on installation token for {}/{} — invalidating cache and retrying", owner, repo);
            installationIdCache.invalidate(owner + "/" + repo);
            long freshId = resolveInstallationId(owner, repo);
            tokenCache.invalidate(freshId);
            return getInstallationToken(freshId);
        }
    }

    // ── GitHub API calls ───────────────────────────────────────

    private long resolveInstallationId(String owner, String repo) {
        String key = owner + "/" + repo;
        Long cached = installationIdCache.getIfPresent(key);
        if (cached != null) {
            log.debug("Using cached installation ID {} for {}", cached, key);
            return cached;
        }
        try {
            String url = String.format("%s/repos/%s/%s/installation", GH_API, owner, repo);
            ResponseEntity<String> res = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(jwtHeaders()), String.class);
            JsonNode node = mapper.readTree(res.getBody());
            long id = node.path("id").asLong();
            installationIdCache.put(key, id);
            log.info("Resolved and cached installation ID {} for {}", id, key);
            return id;
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve installation ID for " + owner + "/" + repo, e);
        }
    }

    private String fetchInstallationToken(long installationId) {
        try {
            String url = String.format("%s/app/installations/%d/access_tokens", GH_API, installationId);
            ResponseEntity<String> res = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>("{}", jwtHeaders()), String.class);
            JsonNode node = mapper.readTree(res.getBody());
            String token = node.path("token").asText();
            if (token.isBlank()) throw new RuntimeException("Empty token in response");
            log.info("Installation token obtained for installation {}", installationId);
            return token;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch installation token: " + e.getMessage(), e);
        }
    }

    // ── Headers ────────────────────────────────────────────────

    public HttpHeaders jwtHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.set("Authorization",  "Bearer " + generateJwt());
        h.set("Accept",         "application/vnd.github.v3+json");
        h.set("User-Agent",     "AESTHENIXAI-Bot/1.0");
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    public HttpHeaders installationHeaders(String owner, String repo) {
        HttpHeaders h = new HttpHeaders();
        h.set("Authorization",  "token " + getInstallationTokenForRepo(owner, repo));
        h.set("Accept",         "application/vnd.github.v3+json");
        h.set("User-Agent",     "AESTHENIXAI-Bot/1.0");
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    // ── Private key loading ────────────────────────────────────

    private PrivateKey loadPrivateKey() throws Exception {
        String pem = null;

        // 1. Try inline env var first
        if (privateKeyPem != null && !privateKeyPem.isBlank()) {
            pem = privateKeyPem;
            log.info("Loading GitHub App private key from env var");
        }
        // 2. Try file path
        else if (privateKeyPath != null && !privateKeyPath.isBlank()) {
            String path = privateKeyPath.replace("classpath:", "");
            // Try as absolute/relative file path first
            Path filePath = Path.of(path);
            if (Files.exists(filePath)) {
                pem = Files.readString(filePath, StandardCharsets.UTF_8);
                log.info("Loading GitHub App private key from file: {}", filePath);
            } else {
                // Try classpath resource
                try (var stream = getClass().getClassLoader().getResourceAsStream(path)) {
                    if (stream != null) {
                        pem = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                        log.info("Loading GitHub App private key from classpath: {}", path);
                    }
                }
            }
        }

        if (pem == null || pem.isBlank()) {
            throw new IllegalStateException("No private key found — set GITHUB_PRIVATE_KEY or GITHUB_PRIVATE_KEY_PATH");
        }

        return parsePem(pem);
    }

    /**
     * Parses a PKCS#8 or PKCS#1 RSA private key from PEM format.
     * GitHub generates PKCS#1 keys; we convert them to PKCS#8 for Java.
     */
    private PrivateKey parsePem(String pem) throws Exception {
        // Strip PEM headers and whitespace
        String cleaned = pem
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s+", "");

        byte[] keyBytes = Base64.getDecoder().decode(cleaned);

        // Try PKCS#8 first (standard Java format)
        try {
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            // Fall back to PKCS#1 → wrap in PKCS#8 envelope
            log.debug("PKCS#8 parse failed, trying PKCS#1 conversion");
            byte[] pkcs8 = wrapPkcs1InPkcs8(keyBytes);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(pkcs8);
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        }
    }

    /**
     * Wraps a PKCS#1 RSA key in a PKCS#8 DER envelope.
     * GitHub App private keys are PKCS#1 by default.
     */
    private byte[] wrapPkcs1InPkcs8(byte[] pkcs1) {
        // PKCS#8 header for RSA (OID 1.2.840.113549.1.1.1)
        byte[] pkcs8Header = {
            0x30, (byte) 0x82, 0, 0,   // SEQUENCE (length filled below)
            0x02, 0x01, 0x00,           // INTEGER 0 (version)
            0x30, 0x0d,                 // SEQUENCE (algorithm)
            0x06, 0x09,                 // OID
            0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x01, 0x01,
            0x05, 0x00,                 // NULL
            0x04, (byte) 0x82, 0, 0    // OCTET STRING (length filled below)
        };

        int totalLen = pkcs8Header.length - 4 + pkcs1.length;
        pkcs8Header[2] = (byte) ((totalLen >> 8) & 0xff);
        pkcs8Header[3] = (byte) (totalLen & 0xff);
        pkcs8Header[pkcs8Header.length - 2] = (byte) ((pkcs1.length >> 8) & 0xff);
        pkcs8Header[pkcs8Header.length - 1] = (byte) (pkcs1.length & 0xff);

        byte[] result = new byte[pkcs8Header.length + pkcs1.length];
        System.arraycopy(pkcs8Header, 0, result, 0, pkcs8Header.length);
        System.arraycopy(pkcs1, 0, result, pkcs8Header.length, pkcs1.length);
        return result;
    }

    // ── Cache record ───────────────────────────────────────────

    private record CachedToken(String token, Instant expiresAt) {
        boolean isValid() {
            return token != null && !token.isBlank()
                && Instant.now().isBefore(expiresAt.minusSeconds(300)); // 5 min buffer
        }
    }
}
