package com.hrpd.codereview.controller;

import com.hrpd.codereview.model.ReviewResult;
import com.hrpd.codereview.model.request.ReviewDiffRequest;
import com.hrpd.codereview.model.request.ReviewPRRequest;
import com.hrpd.codereview.service.DiffService;
import com.hrpd.codereview.service.GithubClientService;
import com.hrpd.codereview.service.ParallelWorkflowService;
import com.hrpd.codereview.service.StandardsIngestorService;
import com.hrpd.codereview.service.StandardsRetrieverService;
import org.springframework.ai.chat.client.ChatClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * REST controller providing endpoints for AI-powered code review functionality.
 * 
 * <p>This controller exposes HTTP endpoints that allow clients to submit code for review
 * either as raw unified diff patches or by referencing GitHub pull requests. The controller
 * orchestrates the complete review workflow including diff parsing, parallel reviewer
 * execution, and result aggregation.</p>
 * 
 * <p>Available endpoints:
 * <ul>
 *   <li>POST /review/diff - Review a raw unified diff patch</li>
 *   <li>POST /review/pr - Review a GitHub pull request</li>
 *   <li>POST /review/admin/reingest - Admin endpoint to re-ingest standards</li>
 *   <li>POST /review/debug/ai - Debug endpoint for testing AI model</li>
 * </ul></p>
 * 
 * <p>The controller supports both sequential and parallel execution modes, allowing
 * clients to choose between faster parallel processing or more predictable sequential
 * execution for debugging purposes.</p>
 * 
 * @see ParallelWorkflowService
 * @see DiffService
 * @see GithubClientService
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/review")
public class ReviewController {

    /**
     * Service for parsing unified diff patches into reviewable hunks.
     */
    private final DiffService diffService;
    
    /**
     * Service for fetching pull request patches from GitHub.
     */
    private final GithubClientService githubClient;
    
    /**
     * Service for orchestrating the parallel review workflow.
     */
    private final ParallelWorkflowService workflow;
    
    /**
     * Service for ingesting coding standards into the knowledge base.
     */
    private final StandardsIngestorService standardsIngestor;
    
    /**
     * Service for retrieving relevant standards during review.
     */
    private final StandardsRetrieverService standardsRetriever;
    
    /**
     * Chat client for direct AI model communication (used in debug endpoint).
     */
    private final ChatClient chatClient;

    /**
     * Reviews a raw unified diff patch and returns comprehensive findings.
     * 
     * <p>This endpoint accepts a unified diff patch and processes it through the complete
     * review workflow. The patch is parsed into individual hunks, each hunk is analyzed
     * by all configured reviewers (security, performance, clean code), and the results
     * are aggregated and deduplicated.</p>
     * 
     * <p>The execution can be configured to run in parallel (faster) or sequential
     * (more predictable) mode based on the request parameter.</p>
     * 
     * @param req the review request containing the diff patch and execution mode
     * @return a comprehensive ReviewResult with all findings and summary
     */
    @PostMapping("/diff")
    public ReviewResult fromDiff(@RequestBody ReviewDiffRequest req) {
        long startTime = System.currentTimeMillis();
        log.info("🎯 ===== NEW DIFF REVIEW REQUEST =====");
        log.info("📋 Request details: parallel={}, patch size={} characters", 
                req.parallel(), req.patch().length());
        
        // Parse the unified diff into reviewable hunks
        var hunks = diffService.parseUnifiedPatch(req.patch());
        
        // Execute the review workflow with specified execution mode
        var result = workflow.run(hunks, req.parallel());
        
        long totalDuration = System.currentTimeMillis() - startTime;
        log.info("🏁 DIFF REVIEW COMPLETE: {} findings in {}ms", result.findings().size(), totalDuration);
        log.info("📊 Summary: {}", result.summary());
        log.info("🎯 ===== END DIFF REVIEW REQUEST =====");
        
        return result;
    }

