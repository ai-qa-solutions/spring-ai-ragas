package ai.qa.solutions.execution.listener.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("StepType Tests")
class StepTypeTest {

    @Test
    @DisplayName("Should have exactly 3 values")
    void shouldHaveExactly3Values() {
        assertThat(StepType.values()).hasSize(3);
    }

    @Test
    @DisplayName("Should contain LLM value")
    void shouldContainLlmValue() {
        assertThat(StepType.valueOf("LLM")).isEqualTo(StepType.LLM);
    }

    @Test
    @DisplayName("Should contain EMBEDDING value")
    void shouldContainEmbeddingValue() {
        assertThat(StepType.valueOf("EMBEDDING")).isEqualTo(StepType.EMBEDDING);
    }

    @Test
    @DisplayName("Should contain COMPUTE value")
    void shouldContainComputeValue() {
        assertThat(StepType.valueOf("COMPUTE")).isEqualTo(StepType.COMPUTE);
    }

    @Test
    @DisplayName("Enum values should be in expected order")
    void enumValuesShouldBeInExpectedOrder() {
        final StepType[] values = StepType.values();
        assertThat(values[0]).isEqualTo(StepType.LLM);
        assertThat(values[1]).isEqualTo(StepType.EMBEDDING);
        assertThat(values[2]).isEqualTo(StepType.COMPUTE);
    }

    @Test
    @DisplayName("name() should return correct string representation")
    void nameShouldReturnCorrectString() {
        assertThat(StepType.LLM.name()).isEqualTo("LLM");
        assertThat(StepType.EMBEDDING.name()).isEqualTo("EMBEDDING");
        assertThat(StepType.COMPUTE.name()).isEqualTo("COMPUTE");
    }
}
