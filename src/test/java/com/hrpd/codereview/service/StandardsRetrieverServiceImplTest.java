package com.hrpd.codereview.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StandardsRetrieverServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class StandardsRetrieverServiceImplTest {

    @Mock
    private VectorStore vectorStore;

    private StandardsRetrieverServiceImpl retrieverService;

    @BeforeEach
    void setUp() {
        retrieverService = new StandardsRetrieverServiceImpl(vectorStore);
    }

    @Test
    void testRetrieveContext_withValidQuery() {
        // Arrange
        String query = "java security best practices";
        int topK = 5;
        String categoryHint = "security";

        Document doc1 = new Document("Security standard 1", Map.of("category", "security"));
        Document doc2 = new Document("Security standard 2", Map.of("category", "security"));
        List<Document> documents = List.of(doc1, doc2);

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(documents);

        // Act
        String result = retrieverService.retrieveContext(query, topK, categoryHint);

        // Assert
        assertNotNull(result);
        assertEquals("- Security standard 1\n- Security standard 2", result);
        
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void testRetrieveContext_withoutCategoryHint() {
        // Arrange
        String query = "java performance optimization";
        int topK = 3;
        String categoryHint = null;

        Document doc1 = new Document("Performance standard 1", Map.of("category", "performance"));
        Document doc2 = new Document("Performance standard 2", Map.of("category", "performance"));
        List<Document> documents = List.of(doc1, doc2);

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(documents);

        // Act
        String result = retrieverService.retrieveContext(query, topK, categoryHint);

        // Assert
        assertNotNull(result);
        assertEquals("- Performance standard 1\n- Performance standard 2", result);
        
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void testRetrieveContext_withEmptyCategoryHint() {
        // Arrange
        String query = "java clean code";
        int topK = 4;
        String categoryHint = "";

        Document doc1 = new Document("Clean code standard 1", Map.of("category", "general"));
        List<Document> documents = List.of(doc1);

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(documents);

        // Act
        String result = retrieverService.retrieveContext(query, topK, categoryHint);

        // Assert
        assertNotNull(result);
        assertEquals("- Clean code standard 1", result);
        
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void testRetrieveContext_withBlankCategoryHint() {
        // Arrange
        String query = "java testing";
        int topK = 2;
        String categoryHint = "   ";

        Document doc1 = new Document("Testing standard 1", Map.of("category", "testing"));
        List<Document> documents = List.of(doc1);

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(documents);

        // Act
        String result = retrieverService.retrieveContext(query, topK, categoryHint);

        // Assert
        assertNotNull(result);
        assertEquals("- Testing standard 1", result);
        
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void testRetrieveContext_withEmptyResults() {
        // Arrange
        String query = "nonexistent query";
        int topK = 5;
        String categoryHint = "nonexistent";

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        // Act
        String result = retrieverService.retrieveContext(query, topK, categoryHint);

        // Assert
        assertNotNull(result);
        assertEquals("", result);
        
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void testRetrieveContext_withSingleDocument() {
        // Arrange
        String query = "java concurrency";
        int topK = 1;
        String categoryHint = "concurrency";

        Document doc = new Document("Concurrency standard", Map.of("category", "concurrency"));
        List<Document> documents = List.of(doc);

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(documents);

        // Act
        String result = retrieverService.retrieveContext(query, topK, categoryHint);

        // Assert
        assertNotNull(result);
        assertEquals("- Concurrency standard", result);
        
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void testRetrieveContext_withMultipleDocuments() {
        // Arrange
        String query = "java best practices";
        int topK = 10;
        String categoryHint = "general";

        Document doc1 = new Document("Best practice 1", Map.of("category", "general"));
        Document doc2 = new Document("Best practice 2", Map.of("category", "general"));
        Document doc3 = new Document("Best practice 3", Map.of("category", "general"));
        List<Document> documents = List.of(doc1, doc2, doc3);

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(documents);

        // Act
        String result = retrieverService.retrieveContext(query, topK, categoryHint);

        // Assert
        assertNotNull(result);
        assertEquals("- Best practice 1\n- Best practice 2\n- Best practice 3", result);
        
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void testRetrieveContext_withZeroTopK() {
        // Arrange
        String query = "java security";
        int topK = 0;
        String categoryHint = "security";

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        // Act
        String result = retrieverService.retrieveContext(query, topK, categoryHint);

        // Assert
        assertNotNull(result);
        assertEquals("", result);
        
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void testRetrieveContext_withEmptyQuery() {
        // Arrange
        String query = "";
        int topK = 5;
        String categoryHint = "general";

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        // Act
        String result = retrieverService.retrieveContext(query, topK, categoryHint);

        // Assert
        assertNotNull(result);
        assertEquals("", result);
        
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void testRetrieveContext_withEmptyQuery() {
        // Arrange
        String query = "";
        int topK = 5;
        String categoryHint = "general";

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        // Act
        String result = retrieverService.retrieveContext(query, topK, categoryHint);

        // Assert
        assertNotNull(result);
        assertEquals("", result);
        
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }
}
