package com.hrpd.codereview.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrpd.codereview.model.Finding;
import com.hrpd.codereview.model.ReviewerType;
import com.hrpd.codereview.model.Severity;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal utility to parse LLM JSON into Finding records.
 */
public class JsonUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Extract JSON from AI response that may contain explanatory text.
     * Handles various formats including markdown code blocks and mixed content.
     */
    private static String extractJsonFromResponse(String response) {
        String cleaned = response.trim();
        
        // First, try to find JSON within markdown code blocks
        if (cleaned.contains("```json")) {
            int start = cleaned.indexOf("```json") + 7;
            int end = cleaned.indexOf("```", start);
            if (end > start) {
                return cleaned.substring(start, end).trim();
            }
        }
        
        if (cleaned.contains("```")) {
            int start = cleaned.indexOf("```") + 3;
            int end = cleaned.indexOf("```", start);
            if (end > start) {
                String content = cleaned.substring(start, end).trim();
                // Check if this looks like JSON (starts with { or [)
                if (content.startsWith("{") || content.startsWith("[")) {
                    return content;
                }
            }
        }
        
        // Try to find JSON object/array in the response
        int jsonStart = -1;
        int jsonEnd = -1;
        
        // Look for the start of a JSON object or array
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if (c == '{' || c == '[') {
                jsonStart = i;
                break;
            }
        }
        
        if (jsonStart >= 0) {
            // Find the matching closing brace/bracket
            int braceCount = 0;
            boolean inString = false;
            boolean escaped = false;
            
            for (int i = jsonStart; i < cleaned.length(); i++) {
                char c = cleaned.charAt(i);
                
                if (escaped) {
                    escaped = false;
                    continue;
                }
                
                if (c == '\\') {
                    escaped = true;
                    continue;
                }
                
                if (c == '"' && !escaped) {
                    inString = !inString;
                    continue;
                }
                
                if (!inString) {
                    if (c == '{' || c == '[') {
                        braceCount++;
                    } else if (c == '}' || c == ']') {
                        braceCount--;
                        if (braceCount == 0) {
                            jsonEnd = i + 1;
                            break;
                        }
                    }
                }
            }
            
            if (jsonEnd > jsonStart) {
                return cleaned.substring(jsonStart, jsonEnd).trim();
            }
        }
        
        // If no JSON found, return the original cleaned string
        return cleaned;
    }

    /**
     * Parse findings with filePath fallback for empty AI responses.
     */
    public static List<Finding> parseFindings(String json, ReviewerType type, String filePath) {
        try {
            // Clean the JSON string by removing markdown code blocks and extracting JSON from mixed content
            String cleanedJson = extractJsonFromResponse(json);
            
            JsonNode root = MAPPER.readTree(cleanedJson);
            JsonNode arr = root.path("findings");
            if (!arr.isArray()) {
                System.err.println("No findings array found in JSON: " + cleanedJson);
                return List.of();
            }

            List<Finding> out = new ArrayList<>();
            for (JsonNode n : arr) {
                String sevText = n.path("severity").asText("INFO").toUpperCase();
                Severity sev;
                try {
                    sev = Severity.valueOf(sevText);
                } catch (Exception e) {
                    sev = Severity.INFO;
                }

                // Use the provided filePath if the AI returned an empty one
                String findingFilePath = n.path("filePath").asText("");
                if (findingFilePath.isEmpty() && filePath != null && !filePath.isEmpty()) {
                    findingFilePath = filePath;
                }
                
                out.add(new Finding(
                        findingFilePath,
                        n.path("lineStart").asInt(0),
                        n.path("lineEnd").asInt(0),
                        n.path("title").asText(""),
                        n.path("rationale").asText(""),
                        n.path("suggestion").asText(""),
                        sev,
                        type
                ));
            }
            return out;
        } catch (Exception e) {
            System.err.println("Failed to parse JSON: " + json);
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Backward-compatible method for parsing findings without filePath.
     */
    public static List<Finding> parseFindings(String json, ReviewerType type) {
        return parseFindings(json, type, null);
    }
}
