package com.hrpd.codereview.reviewer;

import com.hrpd.codereview.model.DiffHunk;
import com.hrpd.codereview.model.Finding;
import com.hrpd.codereview.model.ReviewResult;
import com.hrpd.codereview.model.ReviewerType;
import com.hrpd.codereview.service.StandardsRetrieverService;
import com.hrpd.codereview.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.List;

/** LLM-powered "Performance" reviewer. */
@Slf4j
@RequiredArgsConstructor
public class PerformanceReviewer implements Reviewer {

    private final ChatClient chat;
    private final StandardsRetrieverService retriever;

    @Override public ReviewerType type() {
        return ReviewerType.PERFORMANCE;
    }

    @Override
    public ReviewResult review(List<DiffHunk> hunks) {
        log.info("⚡ Starting PERFORMANCE review for {} hunks", hunks.size());
        var findings = new ArrayList<Finding>();
        
        log.debug("🔍 Retrieving performance standards context...");
        String grounding = retriever.retrieveContext(
                "java performance; allocations; GC pressure; streams; SQL N+1; caching; pagination", 6, "performance");
        log.debug("📚 Retrieved {} characters of performance standards", grounding.length());

        for (int i = 0; i < hunks.size(); i++) {
            var h = hunks.get(i);
            log.debug("🔍 Analyzing performance hunk {}/{}: {}", i + 1, hunks.size(), h.filePath());
            String prompt = """
        You are a senior Java PERFORMANCE reviewer. Look for performance issues.
        
        Analyze this code diff and return EXACTLY this JSON format:
        
        {"findings":[
           {"title":"Issue Title","rationale":"Why this is a problem","suggestion":"How to fix it",
            "severity":"HIGH","filePath":"","lineStart":1,"lineEnd":1}
         ],
         "summary":"Brief summary"}
        
        Look specifically for:
        1. Memory leaks and inefficient allocations
        2. N+1 database queries
        3. Missing connection pooling
        4. Inefficient loops or algorithms
        5. Large object creation in hot paths
        6. Missing caching opportunities
        
        IMPORTANT: If you find performance issues, return them in the findings array. If no issues, return empty findings array.
        
        Code to analyze:
        ```diff
        %s
        ```
        """.formatted(h.patch());

            log.debug("🤖 Calling AI model for performance analysis...");
            String json = chat.prompt().user(prompt).call().content();
            var hunkFindings = JsonUtils.parseFindings(json, ReviewerType.PERFORMANCE);
            findings.addAll(hunkFindings);
            log.debug("✅ Performance analysis complete for hunk {}/{}: {} findings", 
                    i + 1, hunks.size(), hunkFindings.size());
        }
        log.info("⚡ PERFORMANCE review complete: {} total findings", findings.size());
        return new ReviewResult(findings, "Performance review (grounded) complete");
    }
}
