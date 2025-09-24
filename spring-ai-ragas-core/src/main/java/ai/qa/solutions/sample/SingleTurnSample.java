package ai.qa.solutions.sample;

import java.util.List;
import java.util.Map;

/**
 * Represents a single-turn interaction sample
 */
public class SingleTurnSample implements EvaluationSample {
    private final String userInput;
    private final List<String> retrievedContexts;
    private final String response;
    private final String reference;
    private final Map<String, String> rubric;
    private final Map<String, Object> metadata;

    public SingleTurnSample(
            String userInput,
            List<String> retrievedContexts,
            String response,
            String reference,
            Map<String, String> rubric,
            Map<String, Object> metadata) {
        this.userInput = userInput;
        this.retrievedContexts = retrievedContexts;
        this.response = response;
        this.reference = reference;
        this.rubric = rubric;
        this.metadata = metadata;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String userInput;
        private List<String> retrievedContexts;
        private String response;
        private String reference;
        private Map<String, String> rubric;
        private Map<String, Object> metadata;

        public Builder userInput(String userInput) {
            this.userInput = userInput;
            return this;
        }

        public Builder retrievedContexts(List<String> retrievedContexts) {
            this.retrievedContexts = retrievedContexts;
            return this;
        }

        public Builder response(String response) {
            this.response = response;
            return this;
        }

        public Builder reference(String reference) {
            this.reference = reference;
            return this;
        }

        public Builder rubric(Map<String, String> rubric) {
            this.rubric = rubric;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public SingleTurnSample build() {
            return new SingleTurnSample(userInput, retrievedContexts, response, reference, rubric, metadata);
        }
    }

    // Getters
    @Override
    public String getUserInput() {
        return userInput;
    }

    @Override
    public String getResponse() {
        return response;
    }

    public List<String> getRetrievedContexts() {
        return retrievedContexts;
    }

    public String getReference() {
        return reference;
    }

    public Map<String, String> getRubric() {
        return rubric;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public Map<String, Object> getFeatures() {
        return Map.of(
                "user_input", userInput,
                "retrieved_contexts", retrievedContexts,
                "response", response,
                "reference", reference,
                "rubric", rubric);
    }

    @Override
    public Map<String, Object> toMap() {
        return getFeatures();
    }
}
