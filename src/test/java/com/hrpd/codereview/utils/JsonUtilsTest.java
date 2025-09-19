package com.hrpd.codereview.utils;

import com.hrpd.codereview.model.Finding;
import com.hrpd.codereview.model.ReviewerType;
import com.hrpd.codereview.model.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JsonUtils.parseFindings method.
 */
class JsonUtilsTest {

    @Test
    void testParseFindings_withExplanatoryTextBeforeJson() {
        // Test case: AI response with explanatory text before JSON (the original error case)
        String problematicResponse = """
            To provide a thorough review of the Java code snippet you provided, I would need the complete code to analyze. However, based on the snippet and the internal standards you've provided, I can offer a template for how the findings would be structured in JSON format. If you provide the complete code, I can give a more detailed analysis. Here's a template based on the standards:

            ```json
            {
              "findings": [
                {
                  "title": "Unused Import Statements",
                  "rationale": "The code imports `java.io.IOException` which is not used anywhere in the provided snippet. Unused imports can clutter the code and should be removed.",
                  "suggestion": "Remove the unused import statement for `java.io.IOException`.",
                  "severity": "LOW",
                  "filePath": "YourFilePath.java",
                  "lineStart": 3,
                  "lineEnd": 3
                }
              ],
              "summary": "The code snippet contains unused import statements which should be removed to maintain clean code standards."
            }
            ```

            Please provide the complete code for a more comprehensive review, including checks for variable naming, method length, nesting, and other critical issues as per your internal standards.
            """;

        List<Finding> findings = JsonUtils.parseFindings(problematicResponse, ReviewerType.CLEAN_CODE, "TestFile.java");
        
        assertNotNull(findings);
        assertEquals(1, findings.size());
        
        Finding finding = findings.get(0);
        assertEquals("Unused Import Statements", finding.title());
        assertEquals("The code imports `java.io.IOException` which is not used anywhere in the provided snippet. Unused imports can clutter the code and should be removed.", finding.rationale());
        assertEquals("Remove the unused import statement for `java.io.IOException`.", finding.suggestion());
        assertEquals(Severity.LOW, finding.severity());
        assertEquals("YourFilePath.java", finding.filePath());
        assertEquals(3, finding.lineStart());
        assertEquals(3, finding.lineEnd());
        assertEquals(ReviewerType.CLEAN_CODE, finding.reviewer());
    }

    @Test
    void testParseFindings_withCleanJsonResponse() {
        // Test case: Clean JSON response
        String cleanJsonResponse = """
            {
              "findings": [
                {
                  "title": "Test Finding",
                  "rationale": "This is a test",
                  "suggestion": "Fix this",
                  "severity": "HIGH",
                  "filePath": "Test.java",
                  "lineStart": 1,
                  "lineEnd": 1
                }
              ],
              "summary": "Test summary"
            }
            """;

        List<Finding> findings = JsonUtils.parseFindings(cleanJsonResponse, ReviewerType.SECURITY, "TestFile.java");
        
        assertNotNull(findings);
        assertEquals(1, findings.size());
        
        Finding finding = findings.get(0);
        assertEquals("Test Finding", finding.title());
        assertEquals("This is a test", finding.rationale());
        assertEquals("Fix this", finding.suggestion());
        assertEquals(Severity.HIGH, finding.severity());
        assertEquals("Test.java", finding.filePath());
        assertEquals(1, finding.lineStart());
        assertEquals(1, finding.lineEnd());
        assertEquals(ReviewerType.SECURITY, finding.reviewer());
    }

    @Test
    void testParseFindings_withEmptyFindings() {
        // Test case: Empty findings
        String emptyFindingsResponse = """
            {
              "findings": [],
              "summary": "No issues found"
            }
            """;

        List<Finding> findings = JsonUtils.parseFindings(emptyFindingsResponse, ReviewerType.PERFORMANCE, "TestFile.java");
        
        assertNotNull(findings);
        assertEquals(0, findings.size());
    }

