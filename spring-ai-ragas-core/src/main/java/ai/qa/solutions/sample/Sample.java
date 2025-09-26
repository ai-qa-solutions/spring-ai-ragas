package ai.qa.solutions.sample;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single-turn interaction sample
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sample {
    private String userInput;
    private List<String> retrievedContexts;
    private String response;
    private String reference;
    private Map<String, String> rubric;
    private Map<String, Object> metadata;
}
