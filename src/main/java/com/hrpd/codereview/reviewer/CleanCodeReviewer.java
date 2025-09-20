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
 * AI-powered reviewer specialized in identifying clean code violations and code quality issues.
 * 
 * <p>This reviewer focuses on code maintainability, readability, and adherence to clean code
 * principles. It analyzes code for common quality issues such as poor naming conventions,
 * excessive complexity, code duplication, and maintainability concerns.</p>
 * 
 * <p>The reviewer uses a combination of AI analysis and internal coding standards to provide
 * comprehensive feedback on code quality. It retrieves relevant standards from the knowledge
 * base to ground its analysis in organizational best practices.</p>
 * 
 * <p>Key areas of focus include:
 * <ul>
 *   <li>Variable and method naming conventions</li>
 *   <li>Code complexity and nesting depth</li>
 *   <li>Method length and single responsibility</li>
 *   <li>Code duplication and DRY principles</li>
 *   <li>Exception handling patterns</li>
 *   <li>Input validation and defensive programming</li>
 *   <li>Dead code and unused methods</li>
 * </ul></p>
 * 
 * @see Reviewer
 * @see StandardsRetrieverService
 */
@Slf4j
@RequiredArgsConstructor
public class CleanCodeReviewer implements Reviewer {

    /**
     * Chat client for communicating with the AI model.
     */
    private final ChatClient chat;
    
    /**
     * Service for retrieving relevant coding standards from the knowledge base.
     */
    private final StandardsRetrieverService retriever;

    /**
     * Returns the type of this reviewer.
     * 
     * @return CLEAN_CODE reviewer type
     */
    @Override
    public ReviewerType type() {
        return ReviewerType.CLEAN_CODE;
    }

    /**
     * Performs a comprehensive clean code review on the provided diff hunks.
     * 
     * <p>This method analyzes each diff hunk for code quality issues using AI-powered
     * analysis combined with organizational coding standards. The review process includes:
     * <ol>
     *   <li>Retrieving relevant clean code standards from the knowledge base</li>
     *   <li>Analyzing each diff hunk individually</li>
     *   <li>Generating specific findings with rationale and suggestions</li>
     *   <li>Aggregating all findings into a comprehensive result</li>
     * </ol></p>
     * 
     * @param hunks the list of diff hunks to review
     * @return a ReviewResult containing all findings and a summary
     */
    @Override
    public ReviewResult review(List<DiffHunk> hunks) {
        log.info("🧹 Starting CLEAN CODE review for {} hunks", hunks.size());
        var findings = new ArrayList<Finding>();
        
        // Retrieve relevant clean code standards to ground the AI analysis
        log.debug("🔍 Retrieving clean code standards context...");
        String grounding = retriever.retrieveContext(
                "java clean code; naming; complexity; duplication; comments; exceptions; logging", 6, "general");
        log.debug("📚 Retrieved {} characters of clean code standards", grounding.length());

        // Analyze each diff hunk individually for clean code issues
        for (int i = 0; i < hunks.size(); i++) {
            var h = hunks.get(i);
            log.debug("🔍 Analyzing clean code hunk {}/{}: {}", i + 1, hunks.size(), h.filePath());
            
            // Construct a detailed prompt that includes standards and specific clean code criteria
            String prompt = """
        You are a senior Java CLEAN CODE reviewer. Your job is to identify code quality issues and violations.
        INTERNAL STANDARDS:
        %s

        CRITICAL: Look for these specific clean code issues:
        - Poor variable naming (single letters like x, y, z or abbreviations like cnt, lst, str)
        - Deep nesting (more than 3 levels of if/for/while statements)
        - Long methods (more than 20 lines)
        - Code duplication (repeated logic patterns)
        - Generic exception handling (catch(Exception e))
        - Missing input validation (null checks, parameter validation)
        - Unused methods or dead code
        - Complex conditional statements that should be extracted

        IMPORTANT: Return ONLY valid JSON. Do not include any explanatory text before or after the JSON.

        Return JSON with findings:
        {"findings":[
           {"title":"","rationale":"","suggestion":"",
            "severity":"BLOCKER|HIGH|MEDIUM|LOW|INFO",
            "filePath":"","lineStart":0,"lineEnd":0}
         ],
         "summary":""}

        - Be thorough and identify ALL code quality issues
        - Cite relevant internal standards in rationale when applicable
        - Use HIGH severity for major code quality violations
        - If no issues found, return empty findings array
        - Return ONLY the JSON object, no other text

        ```diff
        %s
        ```
        """.formatted(grounding, h.patch());

            // Call the AI model to analyze the code for clean code issues
            log.debug("🤖 Calling AI model for clean code analysis...");
            String json = chat.prompt().user(prompt).call().content();
            
            // Parse the AI response and extract findings
            var hunkFindings = JsonUtils.parseFindings(json, ReviewerType.CLEAN_CODE, h.filePath());
            findings.addAll(hunkFindings);
            log.debug("✅ Clean code analysis complete for hunk {}/{}: {} findings", 
                    i + 1, hunks.size(), hunkFindings.size());
        }
        // Return the aggregated results from all hunks
        log.info("🧹 CLEAN CODE review complete: {} total findings", findings.size());
        return new ReviewResult(findings, "Clean code review (grounded) complete");
    }
}
