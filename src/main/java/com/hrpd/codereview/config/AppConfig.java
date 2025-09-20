package com.hrpd.codereview.config;

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

/**
 * Main application configuration class that wires together all the components
 * of the AI-powered code review system.
 * 
 * <p>This configuration class defines all the beans required for the code review
 * system to function, including services, reviewers, and the parallel workflow
 * orchestrator. It also handles startup initialization tasks like ingesting
 * coding standards into the vector database.</p>
 * 
 * <p>The configuration is organized into logical sections:
 * <ul>
 *   <li>Core services (diff parsing, GitHub integration, standards management)</li>
 *   <li>AI-powered reviewers (security, performance, clean code)</li>
 *   <li>Workflow orchestration and aggregation</li>
 *   <li>Startup initialization tasks</li>
 * </ul></p>
 */
@Configuration
public class AppConfig {

    /**
     * Creates the diff service for parsing unified diff patches.
     * 
     * @return configured DiffService instance
     */
    @Bean
    public DiffService diffService() {
        return new DiffServiceImpl();
    }

    /**
     * Creates the GitHub client service for fetching pull request patches.
     * 
     * <p>This service is configured with optional GitHub API settings from
     * application properties. It can be configured to use a custom GitHub
     * Enterprise instance or personal access token for authentication.</p>
     * 
     * @param env Spring environment for reading configuration properties
     * @return configured GithubClientService instance
     */
    @Bean
    public GithubClientService githubClient(org.springframework.core.env.Environment env) {
        // Read optional base url + token from properties
        String baseUrl = env.getProperty("github.base-url", "https://api.github.com");
        String token = env.getProperty("github.token", "");
        return new HttpGithubClientService(baseUrl, token);
    }

    /**
     * Creates the standards retriever service for semantic search of coding standards.
     * 
     * @param vectorStore the vector database for storing and retrieving standards
     * @return configured StandardsRetrieverService instance
     */
    @Bean
    public StandardsRetrieverService standardsRetriever(VectorStore vectorStore) {
        return new StandardsRetrieverServiceImpl(vectorStore);
    }

    /**
     * Creates the standards ingestor service for processing and storing coding standards.
     * 
     * @param vectorStore the vector database for storing processed standards
     * @param jdbcTemplate JDBC template for database operations
     * @return configured StandardsIngestorService instance
     */
    @Bean
    public StandardsIngestorService standardsIngestor(VectorStore vectorStore, org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        return new StandardsIngestorServiceImpl(vectorStore, jdbcTemplate);
    }

    /**
     * Command line runner that ingests coding standards at application startup.
     * 
     * <p>This bean ensures that all coding standards from the classpath are
     * processed and stored in the vector database when the application starts.
     * This provides the knowledge base that reviewers use to ground their
     * AI analysis in organizational standards.</p>
     * 
     * @param ingestor the standards ingestor service
     * @return CommandLineRunner that performs standards ingestion
     */
    @Bean
    public CommandLineRunner ingestStandardsAtStartup(StandardsIngestorService ingestor) {
        return _ -> ingestor.ingestFromClasspath();
    }

    /**
     * Creates the aggregator service for merging and deduplicating review results.
     * 
     * @return configured AggregatorService instance
     */
    @Bean
    public AggregatorService aggregatorService() {
        return new AggregatorServiceImpl();
    }

    // --- AI-Powered Reviewers (implement the Reviewer interface) ---

    /**
     * Creates the security reviewer for identifying security vulnerabilities.
     * 
     * @param chat ChatClient for AI model communication
     * @param retriever StandardsRetrieverService for accessing security standards
     * @return configured SecurityReviewer instance
     */
    @Bean
    public Reviewer securityReviewer(ChatClient chat, StandardsRetrieverService retriever) {
        return new SecurityReviewer(chat, retriever);
    }

    /**
     * Creates the performance reviewer for identifying performance bottlenecks.
     * 
     * @param chat ChatClient for AI model communication
     * @param retriever StandardsRetrieverService for accessing performance standards
     * @return configured PerformanceReviewer instance
     */
    @Bean
    public Reviewer performanceReviewer(ChatClient chat, StandardsRetrieverService retriever) {
        return new PerformanceReviewer(chat, retriever);
    }

    /**
     * Creates the clean code reviewer for identifying code quality issues.
     * 
     * @param chat ChatClient for AI model communication
     * @param retriever StandardsRetrieverService for accessing clean code standards
     * @return configured CleanCodeReviewer instance
     */
    @Bean
    public Reviewer cleanCodeReviewer(ChatClient chat, StandardsRetrieverService retriever) {
        return new CleanCodeReviewer(chat, retriever);
    }

    // --- Parallel Workflow Orchestration ---

    /**
     * Creates the parallel workflow service that orchestrates the complete review process.
     * 
     * <p>This service coordinates all configured reviewers, manages parallel execution,
     * and aggregates results. It receives the complete list of reviewers through
     * Spring's dependency injection, making it easy to add or remove reviewers
     * by simply adding or removing their bean definitions.</p>
     * 
     * @param reviewers list of all configured reviewer beans
     * @param aggregator service for merging and deduplicating results
     * @param pool executor service for parallel execution
     * @return configured ParallelWorkflowService instance
     */
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
