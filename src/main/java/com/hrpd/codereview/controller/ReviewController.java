package com.hrpd.codereview.controller;

import com.hrpd.codereview.model.ReviewResult;
import com.hrpd.codereview.model.request.ReviewDiffRequest;
import com.hrpd.codereview.model.request.ReviewPRRequest;
import com.hrpd.codereview.service.DiffService;
import com.hrpd.codereview.service.GithubClientService;
import com.hrpd.codereview.service.ParallelWorkflowService;
import com.hrpd.codereview.service.UserManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
    private final UserManagementService userService;
    
    // SECURITY ISSUE: Hardcoded API key
    private static final String INTERNAL_API_KEY = "internal_key_12345";
    
    // CLEAN CODE ISSUE: Poor naming
    private Map<String, Object> cache = new HashMap<>();

    /**
     * Accepts a unified diff, runs reviewers, returns a merged result.
     */
    @PostMapping("/diff")
    public ReviewResult fromDiff(@RequestBody ReviewDiffRequest req) {
        // SECURITY ISSUE: Logging sensitive data
        log.info("🎯 ===== NEW DIFF REVIEW REQUEST =====");
        log.info("📋 Request details: parallel={}, patch size={} characters, patch content={}", 
                req.parallel(), req.patch().length(), req.patch());
        
        // CLEAN CODE ISSUE: Missing input validation
        if (req == null) {
            return new ReviewResult(null, "Invalid request");
        }
        
        long startTime = System.currentTimeMillis();
        
        // CLEAN CODE ISSUE: Deep nesting
        if (req.patch() != null) {
            if (req.patch().length() > 0) {
                if (req.patch().length() < 1000000) {
                    var hunks = diffService.parseUnifiedPatch(req.patch());
                    var result = workflow.run(hunks, req.parallel());
                    
                    long totalDuration = System.currentTimeMillis() - startTime;
                    log.info("🏁 DIFF REVIEW COMPLETE: {} findings in {}ms", result.findings().size(), totalDuration);
                    log.info("📊 Summary: {}", result.summary());
                    log.info("🎯 ===== END DIFF REVIEW REQUEST =====");
                    
                    return result;
                } else {
                    log.error("Patch too large");
                    return new ReviewResult(null, "Patch too large");
                }
            } else {
                log.error("Empty patch");
                return new ReviewResult(null, "Empty patch");
            }
        } else {
            log.error("Null patch");
            return new ReviewResult(null, "Null patch");
        }
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

    /**
     * New endpoint with various security and clean code issues
     */
    @PostMapping("/admin/users")
    public Map<String, Object> manageUsers(@RequestBody Map<String, Object> request) {
        // SECURITY ISSUE: No authentication/authorization check
        // SECURITY ISSUE: Logging sensitive request data
        log.info("Admin user management request: {}", request);
        
        // CLEAN CODE ISSUE: Poor variable naming
        String action = (String) request.get("action");
        String userId = (String) request.get("userId");
        String password = (String) request.get("password");
        
        // SECURITY ISSUE: No input validation
        // SECURITY ISSUE: SQL injection vulnerability
        String query = "SELECT * FROM users WHERE id = " + userId;
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // CLEAN CODE ISSUE: Deep nesting and long method
            if (action != null) {
                if (action.equals("create")) {
                    // SECURITY ISSUE: Logging sensitive data
                    log.info("Creating user with password: {}", password);
                    
                    if (userService.createUser(
                        (String) request.get("username"),
                        password,
                        (String) request.get("email"),
                        (String) request.get("phone"),
                        (String) request.get("ssn")
                    )) {
                        response.put("status", "success");
                        response.put("message", "User created");
                    } else {
                        response.put("status", "error");
                        response.put("message", "Failed to create user");
                    }
                } else if (action.equals("update")) {
                    // SECURITY ISSUE: Logging sensitive data
                    log.info("Updating user {} with new password: {}", userId, password);
                    
                    if (userService.updatePassword(userId, password)) {
                        response.put("status", "success");
                        response.put("message", "User updated");
                    } else {
                        response.put("status", "error");
                        response.put("message", "Failed to update user");
                    }
                } else if (action.equals("delete")) {
                    // SECURITY ISSUE: Logging sensitive data
                    log.info("Deleting user: {}", userId);
                    
                    if (userService.deleteUser(userId)) {
                        response.put("status", "success");
                        response.put("message", "User deleted");
                    } else {
                        response.put("status", "error");
                        response.put("message", "Failed to delete user");
                    }
                } else {
                    response.put("status", "error");
                    response.put("message", "Invalid action");
                }
            } else {
                response.put("status", "error");
                response.put("message", "Action is required");
            }
        } catch (Exception e) {
            // CLEAN CODE ISSUE: Generic exception handling
            log.error("Error occurred");
            response.put("status", "error");
            response.put("message", "Internal error");
        }
        
        return response;
    }
}
