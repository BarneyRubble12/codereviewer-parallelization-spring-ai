package com.hrpd.codereview.service;

import com.hrpd.codereview.model.ReviewResult;

import java.util.List;

/**
 * Merges reviewer outputs, dedupes, summarizes.
 */
public interface AggregatorService {
    ReviewResult merge(List<ReviewResult> parts);
}
