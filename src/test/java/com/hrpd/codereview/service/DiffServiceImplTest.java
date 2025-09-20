package com.hrpd.codereview.service;

import com.hrpd.codereview.model.DiffHunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DiffServiceImpl.
 */
class DiffServiceImplTest {

    private DiffServiceImpl diffService;

    @BeforeEach
    void setUp() {
        diffService = new DiffServiceImpl();
    }

    @Test
    void testParseUnifiedPatch_withValidDiff() {
        // Arrange
        String patch = """
                diff --git a/src/main/java/TestFile.java b/src/main/java/TestFile.java
                index 1234567..abcdefg 100644
                --- a/src/main/java/TestFile.java
                +++ b/src/main/java/TestFile.java
                @@ -1,3 +1,4 @@
                 package com.example;
                 
                +import java.util.List;
                 public class TestFile {
                 }
                @@ -5,7 +6,8 @@ public class TestFile {
                     public void method() {
                +        // New comment
                     System.out.println("Hello");
                 }
                """;

        // Act
        List<DiffHunk> result = diffService.parseUnifiedPatch(patch);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        
        DiffHunk firstHunk = result.get(0);
        assertEquals("src/main/java/TestFile.java", firstHunk.filePath());
        assertEquals(0, firstHunk.start());
        assertEquals(0, firstHunk.end());
        assertTrue(firstHunk.patch().contains("@@ -1,3 +1,4 @@"));
        assertTrue(firstHunk.patch().contains("+import java.util.List;"));
        
        DiffHunk secondHunk = result.get(1);
        assertEquals("src/main/java/TestFile.java", secondHunk.filePath());
        assertEquals(0, secondHunk.start());
        assertEquals(0, secondHunk.end());
        assertTrue(secondHunk.patch().contains("@@ -5,7 +6,8 @@"));
        assertTrue(secondHunk.patch().contains("+        // New comment"));
    }

    @Test
    void testParseUnifiedPatch_withMultipleFiles() {
        // Arrange
        String patch = """
                diff --git a/src/main/java/File1.java b/src/main/java/File1.java
                index 1111111..2222222 100644
                --- a/src/main/java/File1.java
                +++ b/src/main/java/File1.java
                @@ -1,3 +1,4 @@
                 package com.example;
                +import java.util.List;
                 
                diff --git a/src/main/java/File2.java b/src/main/java/File2.java
                index 3333333..4444444 100644
                --- a/src/main/java/File2.java
                +++ b/src/main/java/File2.java
                @@ -1,3 +1,4 @@
                 package com.example;
                +import java.util.Map;
                """;

        // Act
        List<DiffHunk> result = diffService.parseUnifiedPatch(patch);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        
        DiffHunk firstHunk = result.get(0);
        assertEquals("src/main/java/File1.java", firstHunk.filePath());
        assertTrue(firstHunk.patch().contains("+import java.util.List;"));
        
        DiffHunk secondHunk = result.get(1);
        assertEquals("src/main/java/File2.java", secondHunk.filePath());
        assertTrue(secondHunk.patch().contains("+import java.util.Map;"));
    }

    @Test
    void testParseUnifiedPatch_withEmptyPatch() {
        // Arrange
        String patch = "";

        // Act
        List<DiffHunk> result = diffService.parseUnifiedPatch(patch);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        
        DiffHunk hunk = result.get(0);
        assertEquals("all", hunk.filePath());
        assertEquals("", hunk.patch());
    }

    @Test
    void testParseUnifiedPatch_withNoHunks() {
        // Arrange
        String patch = """
                diff --git a/src/main/java/TestFile.java b/src/main/java/TestFile.java
                index 1234567..abcdefg 100644
                --- a/src/main/java/TestFile.java
                +++ b/src/main/java/TestFile.java
                """;

        // Act
        List<DiffHunk> result = diffService.parseUnifiedPatch(patch);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        
        DiffHunk hunk = result.get(0);
        assertEquals("all", hunk.filePath());
        assertEquals(patch, hunk.patch());
    }

    @Test
    void testParseUnifiedPatch_withMalformedDiff() {
        // Arrange
        String patch = "This is not a valid diff format";

        // Act
        List<DiffHunk> result = diffService.parseUnifiedPatch(patch);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        
        DiffHunk hunk = result.get(0);
        assertEquals("all", hunk.filePath());
        assertEquals(patch, hunk.patch());
    }

    @Test
    void testParseUnifiedPatch_withComplexDiff() {
        // Arrange
        String patch = """
                diff --git a/src/main/java/ComplexFile.java b/src/main/java/ComplexFile.java
                index 1234567..abcdefg 100644
                --- a/src/main/java/ComplexFile.java
                +++ b/src/main/java/ComplexFile.java
                @@ -1,10 +1,12 @@
                 package com.example;
                 
                +import java.util.List;
                +import java.util.Map;
                 public class ComplexFile {
                     private String name;
                 
                @@ -5,15 +7,20 @@ public class ComplexFile {
                     public void method1() {
                +        // Added comment
                     System.out.println("Method 1");
                 }
                 
                @@ -20,25 +25,30 @@ public class ComplexFile {
                     public void method2() {
                +        // Another comment
                     System.out.println("Method 2");
                 }
                """;

        // Act
        List<DiffHunk> result = diffService.parseUnifiedPatch(patch);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        
        // Verify all hunks have the correct file path
        for (DiffHunk hunk : result) {
            assertEquals("src/main/java/ComplexFile.java", hunk.filePath());
        }
        
        // Verify first hunk contains import additions
        assertTrue(result.get(0).patch().contains("+import java.util.List;"));
        assertTrue(result.get(0).patch().contains("+import java.util.Map;"));
        
        // Verify second hunk contains method1 changes
        assertTrue(result.get(1).patch().contains("+        // Added comment"));
        
        // Verify third hunk contains method2 changes
        assertTrue(result.get(2).patch().contains("+        // Another comment"));
    }

    @Test
    void testParseUnifiedPatch_withNullPatch() {
        // Arrange
        String patch = null;

        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            diffService.parseUnifiedPatch(patch);
        });
    }
}
