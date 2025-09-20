package com.hrpd.codereview.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Configuration class for AI-related beans and services.
 * 
 * <p>This configuration class sets up the core AI infrastructure components
 * required for the code review system, including the ChatClient for AI model
 * communication and the virtual thread executor for parallel processing.</p>
 * 
 * <p>The configuration emphasizes performance and scalability by using virtual
 * threads for concurrent operations, which provides excellent throughput for
 * I/O-bound AI model calls while maintaining low resource overhead.</p>
 */
@Configuration
public class AIConfig {

    /**
     * Creates and configures the ChatClient for AI model communication.
     * 
     * <p>The ChatClient is the primary interface for communicating with AI models
     * in the Spring AI framework. It handles the underlying HTTP communication,
     * request/response processing, and provides a fluent API for constructing
     * prompts and processing responses.</p>
     * 
     * <p>This bean is used by all reviewer implementations to send code analysis
     * requests to the AI model and receive structured findings in response.</p>
     *
     * @param builder Spring AI ChatClient builder, auto-configured by Spring AI
     * @return configured ChatClient ready for AI model communication
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    /**
     * Creates a virtual thread executor service for high-performance parallel processing.
     * 
     * <p>This executor uses Java's virtual threads (Project Loom) to provide excellent
     * performance for I/O-bound operations like AI model calls. Virtual threads are
     * lightweight and can handle thousands of concurrent operations with minimal
     * resource overhead, making them ideal for parallel code review workflows.</p>
     * 
     * <p>Benefits of virtual threads for this use case:
     * <ul>
     *   <li>High concurrency with minimal memory footprint</li>
     *   <li>Excellent performance for I/O-bound AI model calls</li>
     *   <li>Simplified error handling and debugging</li>
     *   <li>No need for complex thread pool tuning</li>
     * </ul></p>
     *
     * @return ExecutorService using virtual threads for optimal parallel performance
     */
    @Bean
    public ExecutorService parallelExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
