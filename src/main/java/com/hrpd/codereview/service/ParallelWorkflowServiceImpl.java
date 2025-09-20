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
 * Implementation of the parallel workflow service that orchestrates code reviews using virtual threads.
 * 
 * <p>This service manages the execution of multiple AI-powered reviewers either sequentially
 * or in parallel, depending on the configuration. When running in parallel mode, it uses
 * virtual threads to achieve high concurrency with minimal resource overhead.</p>
 * 
 * <p>The workflow process includes:
 * <ol>
 *   <li>Receiving a list of diff hunks to review</li>
 *   <li>Executing all configured reviewers (security, performance, clean code)</li>
 *   <li>Collecting results from all reviewers</li>
 *   <li>Aggregating and deduplicating findings</li>
 *   <li>Returning a comprehensive review result</li>
 * </ol></p>
 * 
 * <p>Performance benefits of parallel execution include:
 * <ul>
 *   <li>Reduced total review time through concurrent AI model calls</li>
 *   <li>Better resource utilization using virtual threads</li>
 *   <li>Improved throughput for large code reviews</li>
 * </ul></p>
 * 
 * @see ParallelWorkflowService
 * @see Reviewer
 * @see AggregatorService
 */
@Slf4j
@RequiredArgsConstructor
public class ParallelWorkflowServiceImpl implements ParallelWorkflowService {

    /**
     * List of all configured reviewers to execute during the review process.
     */
    private final List<Reviewer> reviewers;
    
    /**
     * Service responsible for aggregating and deduplicating findings from multiple reviewers.
     */
    private final AggregatorService aggregator;
    
    /**
     * Executor service for managing virtual threads in parallel execution mode.
     */
    private final ExecutorService executorService;

    /**
     * Executes the code review workflow with the specified execution mode.
     * 
     * <p>This method orchestrates the complete review process by:
     * <ol>
     *   <li>Executing all configured reviewers either sequentially or in parallel</li>
     *   <li>Collecting results from all reviewers</li>
     *   <li>Aggregating findings and removing duplicates</li>
     *   <li>Generating a comprehensive summary</li>
     * </ol></p>
     * 
     * <p>When parallel execution is enabled, all reviewers run concurrently using
     * virtual threads, significantly reducing total execution time. Sequential
     * execution runs reviewers one after another, which may be preferred for
     * debugging or when resource constraints require it.</p>
     * 
     * @param hunks the list of diff hunks to review
     * @param parallel true to run reviewers in parallel, false for sequential execution
     * @return a comprehensive ReviewResult containing all findings and summary
     */
    @Override
    public ReviewResult run(List<DiffHunk> hunks, boolean parallel) {
        long startTime = System.currentTimeMillis();
        log.info("🚀 Starting code review workflow - {} hunks, parallel execution: {}", hunks.size(), parallel);
        
        if (!parallel) {
            // Execute reviewers sequentially for debugging or resource-constrained scenarios
            log.info("📋 Executing SEQUENTIAL workflow with {} reviewers", reviewers.size());
            long sequentialStart = System.currentTimeMillis();
            
            // Process each reviewer one after another
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
            
            // Aggregate all reviewer results and return final result
            var finalResult = aggregator.merge(parts);
            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("🎯 Total sequential workflow completed in {}ms", totalDuration);
            return finalResult;
        }

        // Execute reviewers in parallel using virtual threads for maximum performance
        log.info("⚡ Executing PARALLEL workflow with {} reviewers using virtual threads", reviewers.size());
        long parallelStart = System.currentTimeMillis();
        
        // Create CompletableFuture for each reviewer to run concurrently
        var futures = reviewers.stream()
                .map(reviewer -> CompletableFuture.supplyAsync(() -> {
                    log.debug("🔄 Starting {} review (parallel)", reviewer.type());
                    long reviewerStart = System.currentTimeMillis();
                    var result = reviewer.review(hunks);
                    long reviewerDuration = System.currentTimeMillis() - reviewerStart;
                    log.info("✅ {} review completed in {}ms - {} findings", 
                            reviewer.type(), reviewerDuration, result.findings().size());
                    return result;
                }, executorService))
                .toList();

        // Wait for all parallel reviewers to complete and handle any failures gracefully
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
        
        // Aggregate all reviewer results and return final result
        var finalResult = aggregator.merge(parts);
        long totalDuration = System.currentTimeMillis() - startTime;
        log.info("🎯 Total parallel workflow completed in {}ms", totalDuration);
        return finalResult;
    }
}
