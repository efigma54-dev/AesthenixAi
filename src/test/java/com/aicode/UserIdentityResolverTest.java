package com.aicode;

import com.aicode.service.UserIdentityResolver;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for UserIdentityResolver.
 * Verifies the three-tier identity resolution order and IP hashing.
 */
class UserIdentityResolverTest {

    @Test
    void githubLoginTakesPrecedenceOverHeader() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-User-Id", "some-uuid");
        req.setRemoteAddr("192.168.1.1");

        String identity = UserIdentityResolver.resolve(req, "octocat");

        assertThat(identity).isEqualTo("github:octocat");
    }

    @Test
    void headerTakesPrecedenceOverIp() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-User-Id", "test-uuid-123");
        req.setRemoteAddr("10.0.0.1");

        String identity = UserIdentityResolver.resolve(req, null);

        assertThat(identity).isEqualTo("header:test-uuid-123");
    }

    @Test
    void ipFallbackReturnsSha256Hash() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("203.0.113.42");

        String identity = UserIdentityResolver.resolve(req, null);

        assertThat(identity).startsWith("ip:");
        // SHA-256 hex is 64 chars
        assertThat(identity.substring(3)).hasSize(64);
        // Raw IP must never appear in the result
        assertThat(identity).doesNotContain("203.0.113.42");
    }

    @Test
    void blankGithubLoginFallsBackToHeader() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-User-Id", "my-uuid");

        String identity = UserIdentityResolver.resolve(req, "  ");

        assertThat(identity).isEqualTo("header:my-uuid");
    }

    @Test
    void sha256IsConsistentForSameIp() {
        String hash1 = UserIdentityResolver.sha256Hex("192.168.0.1");
        String hash2 = UserIdentityResolver.sha256Hex("192.168.0.1");
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void sha256DiffersForDifferentIps() {
        String hash1 = UserIdentityResolver.sha256Hex("192.168.0.1");
        String hash2 = UserIdentityResolver.sha256Hex("192.168.0.2");
        assertThat(hash1).isNotEqualTo(hash2);
    }
}
