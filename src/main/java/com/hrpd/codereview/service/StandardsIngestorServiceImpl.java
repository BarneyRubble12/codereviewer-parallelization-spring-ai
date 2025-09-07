package com.hrpd.codereview.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads standards markdown into the vector store at startup.
 */
public class StandardsIngestorServiceImpl implements StandardsIngestorService {

    private final VectorStore vectorStore;

    public StandardsIngestorServiceImpl(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void ingestFromClasspath() throws Exception {
        var resolver = new PathMatchingResourcePatternResolver();
        Resource[] standardFiles = resolver.getResources("classpath:standards/*.md");
        if (standardFiles == null || standardFiles.length == 0) return;

        List<Document> docs = new ArrayList<>();
        for (Resource r : standardFiles) {
            String content = new String(r.getContentAsByteArray(), StandardCharsets.UTF_8);
            String[] chunks = content.split("\n##\\s"); // chunk by second-level headings
            for (String chunk : chunks) {
                String c = chunk.strip();
                if (c.isEmpty()) continue;
                docs.add(new Document(c, Map.of(
                        "source", r.getFilename(),
                        "category", inferCategory(r.getFilename())
                )));
            }
        }
        if (!docs.isEmpty()) vectorStore.add(docs);
    }

    private String inferCategory(String fn) {
        if (fn == null) return "general";
        String f = fn.toLowerCase();
        if (f.contains("security")) return "security";
        if (f.contains("performance")) return "performance";
        if (f.contains("testing")) return "testing";
        if (f.contains("concurrency")) return "concurrency";
        return "general";
    }
}
