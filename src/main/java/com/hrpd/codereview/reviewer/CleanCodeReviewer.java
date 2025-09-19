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

            log.debug("🤖 Calling AI model for clean code analysis...");
            String json = chat.prompt().user(prompt).call().content();
            var hunkFindings = JsonUtils.parseFindings(json, ReviewerType.CLEAN_CODE, h.filePath());
            findings.addAll(hunkFindings);
            log.debug("✅ Clean code analysis complete for hunk {}/{}: {} findings", 
                    i + 1, hunks.size(), hunkFindings.size());
        }
        log.info("🧹 CLEAN CODE review complete: {} total findings", findings.size());
        return new ReviewResult(findings, "Clean code review (grounded) complete");
    }
}
