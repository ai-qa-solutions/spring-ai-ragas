package ai.qa.solutions.allure.model;

import ai.qa.solutions.execution.ModelResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import lombok.Builder;
import lombok.Value;

/**
 * Data container for a single model's execution result within a step.
 * <p>
 * Contains either successful result data (as pretty-printed JSON)
 * or error information (message and full stack trace).
 */
@Value
@Builder
public class ModelExecutionData {

    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

    /**
     * Model identifier (e.g., "anthropic-claude-3-5-sonnet").
     */
    String modelId;

    /**
     * Whether this execution was successful.
     */
    boolean success;

    /**
     * Execution duration.
     */
    Duration duration;

    /**
     * The request/prompt sent to the model.
     */
    String request;

    /**
     * The raw result object (for successful executions).
     */
    Object result;

    /**
     * Pretty-printed JSON representation of the result (for successful executions).
     */
    String resultJson;

    /**
     * Error message (for failed executions).
     */
    String errorMessage;

    /**
     * Full stack trace (for failed executions).
     */
    String stackTrace;

    /**
     * Creates ModelExecutionData from a ModelResult.
     *
     * @param modelResult the model result to convert
     * @return converted ModelExecutionData
     */
    public static ModelExecutionData from(final ModelResult<?> modelResult) {
        if (modelResult.isSuccess()) {
            return fromSuccess(modelResult);
        }
        return fromFailure(modelResult);
    }

    private static ModelExecutionData fromSuccess(final ModelResult<?> modelResult) {
        final Object resultObj = modelResult.result();
        String json;
        try {
            json = OBJECT_MAPPER.writeValueAsString(resultObj);
        } catch (final JsonProcessingException e) {
            json = resultObj != null ? resultObj.toString() : "null";
        }

        return ModelExecutionData.builder()
                .modelId(modelResult.modelId())
                .success(true)
                .duration(modelResult.duration())
                .request(modelResult.request())
                .result(resultObj)
                .resultJson(json)
                .build();
    }

    private static ModelExecutionData fromFailure(final ModelResult<?> modelResult) {
        final Throwable error = modelResult.error();
        String message = "Unknown error";
        String trace = "";

        if (error != null) {
            message = error.getMessage() != null
                    ? error.getMessage()
                    : error.getClass().getSimpleName();
            final StringWriter sw = new StringWriter();
            error.printStackTrace(new PrintWriter(sw));
            trace = sw.toString();
        }

        return ModelExecutionData.builder()
                .modelId(modelResult.modelId())
                .success(false)
                .duration(modelResult.duration())
                .request(modelResult.request())
                .errorMessage(message)
                .stackTrace(trace)
                .build();
    }

    /**
     * Gets duration in milliseconds.
     *
     * @return duration in ms, or 0 if duration is null
     */
    public long getDurationMs() {
        return duration != null ? duration.toMillis() : 0;
    }

    private static ObjectMapper createObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
