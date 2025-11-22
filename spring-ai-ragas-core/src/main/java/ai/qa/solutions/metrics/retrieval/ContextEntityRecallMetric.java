package ai.qa.solutions.metrics.retrieval;

import ai.qa.solutions.metric.Metric;
import ai.qa.solutions.sample.Sample;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;

/**
 * Context Entity Recall Metric - LLM-based evaluation measuring the recall of entities
 * present in both reference and retrieved contexts relative to entities in reference alone.
 * <p>
 * This metric is particularly useful in fact-based use cases like tourism help desk,
 * historical QA, etc., where entity coverage is crucial for evaluating retrieval mechanisms.
 */
@Slf4j
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ContextEntityRecallMetric implements Metric<ContextEntityRecallMetric.ContextEntityRecallConfig> {
    public static final String DEFAULT_ENTITY_EXTRACTION_PROMPT =
            """
                    Given a text, extract unique entities without repetition. Ensure you consider different forms or mentions of the same entity as a single entity.

                    Text: {text}

                    Instructions:
                    1. Extract all named entities including:
                       - Person names (e.g., "Albert Einstein", "Napoleon")
                       - Place names (e.g., "Paris", "Eiffel Tower", "France")
                       - Organizations (e.g., "UNESCO", "European Union")
                       - Dates and times (e.g., "1889", "July 16, 1969", "7th century BC")
                       - Events (e.g., "World War II", "Apollo 11 mission")
                       - Products/objects (e.g., "iPhone", "Great Wall of China")
                       - Numbers and measurements (e.g., "21,196 kilometers", "50,000 spectators")
                    2. Avoid duplicates - treat different forms of the same entity as one
                    3. Focus on factual, concrete entities rather than abstract concepts
                    4. Include proper nouns, specific dates, numbers, and measurable quantities
                    5. Exclude common words, pronouns, and generic terms

                    Examples:
                    - "The Eiffel Tower, located in Paris, France, was completed in 1889 for the World's Fair."
                      Entities: ["Eiffel Tower", "Paris", "France", "1889", "World's Fair"]

                    - "Neil Armstrong and Buzz Aldrin landed on the Moon during Apollo 11 on July 16, 1969."
                      Entities: ["Neil Armstrong", "Buzz Aldrin", "Moon", "Apollo 11", "July 16, 1969"]

                    Respond with a JSON object containing:
                    - entities: A list of unique entities extracted from the text
                    """;

    @NonNull
    private final ChatClient chatClient;

    @NonNull
    @Builder.Default
    private final String entityExtractionPrompt = DEFAULT_ENTITY_EXTRACTION_PROMPT;

    public Double singleTurnScore(final ContextEntityRecallConfig config, final Sample sample) {
        // Validate required inputs
        String reference = sample.getReference();
        if (reference == null || reference.trim().isEmpty()) {
            log.warn("No reference provided for Context Entity Recall evaluation");
            return 0.0;
        }

        List<String> retrievedContexts = sample.getRetrievedContexts();
        if (retrievedContexts == null || retrievedContexts.isEmpty()) {
            log.warn("No retrieved contexts provided for Context Entity Recall evaluation");
            return 0.0;
        }

        log.debug("Computing LLM-based context entity recall evaluation");

        // Step 1: Extract entities from reference
        Set<String> referenceEntities = extractEntities(reference);

        if (referenceEntities.isEmpty()) {
            log.warn("No entities extracted from reference");
            return 0.0;
        }

        log.debug("Extracted {} entities from reference", referenceEntities.size());

        // Step 2: Extract entities from retrieved contexts
        String combinedContexts = String.join("\n\n", retrievedContexts);
        Set<String> contextEntities = extractEntities(combinedContexts);

        log.debug("Extracted {} entities from retrieved contexts", contextEntities.size());

        // Step 3: Calculate entity recall
        return calculateEntityRecall(referenceEntities, contextEntities);
    }

    public CompletableFuture<Double> singleTurnScoreAsync(ContextEntityRecallConfig config, Sample sample) {
        return CompletableFuture.supplyAsync(() -> singleTurnScore(config, sample));
    }

    private Set<String> extractEntities(String text) {
        final Map<String, Object> variables = Map.of("text", text);

        EntitiesResponse entitiesResponse = chatClient
                .prompt(PromptTemplate.builder()
                        .template(entityExtractionPrompt)
                        .variables(variables)
                        .build()
                        .create())
                .call()
                .entity(EntitiesResponse.class);

        if (entitiesResponse.entities() == null) {
            return new HashSet<>();
        }

        // Convert to lowercase for case-insensitive comparison and create a set
        Set<String> entities = new HashSet<>();
        for (String entity : entitiesResponse.entities()) {
            if (entity != null && !entity.trim().isEmpty()) {
                entities.add(entity.trim().toLowerCase());
            }
        }

        return entities;
    }

    private Double calculateEntityRecall(Set<String> referenceEntities, Set<String> contextEntities) {
        if (referenceEntities.isEmpty()) {
            return 0.0;
        }

        // Find intersection of entities (entities present in both reference and context)
        Set<String> commonEntities = new HashSet<>(referenceEntities);
        commonEntities.retainAll(contextEntities);

        log.debug("Reference entities: {}", referenceEntities);
        log.debug("Context entities: {}", contextEntities);
        log.debug("Common entities: {}", commonEntities);

        // Entity recall = |intersection| / |reference entities|
        double recall = (double) commonEntities.size() / referenceEntities.size();

        log.debug("Entity recall: {} / {} = {}", commonEntities.size(), referenceEntities.size(), recall);

        return recall;
    }

    /**
     * Response DTO for entity extraction
     */
    public record EntitiesResponse(
            @JsonPropertyDescription("List of unique entities extracted from the text") List<String> entities) {}

    @Data
    @Builder
    public static class ContextEntityRecallConfig implements MetricConfiguration {}
}
