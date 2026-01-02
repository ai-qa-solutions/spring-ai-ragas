package ai.qa.solutions.metrics;

import java.util.List;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import org.junit.jupiter.params.provider.Arguments;

@UtilityClass
public class ModelsProvider {

    public static Stream<Arguments> models() {
        return Stream.of(
                // Premium tier - best for critical evaluations
                Arguments.of("anthropic/claude-4.5-sonnet"),
                Arguments.of("anthropic/claude-4.5-opus"),
                Arguments.of("openai/gpt-5-mini"),
                Arguments.of("openai/gpt-4.1"),
                Arguments.of("google/gemini-3-pro-preview"),
                Arguments.of("google/gemini-2.5-pro"),
                // Efficient tier - optimal cost/quality ratio
                Arguments.of("google/gemini-2.5-flash"),
                Arguments.of("google/gemini-2.0-flash-001"),
                Arguments.of("anthropic/claude-4.5-haiku"),
                Arguments.of("openai/gpt-4o-mini"),
                Arguments.of("deepseek/deepseek-v3.2"),
                Arguments.of("openai/gpt-oss-120b"),
                // Open-source leaders
                Arguments.of("qwen/qwen3-coder-480b-a35b-instruct"),
                Arguments.of("meta-llama/llama-3.3-70b-instruct"),
                Arguments.of("qwen/qwen3-235b-instruct"),
                Arguments.of("deepseek/deepseek-r1"),
                // Specialized models
                Arguments.of("x-ai/grok-code-fast-1"),
                Arguments.of("minimax/minimax-m2"),
                Arguments.of("z-ai/glm-4.6"),
                Arguments.of("google/gemini-2.5-flash-lite"));
    }

    public static List<String> modelsList() {
        return models().map(x -> (String) x.get()[0]).toList();
    }
}
