package com.hrpd.codereview.config;

import com.hrpd.codereview.controller.ReviewController;
import com.hrpd.codereview.reviewer.CleanCodeReviewer;
import com.hrpd.codereview.reviewer.PerformanceReviewer;
import com.hrpd.codereview.reviewer.Reviewer;
import com.hrpd.codereview.reviewer.SecurityReviewer;
import com.hrpd.codereview.service.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.ExecutorService;

@Configuration
public class AppConfig {

    @Bean
    public DiffService diffService() {
        return new DiffServiceImpl();
    }

    @Bean
    public GithubClientService githubClient(org.springframework.core.env.Environment env) {
        // Read optional base url + token from properties
        String baseUrl = env.getProperty("github.base-url", "https://api.github.com");
        String token = env.getProperty("github.token", "");
        return new HttpGithubClientService(baseUrl, token);
    }

    @Bean
    public StandardsRetrieverService standardsRetriever(VectorStore vectorStore) {
        return new StandardsRetrieverServiceImpl(vectorStore);
    }

    @Bean
    public StandardsIngestorService standardsIngestor(VectorStore vectorStore) {
        return new StandardsIngestorServiceImpl(vectorStore);
    }

    /** Runs standards ingestion at startup. */
    @Bean
    public CommandLineRunner ingestStandardsAtStartup(StandardsIngestorService ingestor) {
        return args -> ingestor.ingestFromClasspath();
    }

    @Bean
    public AggregatorService aggregatorService() {
        return new AggregatorServiceImpl();
    }

    // --- Reviewers (implement the Reviewer interface) ---

    @Bean
    public Reviewer securityReviewer(ChatClient chat, StandardsRetrieverService retriever) {
        return new SecurityReviewer(chat, retriever);
    }

    @Bean
    public Reviewer performanceReviewer(ChatClient chat, StandardsRetrieverService retriever) {
        return new PerformanceReviewer(chat, retriever);
    }

    @Bean
    public Reviewer cleanCodeReviewer(ChatClient chat, StandardsRetrieverService retriever) {
        return new CleanCodeReviewer(chat, retriever);
    }

    // --- Parallel workflow with explicit list of reviewers ---

    @Bean
    public ParallelWorkflowService parallelWorkflowService(
            List<Reviewer> reviewers,
            AggregatorService aggregator,
            ExecutorService pool) {
        return new ParallelWorkflowServiceImpl(
                reviewers,
                aggregator,
                pool);
    }

}
