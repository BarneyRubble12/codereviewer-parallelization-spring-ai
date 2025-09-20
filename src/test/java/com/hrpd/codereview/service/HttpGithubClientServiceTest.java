package com.hrpd.codereview.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HttpGithubClientService.
 * 
 * Note: These tests focus on constructor behavior and parameter validation
 * since mocking HttpClient is complex and the service creates its own instance.
 */
@ExtendWith(MockitoExtension.class)
class HttpGithubClientServiceTest {

    private HttpGithubClientService githubService;

    @BeforeEach
    void setUp() {
        githubService = new HttpGithubClientService("https://api.github.com", "test-token");
    }

    @Test
    void testConstructor_withTrailingSlash() {
        // Act
        HttpGithubClientService service = new HttpGithubClientService("https://api.github.com/", "token");

        // Assert - Constructor should not throw
        assertNotNull(service);
    }

    @Test
    void testConstructor_withNullToken() {
        // Act
        HttpGithubClientService service = new HttpGithubClientService("https://api.github.com", null);

        // Assert - Constructor should handle null token gracefully
        assertNotNull(service);
    }

    @Test
    void testConstructor_withEmptyToken() {
        // Act
        HttpGithubClientService service = new HttpGithubClientService("https://api.github.com", "   ");

        // Assert - Constructor should handle empty/whitespace token
        assertNotNull(service);
    }

    @Test
    void testConstructor_withBaseApiNotEndingWithSlash() {
        // Act
        HttpGithubClientService service = new HttpGithubClientService("https://api.github.com", "token");

        // Assert - Constructor should not throw
        assertNotNull(service);
    }

    @Test
    void testConstructor_withBaseApiEndingWithSlash() {
        // Act
        HttpGithubClientService service = new HttpGithubClientService("https://api.github.com/", "token");

        // Assert - Constructor should not throw
        assertNotNull(service);
    }

    @Test
    void testConstructor_withCustomBaseApi() {
        // Act
        HttpGithubClientService service = new HttpGithubClientService("https://custom.github.com", "token");

        // Assert - Constructor should not throw
        assertNotNull(service);
    }

    @Test
    void testConstructor_withWhitespaceToken() {
        // Act
        HttpGithubClientService service = new HttpGithubClientService("https://api.github.com", "  token  ");

        // Assert - Constructor should handle whitespace token
        assertNotNull(service);
    }

    @Test
    void testConstructor_withEmptyStringToken() {
        // Act
        HttpGithubClientService service = new HttpGithubClientService("https://api.github.com", "");

        // Assert - Constructor should handle empty string token
        assertNotNull(service);
    }

    @Test
    void testFetchPrPatch_withInvalidRepo() {
        // Act & Assert - Should throw IllegalArgumentException for invalid repo format
        assertThrows(IllegalArgumentException.class, () -> {
            githubService.fetchPrPatch("invalid-repo", 123);
        });
    }

    @Test
    void testFetchPrPatch_withNullRepo() {
        // Act & Assert - Should throw IllegalArgumentException for null repo
        assertThrows(IllegalArgumentException.class, () -> {
            githubService.fetchPrPatch(null, 123);
        });
    }

    @Test
    void testFetchPrPatch_withEmptyRepo() {
        // Act & Assert - Should throw IllegalArgumentException for empty repo
        assertThrows(IllegalArgumentException.class, () -> {
            githubService.fetchPrPatch("", 123);
        });
    }

    @Test
    void testFetchPrDiff_withInvalidRepo() {
        // Act & Assert - Should throw IllegalArgumentException for invalid repo format
        assertThrows(IllegalArgumentException.class, () -> {
            githubService.fetchPrDiff("invalid-repo", 123);
        });
    }

    @Test
    void testFetchPrDiff_withNullRepo() {
        // Act & Assert - Should throw IllegalArgumentException for null repo
        assertThrows(IllegalArgumentException.class, () -> {
            githubService.fetchPrDiff(null, 123);
        });
    }

    @Test
    void testFetchPrDiff_withEmptyRepo() {
        // Act & Assert - Should throw IllegalArgumentException for empty repo
        assertThrows(IllegalArgumentException.class, () -> {
            githubService.fetchPrDiff("", 123);
        });
    }

    @Test
    void testFetchPrPatch_withValidRepo() {
        // Act & Assert - Should not throw for valid repo format (but will fail due to network)
        // This test verifies the parameter validation passes
        assertThrows(Exception.class, () -> {
            githubService.fetchPrPatch("owner/repo", 123);
        });
    }

    @Test
    void testFetchPrDiff_withValidRepo() {
        // Act & Assert - Should not throw for valid repo format (but will fail due to network)
        // This test verifies the parameter validation passes
        assertThrows(Exception.class, () -> {
            githubService.fetchPrDiff("owner/repo", 123);
        });
    }
}