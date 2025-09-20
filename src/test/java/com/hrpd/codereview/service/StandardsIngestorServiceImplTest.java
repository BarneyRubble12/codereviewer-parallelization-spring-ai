package com.hrpd.codereview.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StandardsIngestorServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class StandardsIngestorServiceImplTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private Resource resource1;

    @Mock
    private Resource resource2;

    private StandardsIngestorServiceImpl ingestorService;

    @BeforeEach
    void setUp() {
        ingestorService = new StandardsIngestorServiceImpl(vectorStore, jdbcTemplate);
    }

    @Test
    void testReingestAll() throws Exception {
        // Arrange
        when(jdbcTemplate.update(anyString())).thenReturn(5);
        when(jdbcTemplate.queryForList(anyString(), eq(String.class))).thenReturn(List.of());

        // Mock resource behavior - not needed for this test

        // Act
        ingestorService.reingestAll();

        // Assert
        verify(jdbcTemplate).update("DELETE FROM ai_documents");
        verify(vectorStore).add(any(List.class));
    }

    @Test
    void testReingestAll_withDatabaseError() {
        // Arrange
        when(jdbcTemplate.update(anyString())).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            ingestorService.reingestAll();
        });

        verify(jdbcTemplate).update("DELETE FROM ai_documents");
    }

    @Test
    void testIngestFromClasspath_withNoFiles() throws Exception {
        // Arrange
        when(jdbcTemplate.queryForList(anyString(), eq(String.class))).thenReturn(List.of());

        // Act
        ingestorService.ingestFromClasspath();

        // Assert
        verify(jdbcTemplate).queryForList(anyString(), eq(String.class));
    }

    @Test
    void testIngestFromClasspath_withExistingDocuments() throws Exception {
        // Arrange
        String existingHash = "existing-hash-123";
        when(jdbcTemplate.queryForList(anyString(), eq(String.class))).thenReturn(List.of(existingHash));

        // Act
        ingestorService.ingestFromClasspath();

        // Assert
        verify(jdbcTemplate).queryForList(anyString(), eq(String.class));
    }

    @Test
    void testIngestFromClasspath_withNewDocuments() throws Exception {
        // Arrange
        when(jdbcTemplate.queryForList(anyString(), eq(String.class))).thenReturn(List.of());

        // Act
        ingestorService.ingestFromClasspath();

        // Assert
        verify(vectorStore).add(any(List.class));
        verify(jdbcTemplate).queryForList(anyString(), eq(String.class));
    }

    // Note: Tests for private methods (inferCategory, calculateContentHash, getExistingContentHashes)
    // are not included as they are implementation details. These methods are tested indirectly
    // through the public methods that use them.
}