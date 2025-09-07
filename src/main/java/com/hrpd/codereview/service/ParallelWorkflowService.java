package com.hrpd.codereview.service;

import com.hrpd.codereview.model.DiffHunk;
import com.hrpd.codereview.model.ReviewResult;

import java.util.List;

/**
 * Fan-out to reviewers (optionally parallel), fan-in via aggregator.
 */
public interface ParallelWorkflowService {
    ReviewResult run(List<DiffHunk> hunks, boolean parallel);
}
