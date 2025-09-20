package com.hrpd.codereview.service;

import com.hrpd.codereview.model.Finding;
import com.hrpd.codereview.model.ReviewResult;
import com.hrpd.codereview.model.ReviewerType;
import com.hrpd.codereview.model.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AggregatorServiceImpl.
 */
class AggregatorServiceImplTest {

    private AggregatorServiceImpl aggregatorService;

    @BeforeEach
    void setUp() {
        aggregatorService = new AggregatorServiceImpl();
    }

    @Test
    void testMerge_withEmptyResults() {
        // Arrange
        List<ReviewResult> parts = List.of();

        // Act
        ReviewResult result = aggregatorService.merge(parts);

        // Assert
        assertNotNull(result);
        assertTrue(result.findings().isEmpty());
        assertEquals("Findings: 0 (BLOCKER=0, HIGH=0)", result.summary());
    }

    @Test
    void testMerge_withSingleResult() {
        // Arrange
        Finding finding1 = new Finding("TestFile.java", 1, 5, "Test Issue", 
                                     "Test rationale", "Test suggestion", 
                                     Severity.HIGH, ReviewerType.SECURITY);
        ReviewResult part1 = new ReviewResult(List.of(finding1), "Security review complete");
        List<ReviewResult> parts = List.of(part1);

        // Act
        ReviewResult result = aggregatorService.merge(parts);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.findings().size());
        assertEquals("Findings: 1 (BLOCKER=0, HIGH=1)", result.summary());
        assertEquals(finding1, result.findings().get(0));
    }

    @Test
    void testMerge_withMultipleResults() {
        // Arrange
        Finding finding1 = new Finding("TestFile.java", 1, 5, "Security Issue", 
                                     "Security rationale", "Security suggestion", 
                                     Severity.HIGH, ReviewerType.SECURITY);
        Finding finding2 = new Finding("TestFile.java", 10, 15, "Performance Issue", 
                                     "Performance rationale", "Performance suggestion", 
                                     Severity.MEDIUM, ReviewerType.PERFORMANCE);
        Finding finding3 = new Finding("AnotherFile.java", 20, 25, "Clean Code Issue", 
                                     "Clean code rationale", "Clean code suggestion", 
                                     Severity.LOW, ReviewerType.CLEAN_CODE);

        ReviewResult part1 = new ReviewResult(List.of(finding1), "Security review complete");
        ReviewResult part2 = new ReviewResult(List.of(finding2), "Performance review complete");
        ReviewResult part3 = new ReviewResult(List.of(finding3), "Clean code review complete");
        List<ReviewResult> parts = List.of(part1, part2, part3);

        // Act
        ReviewResult result = aggregatorService.merge(parts);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.findings().size());
        assertEquals("Findings: 3 (BLOCKER=0, HIGH=1)", result.summary());
        
        // Verify all findings are present
        assertTrue(result.findings().contains(finding1));
        assertTrue(result.findings().contains(finding2));
        assertTrue(result.findings().contains(finding3));
    }

    @Test
    void testMerge_withDuplicateFindings() {
        // Arrange
        Finding finding1 = new Finding("TestFile.java", 1, 5, "Duplicate Issue", 
                                     "First rationale", "First suggestion", 
                                     Severity.MEDIUM, ReviewerType.SECURITY);
        Finding finding2 = new Finding("TestFile.java", 1, 5, "Duplicate Issue", 
                                     "Second rationale", "Second suggestion", 
                                     Severity.HIGH, ReviewerType.PERFORMANCE);

        ReviewResult part1 = new ReviewResult(List.of(finding1), "Security review complete");
        ReviewResult part2 = new ReviewResult(List.of(finding2), "Performance review complete");
        List<ReviewResult> parts = List.of(part1, part2);

        // Act
        ReviewResult result = aggregatorService.merge(parts);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.findings().size());
        assertEquals("Findings: 1 (BLOCKER=0, HIGH=1)", result.summary());
        
        // Verify the higher severity finding is kept
        Finding mergedFinding = result.findings().get(0);
        assertEquals("Duplicate Issue", mergedFinding.title());
        assertEquals(Severity.HIGH, mergedFinding.severity());
        assertEquals(ReviewerType.PERFORMANCE, mergedFinding.reviewer());
    }

    @Test
    void testMerge_withBlockerSeverity() {
        // Arrange
        Finding finding1 = new Finding("TestFile.java", 1, 5, "Critical Issue", 
                                     "Critical rationale", "Critical suggestion", 
                                     Severity.BLOCKER, ReviewerType.SECURITY);
        Finding finding2 = new Finding("TestFile.java", 10, 15, "High Issue", 
                                     "High rationale", "High suggestion", 
                                     Severity.HIGH, ReviewerType.PERFORMANCE);

        ReviewResult part1 = new ReviewResult(List.of(finding1), "Security review complete");
        ReviewResult part2 = new ReviewResult(List.of(finding2), "Performance review complete");
        List<ReviewResult> parts = List.of(part1, part2);

        // Act
        ReviewResult result = aggregatorService.merge(parts);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.findings().size());
        assertEquals("Findings: 2 (BLOCKER=1, HIGH=1)", result.summary());
        
        // Verify both findings are present
        assertTrue(result.findings().contains(finding1));
        assertTrue(result.findings().contains(finding2));
    }

    @Test
    void testMerge_withEmptyFindingsInResults() {
        // Arrange
        ReviewResult part1 = new ReviewResult(List.of(), "No security issues found");
        ReviewResult part2 = new ReviewResult(List.of(), "No performance issues found");
        List<ReviewResult> parts = List.of(part1, part2);

        // Act
        ReviewResult result = aggregatorService.merge(parts);

        // Assert
        assertNotNull(result);
        assertTrue(result.findings().isEmpty());
        assertEquals("Findings: 0 (BLOCKER=0, HIGH=0)", result.summary());
    }

    @Test
    void testMerge_withMixedSeverities() {
        // Arrange
        Finding blocker = new Finding("TestFile.java", 1, 5, "Blocker Issue", 
                                    "Blocker rationale", "Blocker suggestion", 
                                    Severity.BLOCKER, ReviewerType.SECURITY);
        Finding high = new Finding("TestFile.java", 10, 15, "High Issue", 
                                 "High rationale", "High suggestion", 
                                 Severity.HIGH, ReviewerType.PERFORMANCE);
        Finding medium = new Finding("TestFile.java", 20, 25, "Medium Issue", 
                                   "Medium rationale", "Medium suggestion", 
                                   Severity.MEDIUM, ReviewerType.CLEAN_CODE);
        Finding low = new Finding("TestFile.java", 30, 35, "Low Issue", 
                                "Low rationale", "Low suggestion", 
                                Severity.LOW, ReviewerType.SECURITY);
        Finding info = new Finding("TestFile.java", 40, 45, "Info Issue", 
                                 "Info rationale", "Info suggestion", 
                                 Severity.INFO, ReviewerType.PERFORMANCE);

        ReviewResult part1 = new ReviewResult(List.of(blocker, medium), "Security review complete");
        ReviewResult part2 = new ReviewResult(List.of(high, low, info), "Performance review complete");
        List<ReviewResult> parts = List.of(part1, part2);

        // Act
        ReviewResult result = aggregatorService.merge(parts);

        // Assert
        assertNotNull(result);
        assertEquals(5, result.findings().size());
        assertEquals("Findings: 5 (BLOCKER=1, HIGH=1)", result.summary());
        
        // Verify all findings are present
        assertTrue(result.findings().contains(blocker));
        assertTrue(result.findings().contains(high));
        assertTrue(result.findings().contains(medium));
        assertTrue(result.findings().contains(low));
        assertTrue(result.findings().contains(info));
    }

    @Test
    void testMerge_withSameLocationDifferentTitles() {
        // Arrange
        Finding finding1 = new Finding("TestFile.java", 1, 5, "First Issue", 
                                     "First rationale", "First suggestion", 
                                     Severity.HIGH, ReviewerType.SECURITY);
        Finding finding2 = new Finding("TestFile.java", 1, 5, "Second Issue", 
                                     "Second rationale", "Second suggestion", 
                                     Severity.MEDIUM, ReviewerType.PERFORMANCE);

        ReviewResult part1 = new ReviewResult(List.of(finding1), "Security review complete");
        ReviewResult part2 = new ReviewResult(List.of(finding2), "Performance review complete");
        List<ReviewResult> parts = List.of(part1, part2);

        // Act
        ReviewResult result = aggregatorService.merge(parts);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.findings().size());
        assertEquals("Findings: 2 (BLOCKER=0, HIGH=1)", result.summary());
        
        // Verify both findings are present (different titles, so not considered duplicates)
        assertTrue(result.findings().contains(finding1));
        assertTrue(result.findings().contains(finding2));
    }

    @Test
    void testMerge_withNullResults() {
        // Arrange
        List<ReviewResult> parts = List.of(
                new ReviewResult(List.of(), "Empty result"),
                new ReviewResult(List.of(), "Another empty result")
        );

        // Act
        ReviewResult result = aggregatorService.merge(parts);

        // Assert
        assertNotNull(result);
        assertTrue(result.findings().isEmpty());
        assertEquals("Findings: 0 (BLOCKER=0, HIGH=0)", result.summary());
    }
}
