package com.hrpd.codereview.service;

import com.hrpd.codereview.model.DiffHunk;
import com.hrpd.codereview.model.ReviewResult;
import com.hrpd.codereview.reviewer.Reviewer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Parallelization workflow using virtual threads.
 */
@Slf4j
@RequiredArgsConstructor
public class ParallelWorkflowServiceImpl implements ParallelWorkflowService {

    private final List<Reviewer> reviewers;
    private final AggregatorService aggregator;
    private final ExecutorService pool;

    @Override
    public ReviewResult run(List<DiffHunk> hunks, boolean parallel) {
        long startTime = System.currentTimeMillis();
        log.info("🚀 Starting code review workflow - {} hunks, parallel execution: {}", hunks.size(), parallel);
        
        if (!parallel) {
            log.info("📋 Executing SEQUENTIAL workflow with {} reviewers", reviewers.size());
            long sequentialStart = System.currentTimeMillis();
            
            var parts = reviewers.stream()
                    .map(r -> {
                        log.debug("🔄 Starting {} review (sequential)", r.type());
                        long reviewerStart = System.currentTimeMillis();
                        var result = r.review(hunks);
                        long reviewerDuration = System.currentTimeMillis() - reviewerStart;
                        log.info("✅ {} review completed in {}ms - {} findings", 
                                r.type(), reviewerDuration, result.findings().size());
                        return result;
                    })
                    .toList();
            
            long sequentialDuration = System.currentTimeMillis() - sequentialStart;
            log.info("📊 Sequential execution completed in {}ms", sequentialDuration);
            
            var finalResult = aggregator.merge(parts);
            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("🎯 Total sequential workflow completed in {}ms", totalDuration);
            return finalResult;
        }

        log.info("⚡ Executing PARALLEL workflow with {} reviewers using virtual threads", reviewers.size());
        long parallelStart = System.currentTimeMillis();
        
        var futures = reviewers.stream()
                .map(r -> CompletableFuture.supplyAsync(() -> {
                    log.debug("🔄 Starting {} review (parallel)", r.type());
                    long reviewerStart = System.currentTimeMillis();
                    var result = r.review(hunks);
                    long reviewerDuration = System.currentTimeMillis() - reviewerStart;
                    log.info("✅ {} review completed in {}ms - {} findings", 
                            r.type(), reviewerDuration, result.findings().size());
                    return result;
                }, pool))
                .toList();

        log.info("⏳ Waiting for all {} parallel reviewers to complete...", futures.size());
        var parts = futures.stream()
                .map(f -> f.handle((reviewResult, ex) -> {
                    if (ex != null) {
                        log.error("❌ Reviewer failed with exception", ex);
                        return ReviewResult.empty();
                    }
                    return reviewResult;
                }))
                .map(CompletableFuture::join)
                .toList();

        long parallelDuration = System.currentTimeMillis() - parallelStart;
        log.info("📊 Parallel execution completed in {}ms", parallelDuration);
        
        var finalResult = aggregator.merge(parts);
        long totalDuration = System.currentTimeMillis() - startTime;
        log.info("🎯 Total parallel workflow completed in {}ms", totalDuration);
        return finalResult;
    }
}
