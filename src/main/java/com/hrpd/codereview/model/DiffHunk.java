package com.hrpd.codereview.model;

/**
 * Minimal representation of a unified diff hunk.
 */
public record DiffHunk(String filePath, int start, int end, String patch) {}

