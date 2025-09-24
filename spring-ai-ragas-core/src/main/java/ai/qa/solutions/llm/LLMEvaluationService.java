package ai.qa.solutions.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.CompletableFuture;
import org.springframework.ai.chat.client.ChatClient;

public class LLMEvaluationService {
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public LLMEvaluationService(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public String evaluate(String prompt) {
        return chatClient.prompt(prompt).call().content();
    }

    public <T> T evaluateWithStructuredOutput(String prompt, Class<T> responseType) {
        return chatClient.prompt(prompt).call().entity(responseType);
    }

    public CompletableFuture<String> evaluateAsync(String prompt) {
        return CompletableFuture.supplyAsync(() -> evaluate(prompt));
    }

    /**
     * Parse JSON response from LLM to extract score
     */
    public Double parseJsonScore(String jsonResponse) {
        try {
            JsonNode node = objectMapper.readTree(jsonResponse);
            if (node.has("score")) {
                return node.get("score").asDouble();
            } else if (node.has("verdict")) {
                return node.get("verdict").asBoolean() ? 1.0 : 0.0;
            }
            throw new RuntimeException("No score or verdict found in response: " + jsonResponse);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse LLM response: " + jsonResponse, e);
        }
    }
}
