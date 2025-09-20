package com.hrpd.codereview.model;

/**
 * Represents a single hunk (section) from a unified diff format.
 * 
 * <p>A diff hunk contains the changes made to a specific file, including
 * the file path, line number range, and the actual patch content in unified
 * diff format. This is the basic unit of code that gets analyzed by the
 * AI-powered reviewers.</p>
 * 
 * <p>The patch content follows the standard unified diff format with lines
 * prefixed by '+' (additions), '-' (deletions), or ' ' (context lines).</p>
 * 
 * @param filePath the relative path to the file being modified
 * @param start the starting line number in the original file (1-based)
 * @param end the ending line number in the original file (1-based)
 * @param patch the unified diff patch content showing the actual changes
 */
public record DiffHunk(String filePath, int start, int end, String patch) {}

