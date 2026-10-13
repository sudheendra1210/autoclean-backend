package com.datacleaning.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.*;

@Service
public class OllamaService {
  private final RestClient client; private final ObjectMapper mapper;
  @Value("${app.ollama.model:llama3.2}") private String model;
  public OllamaService(@Value("${app.ollama.base-url:http://localhost:11434}") String baseUrl, ObjectMapper mapper) { this.client=RestClient.builder().baseUrl(baseUrl).build(); this.mapper=mapper; }
  public String generatePlan(Map<String,String> schema, List<Map<String,Object>> sample, String instruction) {
    String prompt = "You are a data-cleaning planner. Return ONLY valid JSON, no markdown, with shape {\"operations\":[{\"type\":\"remove_duplicates\"}]}. Supported operation types: remove_duplicates, drop_missing_rows, fill_missing (requires value), trim_whitespace, lowercase (requires column), uppercase (requires column), drop_column (requires column), rename_column (requires column and new_name), replace_value (requires column, from, to). Use only these operations. Never return code, SQL, shell commands, or other operation types. If the instruction is unclear, return {\"operations\":[]}. Schema: " + schema + " Sample: " + sample + " User instruction: " + instruction;
    try {
      Map<String,Object> body=Map.of("model",model,"prompt",prompt,"stream",false,"format","json","options",Map.of("temperature",0.1));
      Map<?,?> response=client.post().uri("/api/generate").contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(Map.class);
      String output=String.valueOf(response==null?"":response.get("response"));
      JsonNode root=mapper.readTree(output);
      if (!root.isObject() || !root.path("operations").isArray()) throw new IllegalArgumentException("Ollama returned an invalid plan.");
      for(JsonNode op:root.path("operations")) { String type=op.path("type").asText(); if(!Set.of("remove_duplicates","drop_missing_rows","fill_missing","trim_whitespace","lowercase","uppercase","drop_column","rename_column","replace_value").contains(type)) throw new IllegalArgumentException("Unsupported operation from AI: "+type); }
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    } catch(Exception e) { throw new IllegalStateException("Could not generate a cleaning plan. Check that Ollama is running and model '"+model+"' is installed. Details: "+e.getMessage(),e); }
  }
}
