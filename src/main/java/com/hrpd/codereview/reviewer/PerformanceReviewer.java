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
        You are a senior Java PERFORMANCE reviewer.
        INTERNAL STANDARDS:
        %s

        Return JSON with findings as specified earlier.
        Focus on hot paths, allocations, I/O, SQL patterns, pagination, caching.

        ```diff
        %s
        ```
        """.formatted(grounding, h.patch());

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
