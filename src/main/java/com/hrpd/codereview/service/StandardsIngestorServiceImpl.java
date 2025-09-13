package com.hrpd.codereview.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads standards markdown into the vector store at startup.
 */
@Slf4j
@RequiredArgsConstructor
public class StandardsIngestorServiceImpl implements StandardsIngestorService {

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void ingestFromClasspath() throws Exception {
        log.info("Starting standards ingestion from classpath");
        var resolver = new PathMatchingResourcePatternResolver();
        Resource[] standardFiles = resolver.getResources("classpath:standards/*.md");
        if (standardFiles == null || standardFiles.length == 0) {
            log.warn("No standard files found in classpath:standards/*.md");
            return;
        }

        // Get existing content hashes for duplicate detection
        Set<String> existingHashes = getExistingContentHashes();
        log.info("Found {} existing document chunks in database", existingHashes.size());

        List<Document> newDocs = new ArrayList<>();
        int skippedCount = 0;
        
        for (Resource r : standardFiles) {
            String content = new String(r.getContentAsByteArray(), StandardCharsets.UTF_8);
            String[] chunks = content.split("\n##\\s"); // chunk by second-level headings
            for (String chunk : chunks) {
                String c = chunk.strip();
                if (c.isEmpty()) continue;
                
                String contentHash = calculateContentHash(c, r.getFilename());
                if (existingHashes.contains(contentHash)) {
                    skippedCount++;
                    log.debug("Skipping duplicate document chunk from {}", r.getFilename());
                    continue;
                }
                
                Document doc = new Document(c, Map.of(
                        "source", r.getFilename(),
                        "category", inferCategory(r.getFilename()),
                        "content_hash", contentHash
                ));
                newDocs.add(doc);
            }
        }
        
        if (!newDocs.isEmpty()) {
            vectorStore.add(newDocs);
            log.info("Ingested {} new document chunks from {} files (skipped {} duplicates)", 
                    newDocs.size(), standardFiles.length, skippedCount);
        } else {
            log.info("No new documents to ingest (all {} chunks already exist)", skippedCount);
        }
    }

    @Override
    public void reingestAll() throws Exception {
        log.info("Starting complete re-ingestion of standards from classpath");
        
        // Clear all existing documents
        clearAllDocuments();
        
        // Re-ingest everything
        ingestFromClasspath();
        
        log.info("Complete re-ingestion finished");
    }

    /**
     * Clears all documents from the vector store.
     */
    private void clearAllDocuments() {
        try {
            int deletedCount = jdbcTemplate.update("DELETE FROM ai_documents");
            log.info("Cleared {} existing documents from database", deletedCount);
        } catch (Exception e) {
            log.error("Failed to clear existing documents: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to clear existing documents", e);
        }
    }

    private String inferCategory(String fn) {
        if (fn == null) {
            return "general";
        }
        String f = fn.toLowerCase();
        if (f.contains("security")) {
            return "security";
        }
        if (f.contains("performance")) {
            return "performance";
        }
        if (f.contains("testing")) {
            return "testing";
        }
        if (f.contains("concurrency")) {
            return "concurrency";
        }
        return "general";
    }

    /**
     * Retrieves existing content hashes from the database to detect duplicates.
     */
    private Set<String> getExistingContentHashes() {
        try {
            List<String> hashes = jdbcTemplate.queryForList(
                "SELECT content_hash FROM ai_documents WHERE content_hash IS NOT NULL", 
                String.class
            );
            log.debug("Retrieved {} existing content hashes from database", hashes.size());
            return new HashSet<>(hashes);
        } catch (Exception e) {
            log.warn("Failed to retrieve existing content hashes, will ingest all documents: {}", e.getMessage());
            return new HashSet<>();
        }
    }

    /**
     * Calculates a SHA-256 hash of the content combined with source filename for uniqueness.
     */
    private String calculateContentHash(String content, String source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = source + "|" + content;
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            // Fallback to a simple hash
            return String.valueOf((source + content).hashCode());
        }
    }
}
