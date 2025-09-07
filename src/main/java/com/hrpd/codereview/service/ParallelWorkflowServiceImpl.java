package com.hrpd.codereview.service;

import com.hrpd.codereview.model.DiffHunk;
import com.hrpd.codereview.model.ReviewResult;
import com.hrpd.codereview.reviewer.Reviewer;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Parallelization workflow using virtual threads.
 */
public class ParallelWorkflowServiceImpl implements ParallelWorkflowService {

    private final List<Reviewer> reviewers;
    private final AggregatorService aggregator;
    private final ExecutorService pool;

    public ParallelWorkflowServiceImpl(List<Reviewer> reviewers, AggregatorService aggregator, ExecutorService pool) {
        this.reviewers = reviewers;
        this.aggregator = aggregator;
        this.pool = pool;
    }

    @Override
    public ReviewResult run(List<DiffHunk> hunks, boolean parallel) {
        if (!parallel) {
            var parts = reviewers.stream().map(r -> r.review(hunks)).toList();
            return aggregator.merge(parts);
        }

        var futures = reviewers.stream()
                .map(r -> CompletableFuture.supplyAsync(() -> r.review(hunks), pool))
                .toList();

        var parts = futures.stream()
                .map(f -> f.handle((res, ex) -> ex == null ? res : ReviewResult.empty()))
                .map(CompletableFuture::join)
                .toList();

        return aggregator.merge(parts);
    }
}
