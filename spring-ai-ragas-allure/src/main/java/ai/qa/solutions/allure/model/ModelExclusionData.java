package ai.qa.solutions.allure.model;

import ai.qa.solutions.execution.listener.dto.ModelExclusionEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import lombok.Builder;
import lombok.Value;

/**
 * Data container for a model exclusion event.
 * <p>
 * Records when a model was excluded from evaluation due to a failure.
 */
@Value
@Builder
public class ModelExclusionData {

    /**
     * Model identifier that was excluded.
     */
    String modelId;

    /**
     * Name of the step where failure occurred.
     */
    String failedStepName;

    /**
     * Index of the step where failure occurred.
     */
    int failedStepIndex;

    /**
     * Human-readable error message.
     */
    String errorMessage;

    /**
     * Full stack trace of the error.
     */
    String stackTrace;

    /**
     * Creates ModelExclusionData from a ModelExclusionEvent.
     *
     * @param event the exclusion event to convert
     * @return converted ModelExclusionData
     */
    public static ModelExclusionData from(final ModelExclusionEvent event) {
        final Throwable cause = event.getCause();
        String message = "Unknown error";
        String trace = "";

        if (cause != null) {
            message = extractErrorMessage(cause);
            final StringWriter sw = new StringWriter();
            cause.printStackTrace(new PrintWriter(sw));
            trace = sw.toString();
        }

        return ModelExclusionData.builder()
                .modelId(event.getModelId())
                .failedStepName(event.getFailedStepName())
                .failedStepIndex(event.getFailedStepIndex())
                .errorMessage(message)
                .stackTrace(trace)
                .build();
    }

    private static String extractErrorMessage(final Throwable cause) {
        if (cause.getMessage() != null && !cause.getMessage().isBlank()) {
            return cause.getMessage();
        }
        if (cause.getCause() != null && cause.getCause().getMessage() != null) {
            return cause.getCause().getMessage();
        }
        return cause.getClass().getSimpleName();
    }
}
