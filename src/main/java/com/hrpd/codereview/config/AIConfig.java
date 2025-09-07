package com.hrpd.codereview.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AIConfig {

    /**
     * Builds the ChatClient using the injected builder.
     *
     * @param builder Spring AI ChatClient builder.
     * @return configured ChatClient.
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    /**
     * Virtual-thread executor, perfect for parallel fan-out in demos.
     *
     * @return ExecutorService using one virtual thread per task.
     */
    @Bean
    public ExecutorService parallelExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
