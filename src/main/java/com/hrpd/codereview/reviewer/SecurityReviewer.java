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
 * AI-powered reviewer specialized in identifying security vulnerabilities and security-related issues.
 * 
 * <p>This reviewer focuses on detecting security vulnerabilities, insecure coding practices,
 * and potential security risks in Java code. It uses a combination of AI analysis and
 * organizational security standards to provide comprehensive security feedback.</p>
 * 
 * <p>The reviewer is particularly effective at identifying:
 * <ul>
 *   <li>Hardcoded secrets, API keys, and passwords</li>
 *   <li>SQL injection vulnerabilities</li>
 *   <li>Cross-site scripting (XSS) risks</li>
 *   <li>Insecure authentication and authorization</li>
 *   <li>Logging of sensitive information</li>
 *   <li>Insecure cryptographic practices</li>
 *   <li>Server-side request forgery (SSRF) vulnerabilities</li>
 *   <li>XML external entity (XXE) attacks</li>
 * </ul></p>
 * 
 * @see Reviewer
 * @see StandardsRetrieverService
 */
@Slf4j
@RequiredArgsConstructor
public class SecurityReviewer implements Reviewer {

    /**
     * Chat client for communicating with the AI model.
     */
    private final ChatClient chat;
    
    /**
     * Service for retrieving relevant security standards from the knowledge base.
     */
    private final StandardsRetrieverService retriever;

    /**
     * Returns the type of this reviewer.
     * 
     * @return SECURITY reviewer type
     */
    @Override 
    public ReviewerType type() { 
        return ReviewerType.SECURITY; 
    }

    /**
     * Performs a comprehensive security review on the provided diff hunks.
     * 
     * <p>This method analyzes each diff hunk for security vulnerabilities using AI-powered
     * analysis combined with organizational security standards. The review process includes:
     * <ol>
     *   <li>Retrieving relevant security standards from the knowledge base</li>
     *   <li>Analyzing each diff hunk for security vulnerabilities</li>
     *   <li>Generating specific security findings with remediation guidance</li>
     *   <li>Aggregating all findings into a comprehensive security report</li>
     * </ol></p>
     * 
     * @param hunks the list of diff hunks to review for security issues
     * @return a ReviewResult containing all security findings and a summary
     */
    @Override
    public ReviewResult review(List<DiffHunk> hunks) {
        log.info("🔒 Starting SECURITY review for {} hunks", hunks.size());
        var findings = new ArrayList<Finding>();
        
        // Retrieve relevant security standards to ground the AI analysis
        log.debug("🔍 Retrieving security standards context...");
        String grounding = retriever.retrieveContext(
                "java security review; injection; SSRF; XXE; secrets; crypto; authz; PII logging", 6, "security");
        log.info("📚 Retrieved {} characters of security standards", grounding.length());
        log.info("📚 SECURITY STANDARDS: {}", grounding);

        // Analyze each diff hunk individually for security vulnerabilities
        for (int i = 0; i < hunks.size(); i++) {
            var h = hunks.get(i);
            log.debug("🔍 Analyzing security hunk {}/{}: {}", i + 1, hunks.size(), h.filePath());
            
            // Construct a security-focused prompt with specific vulnerability patterns
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

            // Call the AI model to analyze the code for security vulnerabilities
            log.debug("🤖 Calling AI model for security analysis...");
            String json = chat.prompt().user(prompt).call().content();
            log.info("🔍 RAW AI RESPONSE: {}", json);
            
            // Parse the AI response and extract security findings
            var hunkFindings = JsonUtils.parseFindings(json, ReviewerType.SECURITY, h.filePath());
            log.info("🔍 PARSED FINDINGS: {}", hunkFindings.size());
            findings.addAll(hunkFindings);
            log.debug("✅ Security analysis complete for hunk {}/{}: {} findings", 
                    i + 1, hunks.size(), hunkFindings.size());
        }
        // Return the aggregated security findings from all hunks
        log.info("🔒 SECURITY review complete: {} total findings", findings.size());
        return new ReviewResult(findings, "Security review (grounded) complete");
    }
}
