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

    public static List<Finding> parseFindings(String json, ReviewerType type) {
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode arr = root.path("findings");
            if (!arr.isArray()) return List.of();

            List<Finding> out = new ArrayList<>();
            for (JsonNode n : arr) {
                String sevText = n.path("severity").asText("INFO").toUpperCase();
                Severity sev;
                try {
                    sev = Severity.valueOf(sevText);
                } catch (Exception e) {
                    sev = Severity.INFO;
                }

                out.add(new Finding(
                        n.path("filePath").asText(""),
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
            return List.of();
        }
    }
}
