package com.hrpd.codereview.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrpd.codereview.model.Finding;
import com.hrpd.codereview.model.ReviewerType;
import com.hrpd.codereview.model.Severity;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for parsing JSON responses from AI models into structured Finding objects.
 * 
 * <p>This class handles the complex task of extracting valid JSON from AI responses
 * that may contain explanatory text, markdown formatting, or other non-JSON content.
 * It provides robust parsing with fallback mechanisms for common AI response formats.</p>
 * 
 * <p>The parser is designed to handle various response formats including:
 * <ul>
 *   <li>Pure JSON responses</li>
 *   <li>JSON wrapped in markdown code blocks (```json ... ```)</li>
 *   <li>Mixed content with JSON embedded in explanatory text</li>
 *   <li>Responses with multiple code blocks</li>
 * </ul></p>
 */
public class JsonUtils {
    
    /**
     * Jackson ObjectMapper instance for JSON parsing.
     * Configured with default settings suitable for AI response parsing.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Extracts valid JSON content from AI responses that may contain explanatory text.
     * 
     * <p>This method handles various response formats commonly produced by AI models:
     * <ul>
     *   <li>JSON wrapped in markdown code blocks (```json ... ```)</li>
     *   <li>JSON in generic code blocks (``` ... ```)</li>
     *   <li>JSON embedded within explanatory text</li>
     *   <li>Pure JSON responses</li>
     * </ul></p>
     * 
     * <p>The extraction process uses a state machine to properly handle nested
     * JSON structures, string escaping, and bracket matching to ensure only
     * complete, valid JSON is extracted.</p>
     * 
     * @param response the raw AI response that may contain JSON
     * @return the extracted JSON string, or the original response if no JSON is found
     */
    private static String extractJsonFromResponse(String response) {
        String cleaned = response.trim();
        
        // First, try to find JSON within markdown code blocks (```json ... ```)
        if (cleaned.contains("```json")) {
            int start = cleaned.indexOf("```json") + 7;
            int end = cleaned.indexOf("```", start);
            if (end > start) {
                return cleaned.substring(start, end).trim();
            }
        }
        
        // Try generic code blocks (``` ... ```) that might contain JSON
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
        
        // Try to find JSON object/array embedded in the response text
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
            // Find the matching closing brace/bracket using a state machine
            // This properly handles nested JSON structures and string escaping
            int braceCount = 0;
            boolean inString = false;
            boolean escaped = false;
            
            for (int i = jsonStart; i < cleaned.length(); i++) {
                char c = cleaned.charAt(i);
                
                // Handle escaped characters in strings
                if (escaped) {
                    escaped = false;
                    continue;
                }
                
                // Detect escape character
                if (c == '\\') {
                    escaped = true;
                    continue;
                }
                
                // Toggle string state on unescaped quotes
                if (c == '"' && !escaped) {
                    inString = !inString;
                    continue;
                }
                
                // Only count braces/brackets when not inside a string
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
        // This allows the parser to attempt parsing the entire response
        return cleaned;
    }

    /**
     * Parses a JSON response from an AI model into a list of Finding objects.
     * 
     * <p>This method handles the complete parsing pipeline:
     * <ol>
     *   <li>Extracts valid JSON from potentially mixed-content AI responses</li>
     *   <li>Parses the JSON structure to find the "findings" array</li>
     *   <li>Converts each finding object to a Finding record</li>
     *   <li>Handles missing or invalid data with sensible defaults</li>
     *   <li>Provides filePath fallback when AI responses are incomplete</li>
     * </ol></p>
     * 
     * <p>The method is robust against various AI response formats and will
     * return an empty list rather than throwing exceptions for malformed JSON.</p>
     * 
     * @param json the JSON response from the AI model
     * @param type the type of reviewer that generated this response
     * @param filePath fallback file path to use when AI response doesn't include one
     * @return a list of Finding objects, or an empty list if parsing fails
     */
    public static List<Finding> parseFindings(String json, ReviewerType type, String filePath) {
        try {
            // Clean the JSON string by removing markdown code blocks and extracting JSON from mixed content
            String cleanedJson = extractJsonFromResponse(json);
            
            // Parse the JSON and extract the findings array
            JsonNode root = MAPPER.readTree(cleanedJson);
            JsonNode arr = root.path("findings");
            if (!arr.isArray()) {
                System.err.println("No findings array found in JSON: " + cleanedJson);
                return List.of();
            }

            // Convert each JSON finding object to a Finding record
            List<Finding> out = new ArrayList<>();
            for (JsonNode n : arr) {
                // Parse severity with fallback to INFO for invalid values
                String sevText = n.path("severity").asText("INFO").toUpperCase();
                Severity sev;
                try {
                    sev = Severity.valueOf(sevText);
                } catch (Exception e) {
                    sev = Severity.INFO;
                }

                // Use the provided filePath if the AI returned an empty one
                // This handles cases where AI responses don't include file paths
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
     * Backward-compatible method for parsing findings without a filePath fallback.
     * 
     * <p>This method is provided for compatibility with existing code that doesn't
     * need filePath fallback functionality. It delegates to the main parsing method
     * with a null filePath parameter.</p>
     * 
     * @param json the JSON response from the AI model
     * @param type the type of reviewer that generated this response
     * @return a list of Finding objects, or an empty list if parsing fails
     * 
     * @see #parseFindings(String, ReviewerType, String)
     */
    public static List<Finding> parseFindings(String json, ReviewerType type) {
        return parseFindings(json, type, null);
    }
}
