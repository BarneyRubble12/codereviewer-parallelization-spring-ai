package com.hrpd.codereview.model.request;

/**
 * Request for /review/pr.
 */
public record ReviewPRRequest(String repo, int prNumber, boolean parallel) {}

