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

/**
 *  LLM-powered "Clean Code" reviewer.
 */
@Slf4j
@RequiredArgsConstructor
public class CleanCodeReviewer implements Reviewer {

    private final ChatClient chat;
    private final StandardsRetrieverService retriever;

    @Override
    public ReviewerType type() {
        return ReviewerType.CLEAN_CODE;
    }

    @Override
    public ReviewResult review(List<DiffHunk> hunks) {
        log.info("🧹 Starting CLEAN CODE review for {} hunks", hunks.size());
        var findings = new ArrayList<Finding>();
        
        log.debug("🔍 Retrieving clean code standards context...");
        String grounding = retriever.retrieveContext(
                "java clean code; naming; complexity; duplication; comments; exceptions; logging", 6, "general");
        log.debug("📚 Retrieved {} characters of clean code standards", grounding.length());

        for (int i = 0; i < hunks.size(); i++) {
            var h = hunks.get(i);
            log.debug("🔍 Analyzing clean code hunk {}/{}: {}", i + 1, hunks.size(), h.filePath());
            String prompt = """
        You are a senior Java CLEAN CODE reviewer.
        INTERNAL STANDARDS:
        %s

        Return JSON with findings; focus on readability, cohesion, exceptions, logging, duplication.

        ```diff
        %s
        ```
        """.formatted(grounding, h.patch());

            log.debug("🤖 Calling AI model for clean code analysis...");
            String json = chat.prompt().user(prompt).call().content();
            var hunkFindings = JsonUtils.parseFindings(json, ReviewerType.CLEAN_CODE);
            findings.addAll(hunkFindings);
            log.debug("✅ Clean code analysis complete for hunk {}/{}: {} findings", 
                    i + 1, hunks.size(), hunkFindings.size());
        }
        log.info("🧹 CLEAN CODE review complete: {} total findings", findings.size());
        return new ReviewResult(findings, "Clean code review (grounded) complete");
    }
}
