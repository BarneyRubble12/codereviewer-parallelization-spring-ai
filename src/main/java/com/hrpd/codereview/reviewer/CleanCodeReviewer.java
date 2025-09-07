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

/**
 *  LLM-powered "Clean Code" reviewer.
 */
public class CleanCodeReviewer implements Reviewer {

    private final ChatClient chat;
    private final StandardsRetrieverService retriever;

    public CleanCodeReviewer(ChatClient chat, StandardsRetrieverService retriever) {
        this.chat = chat; this.retriever = retriever;
    }

    @Override
    public ReviewerType type() {
        return ReviewerType.CLEAN_CODE;
    }

    @Override
    public ReviewResult review(List<DiffHunk> hunks) {
        var findings = new ArrayList<Finding>();
        String grounding = retriever.retrieveContext(
                "java clean code; naming; complexity; duplication; comments; exceptions; logging", 6, "general");

        for (var h : hunks) {
            String prompt = """
        You are a senior Java CLEAN CODE reviewer.
        INTERNAL STANDARDS:
        %s

        Return JSON with findings; focus on readability, cohesion, exceptions, logging, duplication.

        ```diff
        %s
        ```
        """.formatted(grounding, h.patch());

            String json = chat.prompt().user(prompt).call().content();
            findings.addAll(JsonUtils.parseFindings(json, ReviewerType.CLEAN_CODE));
        }
        return new ReviewResult(findings, "Clean code review (grounded) complete");
    }
}
