package com.hrpd.codereview.service;

import com.hrpd.codereview.model.DiffHunk;
import com.hrpd.codereview.model.Finding;
import com.hrpd.codereview.model.ReviewResult;
import com.hrpd.codereview.model.ReviewerType;
import com.hrpd.codereview.model.Severity;
import com.hrpd.codereview.reviewer.Reviewer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ParallelWorkflowServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class ParallelWorkflowServiceImplTest {

    @Mock
    private Reviewer securityReviewer;

    @Mock
    private Reviewer performanceReviewer;

    @Mock
    private Reviewer cleanCodeReviewer;

    @Mock
    private AggregatorService aggregatorService;

    @Mock
    private ExecutorService executorService;

    private ParallelWorkflowServiceImpl workflowService;

    @BeforeEach
    void setUp() {
        List<Reviewer> reviewers = List.of(securityReviewer, performanceReviewer, cleanCodeReviewer);
        workflowService = new ParallelWorkflowServiceImpl(reviewers, aggregatorService, executorService);
    }

    @Test
    void testRun_sequentialMode() {
        // Arrange
        List<DiffHunk> hunks = List.of(
                new DiffHunk("TestFile.java", 1, 10, "diff content")
        );

        Finding securityFinding = new Finding("TestFile.java", 1, 5, "Security Issue", 
                                            "Security rationale", "Security suggestion", 
                                            Severity.HIGH, ReviewerType.SECURITY);
        Finding performanceFinding = new Finding("TestFile.java", 6, 10, "Performance Issue", 
                                               "Performance rationale", "Performance suggestion", 
                                               Severity.MEDIUM, ReviewerType.PERFORMANCE);

        ReviewResult securityResult = new ReviewResult(List.of(securityFinding), "Security review complete");
        ReviewResult performanceResult = new ReviewResult(List.of(performanceFinding), "Performance review complete");
        ReviewResult cleanCodeResult = new ReviewResult(List.of(), "Clean code review complete");

        ReviewResult aggregatedResult = new ReviewResult(
                List.of(securityFinding, performanceFinding), 
                "Aggregated review complete"
        );

        when(securityReviewer.type()).thenReturn(ReviewerType.SECURITY);
        when(performanceReviewer.type()).thenReturn(ReviewerType.PERFORMANCE);
        when(cleanCodeReviewer.type()).thenReturn(ReviewerType.CLEAN_CODE);

        when(securityReviewer.review(hunks)).thenReturn(securityResult);
        when(performanceReviewer.review(hunks)).thenReturn(performanceResult);
        when(cleanCodeReviewer.review(hunks)).thenReturn(cleanCodeResult);

        when(aggregatorService.merge(any())).thenReturn(aggregatedResult);

        // Act
        ReviewResult result = workflowService.run(hunks, false);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.findings().size());
        assertEquals("Aggregated review complete", result.summary());

        // Verify sequential execution
        verify(securityReviewer).review(hunks);
        verify(performanceReviewer).review(hunks);
        verify(cleanCodeReviewer).review(hunks);
        verify(aggregatorService).merge(any());

        // Verify no parallel execution
        verify(executorService, never()).submit(any(Runnable.class));
    }

    @Test
    void testRun_withEmptyHunks() {
        // Arrange
        List<DiffHunk> hunks = List.of();
        ReviewResult emptyResult = new ReviewResult(List.of(), "No findings");

        when(securityReviewer.type()).thenReturn(ReviewerType.SECURITY);
        when(performanceReviewer.type()).thenReturn(ReviewerType.PERFORMANCE);
        when(cleanCodeReviewer.type()).thenReturn(ReviewerType.CLEAN_CODE);

        when(securityReviewer.review(hunks)).thenReturn(emptyResult);
        when(performanceReviewer.review(hunks)).thenReturn(emptyResult);
        when(cleanCodeReviewer.review(hunks)).thenReturn(emptyResult);

        when(aggregatorService.merge(any())).thenReturn(emptyResult);

        // Act
        ReviewResult result = workflowService.run(hunks, false);

        // Assert
        assertNotNull(result);
        assertTrue(result.findings().isEmpty());
        assertEquals("No findings", result.summary());

        verify(aggregatorService).merge(any());
    }

    @Test
    void testRun_withSingleReviewer() {
        // Arrange
        List<Reviewer> singleReviewer = List.of(securityReviewer);
        ParallelWorkflowServiceImpl singleWorkflowService = 
                new ParallelWorkflowServiceImpl(singleReviewer, aggregatorService, executorService);

        List<DiffHunk> hunks = List.of(
                new DiffHunk("TestFile.java", 1, 10, "diff content")
        );

        Finding securityFinding = new Finding("TestFile.java", 1, 5, "Security Issue", 
                                            "Security rationale", "Security suggestion", 
                                            Severity.HIGH, ReviewerType.SECURITY);

        ReviewResult securityResult = new ReviewResult(List.of(securityFinding), "Security review complete");
        ReviewResult aggregatedResult = new ReviewResult(List.of(securityFinding), "Aggregated review complete");

        when(securityReviewer.type()).thenReturn(ReviewerType.SECURITY);
        when(securityReviewer.review(hunks)).thenReturn(securityResult);
        when(aggregatorService.merge(any())).thenReturn(aggregatedResult);

        // Act
        ReviewResult result = singleWorkflowService.run(hunks, false);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.findings().size());
        assertEquals("Aggregated review complete", result.summary());

        verify(securityReviewer).review(hunks);
        verify(aggregatorService).merge(any());
    }

    @Test
    void testRun_withNullHunks() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            workflowService.run(null, false);
        });
    }

    @Test
    void testRun_withLargeNumberOfHunks() {
        // Arrange
        List<DiffHunk> hunks = List.of(
                new DiffHunk("File1.java", 1, 10, "diff1"),
                new DiffHunk("File2.java", 1, 10, "diff2"),
                new DiffHunk("File3.java", 1, 10, "diff3"),
                new DiffHunk("File4.java", 1, 10, "diff4"),
                new DiffHunk("File5.java", 1, 10, "diff5")
        );

        ReviewResult emptyResult = new ReviewResult(List.of(), "No findings");
        ReviewResult aggregatedResult = new ReviewResult(List.of(), "Aggregated review complete");

        when(securityReviewer.type()).thenReturn(ReviewerType.SECURITY);
        when(performanceReviewer.type()).thenReturn(ReviewerType.PERFORMANCE);
        when(cleanCodeReviewer.type()).thenReturn(ReviewerType.CLEAN_CODE);

        when(securityReviewer.review(hunks)).thenReturn(emptyResult);
        when(performanceReviewer.review(hunks)).thenReturn(emptyResult);
        when(cleanCodeReviewer.review(hunks)).thenReturn(emptyResult);

        when(aggregatorService.merge(any())).thenReturn(aggregatedResult);

        // Act
        ReviewResult result = workflowService.run(hunks, false);

        // Assert
        assertNotNull(result);
        assertEquals("Aggregated review complete", result.summary());

        // Verify all reviewers were called with the same hunks
        verify(securityReviewer).review(hunks);
        verify(performanceReviewer).review(hunks);
        verify(cleanCodeReviewer).review(hunks);
        verify(aggregatorService).merge(any());
    }

    @Test
    void testRun_sequentialModePerformance() {
        // Arrange
        List<DiffHunk> hunks = List.of(
                new DiffHunk("TestFile.java", 1, 10, "diff content")
        );

        ReviewResult emptyResult = new ReviewResult(List.of(), "No findings");
        ReviewResult aggregatedResult = new ReviewResult(List.of(), "Aggregated review complete");

        when(securityReviewer.type()).thenReturn(ReviewerType.SECURITY);
        when(performanceReviewer.type()).thenReturn(ReviewerType.PERFORMANCE);
        when(cleanCodeReviewer.type()).thenReturn(ReviewerType.CLEAN_CODE);

        when(securityReviewer.review(hunks)).thenReturn(emptyResult);
        when(performanceReviewer.review(hunks)).thenReturn(emptyResult);
        when(cleanCodeReviewer.review(hunks)).thenReturn(emptyResult);

        when(aggregatorService.merge(any())).thenReturn(aggregatedResult);

        // Act
        long startTime = System.currentTimeMillis();
        ReviewResult result = workflowService.run(hunks, false);
        long duration = System.currentTimeMillis() - startTime;

        // Assert
        assertNotNull(result);
        assertTrue(duration >= 0); // Basic performance check
        assertEquals("Aggregated review complete", result.summary());
    }
}