    @Test
    void testParseFindings_withMarkdownCodeBlock() {
        // Test case: JSON within markdown code block without language specification
        String markdownResponse = """
            Here's the analysis:

            ```
            {
              "findings": [
                {
                  "title": "Performance Issue",
                  "rationale": "Inefficient loop detected",
                  "suggestion": "Use stream API",
                  "severity": "MEDIUM",
                  "filePath": "Performance.java",
                  "lineStart": 10,
                  "lineEnd": 15
                }
              ],
              "summary": "Performance optimization needed"
            }
            ```

            This should be addressed.
            """;

        List<Finding> findings = JsonUtils.parseFindings(markdownResponse, ReviewerType.PERFORMANCE, "TestFile.java");
        
        assertNotNull(findings);
        assertEquals(1, findings.size());
        
        Finding finding = findings.get(0);
        assertEquals("Performance Issue", finding.title());
        assertEquals("Inefficient loop detected", finding.rationale());
        assertEquals("Use stream API", finding.suggestion());
        assertEquals(Severity.MEDIUM, finding.severity());
        assertEquals("Performance.java", finding.filePath());
        assertEquals(10, finding.lineStart());
        assertEquals(15, finding.lineEnd());
        assertEquals(ReviewerType.PERFORMANCE, finding.reviewer());
    }

    @Test
    void testParseFindings_withInvalidJson() {
        // Test case: Invalid JSON should return empty list
        String invalidJson = "This is not valid JSON at all";
        
        List<Finding> findings = JsonUtils.parseFindings(invalidJson, ReviewerType.CLEAN_CODE, "TestFile.java");
        
        assertNotNull(findings);
        assertEquals(0, findings.size());
    }

    @Test
    void testParseFindings_withFilepathFallback() {
        // Test case: AI returns empty filePath, should use provided fallback
        String responseWithEmptyFilePath = """
            {
              "findings": [
                {
                  "title": "Test Finding",
                  "rationale": "Test rationale",
                  "suggestion": "Test suggestion",
                  "severity": "LOW",
                  "filePath": "",
                  "lineStart": 5,
                  "lineEnd": 5
                }
              ],
              "summary": "Test summary"
            }
            """;

        List<Finding> findings = JsonUtils.parseFindings(responseWithEmptyFilePath, ReviewerType.SECURITY, "FallbackFile.java");
        
        assertNotNull(findings);
        assertEquals(1, findings.size());
        
        Finding finding = findings.get(0);
        assertEquals("FallbackFile.java", finding.filePath());
    }

    @Test
    void testParseFindings_withInvalidSeverity() {
        // Test case: Invalid severity should default to INFO
        String responseWithInvalidSeverity = """
            {
              "findings": [
                {
                  "title": "Test Finding",
                  "rationale": "Test rationale",
                  "suggestion": "Test suggestion",
                  "severity": "INVALID_SEVERITY",
                  "filePath": "Test.java",
                  "lineStart": 1,
                  "lineEnd": 1
                }
              ],
              "summary": "Test summary"
            }
            """;

        List<Finding> findings = JsonUtils.parseFindings(responseWithInvalidSeverity, ReviewerType.CLEAN_CODE, "TestFile.java");
        
        assertNotNull(findings);
        assertEquals(1, findings.size());
        
        Finding finding = findings.get(0);
        assertEquals(Severity.INFO, finding.severity());
    }

    @Test
    void testParseFindings_withMultipleFindings() {
        // Test case: Multiple findings in the response
        String multipleFindingsResponse = """
            {
              "findings": [
                {
                  "title": "First Issue",
                  "rationale": "First rationale",
                  "suggestion": "First suggestion",
                  "severity": "HIGH",
                  "filePath": "Test1.java",
                  "lineStart": 1,
                  "lineEnd": 1
                },
                {
                  "title": "Second Issue",
                  "rationale": "Second rationale",
                  "suggestion": "Second suggestion",
                  "severity": "LOW",
                  "filePath": "Test2.java",
                  "lineStart": 2,
                  "lineEnd": 2
                }
              ],
              "summary": "Multiple issues found"
            }
            """;

        List<Finding> findings = JsonUtils.parseFindings(multipleFindingsResponse, ReviewerType.SECURITY, "TestFile.java");
        
        assertNotNull(findings);
        assertEquals(2, findings.size());
        
        Finding firstFinding = findings.get(0);
        assertEquals("First Issue", firstFinding.title());
        assertEquals(Severity.HIGH, firstFinding.severity());
        
        Finding secondFinding = findings.get(1);
        assertEquals("Second Issue", secondFinding.title());
        assertEquals(Severity.LOW, secondFinding.severity());
    }
}
