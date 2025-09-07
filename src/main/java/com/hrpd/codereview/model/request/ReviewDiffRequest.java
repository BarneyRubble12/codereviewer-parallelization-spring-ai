package com.hrpd.codereview.model.request;

/**
 * Request for /review/diff.
 */
public record ReviewDiffRequest(String patch, boolean parallel) {}
