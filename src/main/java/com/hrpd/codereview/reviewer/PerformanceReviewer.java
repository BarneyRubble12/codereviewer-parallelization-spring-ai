package com.hrpd.codereview.reviewer;

import com.hrpd.codereview.model.DiffHunk;
import com.hrpd.codereview.model.Finding;
import com.hrpd.codereview.model.ReviewResult;
import com.hrpd.codereview.model.ReviewerType;
import com.hrpd.codereview.service.StandardsRetrieverService;
import com.hrpd.codereview.utils.JsonUtils;
import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.List;

/** LLM-powered "Performance" reviewer. */
public class PerformanceReviewer implements Reviewer {

    private final ChatClient chat;
    private final StandardsRetrieverService retriever;

    public PerformanceReviewer(ChatClient chat, StandardsRetrieverService retriever) {
        this.chat = chat; this.retriever = retriever;
    }

    @Override public ReviewerType type() {
        return ReviewerType.PERFORMANCE;
    }

    @Override
    public ReviewResult review(List<DiffHunk> hunks) {
        var findings = new ArrayList<Finding>();
        String grounding = retriever.retrieveContext(
                "java performance; allocations; GC pressure; streams; SQL N+1; caching; pagination", 6, "performance");

        for (var h : hunks) {
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

            String json = chat.prompt().user(prompt).call().content();
            findings.addAll(JsonUtils.parseFindings(json, ReviewerType.PERFORMANCE));
        }
        return new ReviewResult(findings, "Performance review (grounded) complete");
    }
}
