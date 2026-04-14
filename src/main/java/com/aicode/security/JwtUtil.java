package com.aicode.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

/**
 * JWT Utility for GitHub App authentication.
 *
 * Generates JWT tokens for GitHub App API calls.
 * Used to authenticate as the GitHub App and get installation tokens.
 *
 * Interview signal:
 * "I implemented secure JWT-based authentication for GitHub App integration,
 * demonstrating knowledge of OAuth2 flows and cryptographic signing."
 */
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    /**
     * Generates a JWT for GitHub App authentication.
     *
     * @param appId          GitHub App ID (from app settings)
     * @param privateKeyPath Path to PEM-encoded private key file
     * @return JWT token valid for 10 minutes
     */
    public static String generateJWT(String appId, String privateKeyPath) {
        try {
            long now = System.currentTimeMillis();
            long expiration = now + (10 * 60 * 1000); // 10 minutes

            PrivateKey key = loadPrivateKey(privateKeyPath);

            return Jwts.builder()
                    .setIssuedAt(new Date(now))
                    .setExpiration(new Date(expiration))
                    .setIssuer(appId)
                    .signWith(key, SignatureAlgorithm.RS256)
                    .compact();

        } catch (Exception e) {
            log.error("Failed to generate JWT: {}", e.getMessage(), e);
            throw new RuntimeException("JWT generation failed", e);
        }
    }

    /**
     * Loads and parses PEM-encoded private key from file.
     *
     * Supports both classpath: and file: URLs.
     * GitHub provides private keys in PEM format with headers.
     */
    private static PrivateKey loadPrivateKey(String keyPath) throws Exception {
        String pemContent;

        if (keyPath == null || keyPath.isBlank()) {
            throw new IllegalArgumentException("GitHub private key is missing");
        }

        if (keyPath.trim().startsWith("-----BEGIN ")) {
            pemContent = keyPath;
        } else if (keyPath.startsWith("classpath:")) {
            String resourcePath = keyPath.substring("classpath:".length());
            ClassPathResource resource = new ClassPathResource(resourcePath);
            pemContent = Files.readString(resource.getFile().toPath());
        } else if (keyPath.startsWith("file:")) {
            pemContent = Files.readString(Path.of(URI.create(keyPath)));
        } else {
            pemContent = Files.readString(Path.of(keyPath));
        }

        // Remove PEM headers and whitespace
        String privateKeyPEM = pemContent
                .replaceAll("\\r?\\n", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "");

        // Decode base64
        byte[] keyBytes = Base64.getDecoder().decode(privateKeyPEM);

        // Create private key spec
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");

        return kf.generatePrivate(spec);
    }
}