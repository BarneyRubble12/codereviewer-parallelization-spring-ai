package com.hrpd.codereview.model.request;

/**
 * Request payload for the /review/diff endpoint.
 * 
 * <p>This request allows clients to submit a raw unified diff patch for
 * code review. The system will parse the diff, extract individual hunks,
 * and run them through the configured AI-powered reviewers.</p>
 * 
 * @param patch the unified diff patch content to be reviewed
 * @param parallel whether to run reviewers in parallel (true) or sequentially (false)
 */
public record ReviewDiffRequest(String patch, boolean parallel) {}
