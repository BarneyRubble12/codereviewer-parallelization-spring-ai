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
        log.info("📚 Retrieved {} characters of security standards", grounding.length());
        log.info("📚 SECURITY STANDARDS: {}", grounding);

        for (int i = 0; i < hunks.size(); i++) {
            var h = hunks.get(i);
            log.debug("🔍 Analyzing security hunk {}/{}: {}", i + 1, hunks.size(), h.filePath());
            String prompt = """
            You are a security expert reviewing Java code. Look for security vulnerabilities.
            
            IMPORTANT: Return ONLY valid JSON. Do not include any explanatory text before or after the JSON.
            
            Analyze this code diff and return EXACTLY this JSON format:
            
            {"findings":[
               {"title":"Issue Title","rationale":"Why this is a problem","suggestion":"How to fix it",
                "severity":"HIGH","filePath":"","lineStart":1,"lineEnd":1}
             ],
             "summary":"Brief summary"}
            
            Look specifically for:
            1. Hardcoded API keys, passwords, or secrets
            2. SQL injection vulnerabilities 
            3. Logging sensitive information
            4. Missing authentication
            
            IMPORTANT: 
            - If you find security issues, return them in the findings array
            - If no issues, return empty findings array
            - Return ONLY the JSON object, no other text
            
            Code to analyze:
            ```diff
            %s
            ```
            """.formatted(h.patch());

            log.debug("🤖 Calling AI model for security analysis...");
            String json = chat.prompt().user(prompt).call().content();
            log.info("🔍 RAW AI RESPONSE: {}", json);
            var hunkFindings = JsonUtils.parseFindings(json, ReviewerType.SECURITY, h.filePath());
            log.info("🔍 PARSED FINDINGS: {}", hunkFindings.size());
            findings.addAll(hunkFindings);
            log.debug("✅ Security analysis complete for hunk {}/{}: {} findings", 
                    i + 1, hunks.size(), hunkFindings.size());
        }
        log.info("🔒 SECURITY review complete: {} total findings", findings.size());
        return new ReviewResult(findings, "Security review (grounded) complete");
    }
}
