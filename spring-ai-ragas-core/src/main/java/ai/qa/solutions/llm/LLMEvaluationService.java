package ai.qa.solutions.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.CompletableFuture;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Enhanced LLMEvaluationService with structured output support
 */
public class LLMEvaluationService {
    private final ChatClient chatClient;

    public LLMEvaluationService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Evaluate with structured output - returns parsed DTO object
     */
    public <T> T evaluateWithStructuredOutput(String prompt, Class<T> responseType) {
        return chatClient.prompt(prompt).call().entity(responseType);
    }

    /**
     * Async evaluation with structured output
     */
    public <T> CompletableFuture<T> evaluateWithStructuredOutputAsync(String prompt, Class<T> responseType) {
        return CompletableFuture.supplyAsync(() -> evaluateWithStructuredOutput(prompt, responseType));
    }
}