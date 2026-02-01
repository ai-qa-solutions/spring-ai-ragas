package ai.qa.solutions.allure.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class providing a shared {@link ObjectMapper} for Allure module.
 * <p>
 * This class centralizes JSON serialization/deserialization configuration
 * to ensure consistency across all Allure components.
 * <p>
 * <strong>Configuration:</strong>
 * <ul>
 *   <li>{@link JavaTimeModule} - Support for Java 8 date/time types</li>
 *   <li>{@link SerializationFeature#INDENT_OUTPUT} - Pretty-print JSON</li>
 *   <li>{@link SerializationFeature#WRITE_DATES_AS_TIMESTAMPS} disabled - ISO format for dates</li>
 * </ul>
 */
@Slf4j
@UtilityClass
public class AllureJsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

    /**
     * Returns the shared ObjectMapper instance.
     * <p>
     * Use this when you need direct access to the ObjectMapper,
     * for example when working with streaming APIs.
     *
     * @return the configured ObjectMapper
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    /**
     * Parses a JSON string into a JsonNode tree.
     *
     * @param json the JSON string to parse
     * @return the parsed JsonNode
     * @throws JsonProcessingException if parsing fails
     */
    public static JsonNode readTree(final String json) throws JsonProcessingException {
        return OBJECT_MAPPER.readTree(json);
    }

    /**
     * Converts an object to a JsonNode tree.
     *
     * @param value the object to convert
     * @return the JsonNode representation
     */
    public static JsonNode valueToTree(final Object value) {
        return OBJECT_MAPPER.valueToTree(value);
    }

    /**
     * Serializes an object to a JSON string.
     *
     * @param value the object to serialize
     * @return the JSON string
     * @throws JsonProcessingException if serialization fails
     */
    public static String writeValueAsString(final Object value) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(value);
    }

    /**
     * Serializes an object to a JSON string, returning fallback on error.
     *
     * @param value the object to serialize
     * @param fallback the fallback value if serialization fails
     * @return the JSON string or fallback
     */
    public static String writeValueAsStringSafe(final Object value, final String fallback) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (final JsonProcessingException e) {
            log.debug("Failed to serialize object to JSON: {}", e.getMessage());
            return fallback;
        }
    }

    private static ObjectMapper createObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
