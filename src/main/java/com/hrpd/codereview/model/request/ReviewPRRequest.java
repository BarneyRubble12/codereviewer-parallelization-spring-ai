package com.hrpd.codereview.model.request;

/**
 * Request payload for the /review/pr endpoint.
 * 
 * <p>This request allows clients to review a GitHub pull request by specifying
 * the repository and pull request number. The system will fetch the PR patch
 * from GitHub, parse it, and run it through the configured AI-powered reviewers.</p>
 * 
 * @param repo the GitHub repository in format "owner/repo" (e.g., "spring-projects/spring-boot")
 * @param prNumber the pull request number to review
 * @param parallel whether to run reviewers in parallel (true) or sequentially (false)
 */
public record ReviewPRRequest(String repo, int prNumber, boolean parallel) {}

