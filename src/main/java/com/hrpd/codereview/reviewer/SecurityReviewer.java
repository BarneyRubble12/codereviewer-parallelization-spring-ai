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
 * LLM-powered "Security" reviewer with grounded standards.
 */
public class SecurityReviewer implements Reviewer {

    private final ChatClient chat;
    private final StandardsRetrieverService retriever;

    public SecurityReviewer(ChatClient chat, StandardsRetrieverService retriever) {
        this.chat = chat; this.retriever = retriever;
    }

    @Override public ReviewerType type() { return ReviewerType.SECURITY; }

    @Override
    public ReviewResult review(List<DiffHunk> hunks) {
        var findings = new ArrayList<Finding>();
        String grounding = retriever.retrieveContext(
                "java security review; injection; SSRF; XXE; secrets; crypto; authz; PII logging", 6, "security");

        for (var h : hunks) {
            String prompt = """
            You are a senior Java SECURITY reviewer.
            Use INTERNAL STANDARDS (below) as authoritative guidance.
    
            INTERNAL STANDARDS:
            %s
    
            Analyze the unified diff hunk and return JSON:
            {"findings":[
               {"title":"","rationale":"","suggestion":"",
                "severity":"BLOCKER|HIGH|MEDIUM|LOW|INFO",
                "filePath":"","lineStart":0,"lineEnd":0}
             ],
             "summary":""}
    
            - Cite relevant internal standards in rationale when applicable.
            - If no issues, return an empty findings array.
    
            Diff hunk:
            ```diff
            %s
            ```
            """.formatted(grounding, h.patch());

            String json = chat.prompt().user(prompt).call().content();
            findings.addAll(JsonUtils.parseFindings(json, ReviewerType.SECURITY));
        }
        return new ReviewResult(findings, "Security review (grounded) complete");
    }
}