    /**
     * Reviews a GitHub pull request by fetching its patch and processing it through the review workflow.
     * 
     * <p>This endpoint accepts a GitHub repository and pull request number, fetches the
     * corresponding patch from GitHub, and processes it through the complete review
     * workflow. This provides a convenient way to review pull requests without
     * manually extracting the diff content.</p>
     * 
     * <p>The process includes:
     * <ol>
     *   <li>Fetching the pull request patch from GitHub API</li>
     *   <li>Parsing the patch into reviewable hunks</li>
     *   <li>Executing the review workflow with all configured reviewers</li>
     *   <li>Returning aggregated and deduplicated results</li>
     * </ol></p>
     * 
     * @param req the PR review request containing repository, PR number, and execution mode
     * @return a comprehensive ReviewResult with all findings and summary
     * @throws IOException if there's an error communicating with GitHub
     * @throws InterruptedException if the request is interrupted
     */
    @PostMapping("/pr")
    public ReviewResult fromPr(@RequestBody ReviewPRRequest req) throws IOException, InterruptedException {
        long startTime = System.currentTimeMillis();
        log.info("🎯 ===== NEW PR REVIEW REQUEST =====");
        log.info("📋 Request details: repo={}, pr={}, parallel={}", 
                req.repo(), req.prNumber(), req.parallel());
        
        // Fetch the pull request patch from GitHub
        var patch = githubClient.fetchPrPatch(req.repo(), req.prNumber());
        
        // Parse the patch into reviewable hunks
        var hunks = diffService.parseUnifiedPatch(patch);
        
        // Execute the review workflow with specified execution mode
        var result = workflow.run(hunks, req.parallel());
        
        long totalDuration = System.currentTimeMillis() - startTime;
        log.info("🏁 PR REVIEW COMPLETE: {} findings in {}ms", result.findings().size(), totalDuration);
        log.info("📊 Summary: {}", result.summary());
        log.info("🎯 ===== END PR REVIEW REQUEST =====");
        
        return result;
    }

    /**
     * Administrative endpoint to force re-ingestion of all coding standards documents.
     * 
     * <p>This endpoint triggers a complete re-ingestion of all standards documents
     * from the file system into the vector database. This is useful during development
     * when standards files have been updated, or for maintenance purposes to ensure
     * the knowledge base is current.</p>
     * 
     * <p>The re-ingestion process includes:
     * <ol>
     *   <li>Scanning the standards directory for markdown files</li>
     *   <li>Processing each file and extracting content</li>
     *   <li>Generating embeddings for semantic search</li>
     *   <li>Storing the processed content in the vector database</li>
     * </ol></p>
     * 
     * @return a ResponseEntity with success or error message
     */
    @PostMapping("/admin/reingest")
    public ResponseEntity<String> reingestStandards() {
        try {
            log.info("🔄 Manual re-ingestion requested via admin endpoint");
            standardsIngestor.reingestAll();
            return ResponseEntity.ok("Standards re-ingestion completed successfully");
        } catch (Exception e) {
            log.error("❌ Failed to re-ingest standards: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Failed to re-ingest standards: " + e.getMessage());
        }
    }

    /**
     * Debug endpoint for testing AI model functionality directly.
     * 
     * <p>This endpoint provides a way to test the AI model directly without going through
     * the full review workflow. It accepts raw code as input and returns the AI model's
     * response, which is useful for debugging AI integration issues or testing model
     * behavior with specific code samples.</p>
     * 
     * <p>The endpoint sends a simple security-focused prompt to the AI model and returns
     * the raw response. This can help identify issues with AI model communication,
     * response parsing, or model behavior.</p>
     * 
     * @param testCode the Java code to test with the AI model
     * @return a ResponseEntity with the AI model's response or error message
     */
    @PostMapping("/debug/ai")
    public ResponseEntity<String> debugAi(@RequestBody String testCode) {
        try {
            log.info("🔍 Testing AI model with code: {}", testCode);
            
            // Test direct AI call with a simple security-focused prompt
            String response = chatClient.prompt()
                .user("Find security issues in this Java code: " + testCode + 
                      " Return JSON: {\"issues\":[\"issue1\",\"issue2\"]}")
                .call()
                .content();
            
            log.info("🤖 Direct AI response: {}", response);
            
            return ResponseEntity.ok("AI test completed: " + response);
        } catch (Exception e) {
            log.error("❌ AI debug test failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("AI test failed: " + e.getMessage());
        }
    }
}
