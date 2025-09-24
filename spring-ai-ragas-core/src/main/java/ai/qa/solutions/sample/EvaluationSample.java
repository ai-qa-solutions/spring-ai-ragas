package ai.qa.solutions.sample;

import java.util.Map;

/**
 * Base interface for all evaluation samples
 */
public interface EvaluationSample {
    String getUserInput();

    String getResponse();

    Map<String, Object> getFeatures();

    Map<String, Object> toMap();
}
