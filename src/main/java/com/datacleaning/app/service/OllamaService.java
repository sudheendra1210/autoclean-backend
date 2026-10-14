package com.datacleaning.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
public class OllamaService {

    private final RestClient client;
    private final ObjectMapper mapper;
    private final String apiKey;

    @Value("${app.groq.model:openai/gpt-oss-20b}")
    private String model;

    public OllamaService(
            @Value("${app.groq.base-url:https://api.groq.com/openai/v1}") String baseUrl,
            @Value("${app.groq.api-key:}") String apiKey,
            ObjectMapper mapper) {

        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .build();

        this.apiKey = apiKey;
        this.mapper = mapper;
    }

    public String generatePlan(
            Map<String, String> schema,
            List<Map<String, Object>> sample,
            String instruction) {

        String prompt =
                "You are a data-cleaning planner. Return ONLY valid JSON, "
                + "with shape {\"operations\":[{\"type\":\"remove_duplicates\"}]}. "
                + "Supported operation types: remove_duplicates, drop_missing_rows, "
                + "fill_missing (requires value), trim_whitespace, lowercase "
                + "(requires column), uppercase (requires column), drop_column "
                + "(requires column), rename_column (requires column and new_name), "
                + "replace_value (requires column, from, to). "
                + "Use only these operations. Never return code, SQL, shell commands, "
                + "or other operation types. If the instruction is unclear, "
                + "return {\"operations\":[]}. "
                + "Schema: " + schema
                + " Sample: " + sample
                + " User instruction: " + instruction;

        try {
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException(
                        "GROQ_API_KEY is missing in the server environment.");
            }

            Map<String, Object> message = Map.of(
                    "role", "user",
                    "content", prompt
            );

            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(message),
                    "temperature", 0.1,
                    "response_format", Map.of("type", "json_object")
            );

            Map<?, ?> response = client.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new IllegalStateException("Groq returned an empty response.");
            }

            Object choicesObject = response.get("choices");

            if (!(choicesObject instanceof List<?> choices)
                    || choices.isEmpty()
                    || !(choices.get(0) instanceof Map<?, ?> choice)
                    || !(choice.get("message") instanceof Map<?, ?> responseMessage)) {
                throw new IllegalStateException(
                        "Groq returned an unexpected response format.");
            }

            String output = String.valueOf(responseMessage.get("content"));
            JsonNode root = mapper.readTree(output);

            if (!root.isObject() || !root.path("operations").isArray()) {
                throw new IllegalArgumentException(
                        "AI returned an invalid cleaning plan.");
            }

            Set<String> allowedOperations = Set.of(
                    "remove_duplicates",
                    "drop_missing_rows",
                    "fill_missing",
                    "trim_whitespace",
                    "lowercase",
                    "uppercase",
                    "drop_column",
                    "rename_column",
                    "replace_value"
            );

            for (JsonNode op : root.path("operations")) {
                String type = op.path("type").asText();

                if (!allowedOperations.contains(type)) {
                    throw new IllegalArgumentException(
                            "Unsupported operation from AI: " + type);
                }
            }

            return mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(root);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not generate a cleaning plan using Groq. Details: "
                            + e.getMessage(),
                    e);
        }
    }
}

