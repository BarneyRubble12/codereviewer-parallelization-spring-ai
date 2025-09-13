package com.hrpd.codereview.controller;

import com.hrpd.codereview.model.ReviewResult;
import com.hrpd.codereview.model.request.ReviewDiffRequest;
import com.hrpd.codereview.model.request.ReviewPRRequest;
import com.hrpd.codereview.service.DiffService;
import com.hrpd.codereview.service.GithubClientService;
import com.hrpd.codereview.service.ParallelWorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * REST endpoints to run a review over:
 *  - a raw unified diff (POST /review/diff)
 *  - a GitHub pull request (POST /review/pr)
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/review")
public class ReviewController {

    private final DiffService diffService;
    private final GithubClientService githubClient;
    private final ParallelWorkflowService workflow;

    /**
     * Accepts a unified diff, runs reviewers, returns a merged result.
     */
    @PostMapping("/diff")
    public ReviewResult fromDiff(@RequestBody ReviewDiffRequest req) {
        long startTime = System.currentTimeMillis();
        log.info("🎯 ===== NEW DIFF REVIEW REQUEST =====");
        log.info("📋 Request details: parallel={}, patch size={} characters", 
                req.parallel(), req.patch().length());
        
        var hunks = diffService.parseUnifiedPatch(req.patch());
        var result = workflow.run(hunks, req.parallel());
        
        long totalDuration = System.currentTimeMillis() - startTime;
        log.info("🏁 DIFF REVIEW COMPLETE: {} findings in {}ms", result.findings().size(), totalDuration);
        log.info("📊 Summary: {}", result.summary());
        log.info("🎯 ===== END DIFF REVIEW REQUEST =====");
        
        return result;
    }

    /**
     * Fetches a PR patch via GitHub REST, then reviews it.
     */
    @PostMapping("/pr")
    public ReviewResult fromPr(@RequestBody ReviewPRRequest req) throws IOException, InterruptedException {
        long startTime = System.currentTimeMillis();
        log.info("🎯 ===== NEW PR REVIEW REQUEST =====");
        log.info("📋 Request details: repo={}, pr={}, parallel={}", 
                req.repo(), req.prNumber(), req.parallel());
        
        var patch = githubClient.fetchPrPatch(req.repo(), req.prNumber());
        var hunks = diffService.parseUnifiedPatch(patch);
        var result = workflow.run(hunks, req.parallel());
        
        long totalDuration = System.currentTimeMillis() - startTime;
        log.info("🏁 PR REVIEW COMPLETE: {} findings in {}ms", result.findings().size(), totalDuration);
        log.info("📊 Summary: {}", result.summary());
        log.info("🎯 ===== END PR REVIEW REQUEST =====");
        
        return result;
    }
}
