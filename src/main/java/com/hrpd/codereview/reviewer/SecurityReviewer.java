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
 * LLM-powered "Security" reviewer with grounded standards.
 */
@Slf4j
@RequiredArgsConstructor
public class SecurityReviewer implements Reviewer {

    private final ChatClient chat;
    private final StandardsRetrieverService retriever;

    @Override public ReviewerType type() { return ReviewerType.SECURITY; }

    @Override
    public ReviewResult review(List<DiffHunk> hunks) {
        log.info("🔒 Starting SECURITY review for {} hunks", hunks.size());
        var findings = new ArrayList<Finding>();
        
        log.debug("🔍 Retrieving security standards context...");
        String grounding = retriever.retrieveContext(
                "java security review; injection; SSRF; XXE; secrets; crypto; authz; PII logging", 6, "security");
        log.debug("📚 Retrieved {} characters of security standards", grounding.length());

        for (int i = 0; i < hunks.size(); i++) {
            var h = hunks.get(i);
            log.debug("🔍 Analyzing security hunk {}/{}: {}", i + 1, hunks.size(), h.filePath());
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

            log.debug("🤖 Calling AI model for security analysis...");
            String json = chat.prompt().user(prompt).call().content();
            var hunkFindings = JsonUtils.parseFindings(json, ReviewerType.SECURITY);
            findings.addAll(hunkFindings);
            log.debug("✅ Security analysis complete for hunk {}/{}: {} findings", 
                    i + 1, hunks.size(), hunkFindings.size());
        }
        log.info("🔒 SECURITY review complete: {} total findings", findings.size());
        return new ReviewResult(findings, "Security review (grounded) complete");
    }
}
