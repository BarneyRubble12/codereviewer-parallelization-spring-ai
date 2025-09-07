package com.hrpd.codereview.controller;

import com.hrpd.codereview.model.ReviewResult;
import com.hrpd.codereview.model.request.ReviewDiffRequest;
import com.hrpd.codereview.model.request.ReviewPRRequest;
import com.hrpd.codereview.service.DiffService;
import com.hrpd.codereview.service.GithubClientService;
import com.hrpd.codereview.service.ParallelWorkflowService;
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
@RestController
@RequestMapping("/review")
public class ReviewController {

    private final DiffService diffService;
    private final GithubClientService githubClient;
    private final ParallelWorkflowService workflow;

    public ReviewController(
            DiffService diffService,
            GithubClientService githubClient,
            ParallelWorkflowService workflow) {
        this.diffService = diffService;
        this.githubClient = githubClient;
        this.workflow = workflow;
    }

    /**
     * Accepts a unified diff, runs reviewers, returns a merged result.
     */
    @PostMapping("/diff")
    public ReviewResult fromDiff(@RequestBody ReviewDiffRequest req) {
        var hunks = diffService.parseUnifiedPatch(req.patch());
        return workflow.run(hunks, req.parallel());
    }

    /**
     * Fetches a PR patch via GitHub REST, then reviews it.
     */
    @PostMapping("/pr")
    public ReviewResult fromPr(@RequestBody ReviewPRRequest req) throws IOException, InterruptedException {
        var patch = githubClient.fetchPrPatch(req.repo(), req.prNumber());
        var hunks = diffService.parseUnifiedPatch(patch);
        return workflow.run(hunks, req.parallel());
    }
}
