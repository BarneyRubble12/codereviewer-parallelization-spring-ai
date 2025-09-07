package com.hrpd.codereview.model;

import java.util.List;

/**
 * Combined output from one reviewer or from the final aggregator.
 */
public record ReviewResult(List<Finding> findings, String summary) {
    public static ReviewResult empty() { return new ReviewResult(List.of(), ""); }
}

