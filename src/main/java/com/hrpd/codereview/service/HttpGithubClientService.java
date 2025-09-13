package com.hrpd.codereview.service;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * GitHub REST client using Java 21 HttpClient.
 */
@Slf4j
public class HttpGithubClientService implements GithubClientService {

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final String baseApi;
    private final String token;

    public HttpGithubClientService(String baseApi, String token) {
        this.baseApi = baseApi.endsWith("/") ? baseApi.substring(0, baseApi.length() - 1) : baseApi;
        this.token = token == null ? "" : token.trim();
    }

    @Override
    public String fetchPrPatch(String repo, int prNumber) throws IOException, InterruptedException {
        log.info("🔗 Fetching PR patch from GitHub: {}/{}", repo, prNumber);
        return fetchWithAccept(repo, prNumber, "application/vnd.github.v3.patch");
    }

    @Override
    public String fetchPrDiff(String repo, int prNumber) throws IOException, InterruptedException {
        log.info("🔗 Fetching PR diff from GitHub: {}/{}", repo, prNumber);
        return fetchWithAccept(repo, prNumber, "application/vnd.github.v3.diff");
    }

    private String fetchWithAccept(String repo, int prNumber, String accept) throws IOException, InterruptedException {
        if (repo == null || !repo.contains("/")) {
            throw new IllegalArgumentException("repo must be 'owner/name', got: " + repo);
        }
        URI uri = URI.create(baseApi + "/repos/" + repo + "/pulls/" + prNumber);
        log.debug("🌐 Making GitHub API request to: {}", uri);

        HttpRequest.Builder req = HttpRequest.newBuilder(uri)
                .GET()
                .header("Accept", accept)
                .header("User-Agent", "pr-code-reviewer");

        if (!token.isBlank()) {
            req.header("Authorization", "token " + token);
            log.debug("🔐 Using authentication token");
        } else {
            log.debug("🔓 No authentication token provided");
        }

        long startTime = System.currentTimeMillis();
        HttpResponse<String> resp = http.send(req.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        long duration = System.currentTimeMillis() - startTime;

        log.info("📡 GitHub API response: {} in {}ms, body size: {} characters", 
                resp.statusCode(), duration, resp.body().length());

        if (resp.statusCode() == 200) {
            log.info("✅ Successfully fetched PR data from GitHub");
            return resp.body();
        }
        
        // Handle redirects
        if (resp.statusCode() == 301 || resp.statusCode() == 302) {
            String location = resp.headers().firstValue("Location").orElse(null);
            log.warn("🔄 GitHub API redirect {} to: {}", resp.statusCode(), location);
            throw new IOException("GitHub API redirect " + resp.statusCode() + " to " + location + 
                    ". Please check the repository URL format: " + repo);
        }
        
        log.error("❌ GitHub API error: {} - {}", resp.statusCode(), resp.body());
        throw new IOException("GitHub API " + resp.statusCode() + " for " + uri + "\n" + resp.body());
    }
}
