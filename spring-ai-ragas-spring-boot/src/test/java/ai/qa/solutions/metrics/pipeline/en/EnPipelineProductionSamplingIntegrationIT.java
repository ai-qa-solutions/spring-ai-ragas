package ai.qa.solutions.metrics.pipeline.en;

import static org.assertj.core.api.Assertions.assertThat;

import ai.qa.solutions.metrics.agent.AgentGoalAccuracyMetric;
import ai.qa.solutions.metrics.general.AspectCriticMetric;
import ai.qa.solutions.metrics.general.RubricsScoreMetric;
import ai.qa.solutions.metrics.retrieval.ResponseRelevancyMetric;
import ai.qa.solutions.sample.Sample;
import ai.qa.solutions.sample.message.AIMessage;
import ai.qa.solutions.sample.message.HumanMessage;
import ai.qa.solutions.sample.message.ToolCall;
import ai.qa.solutions.sample.message.ToolMessage;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

/**
 * Pipeline 2: Evaluation without references (production sampling).
 * <p>
 * Use case: Sampling and analysis of live production traffic.
 * No ground truth available - uses reference-free metrics.
 */
@Slf4j
@SpringBootTest
@EnableAutoConfiguration
@DisplayName("Pipeline 2: Without References (Production)")
class EnPipelineProductionSamplingIntegrationIT {

    @Configuration
    static class TestConfig {}

    @Autowired
    private AspectCriticMetric aspectCritic;

    @Autowired
    private RubricsScoreMetric rubricsScore;

    @Autowired
    private ResponseRelevancyMetric responseRelevancy;

    @Autowired
    private AgentGoalAccuracyMetric goalAccuracy;

    @Test
    @DisplayName("Production sample: customer support conversation")
    void evaluateProductionSample() {
        log.info("=== Pipeline 2: Production Sampling (No References) ===");

        // From production telemetry - no ground truth available
        final Sample sample = Sample.builder()
                .userInput("Order 12345 not delivered, waiting 2 weeks!")
                .response("Refund processed! Funds will arrive in 3-5 days. Anything else?")
                .userInputMessages(List.of(
                        new HumanMessage("Order 12345 not delivered, waiting 2 weeks!"),
                        new AIMessage(
                                "Hello! I apologize for the delay. Checking your order now.",
                                List.of(new ToolCall("get_order_status", Map.of("order_id", "12345")))),
                        new ToolMessage("{\"status\": \"LOST\"}"),
                        new AIMessage(
                                "I see your package was lost. Processing a full refund for you.",
                                List.of(new ToolCall("process_refund", Map.of("order_id", "12345", "amount", 5500)))),
                        new ToolMessage("{\"status\": \"REFUND_INITIATED\"}"),
                        new AIMessage("Refund processed! Funds will arrive in 3-5 days. Anything else?", List.of())))
                // Only RAG contexts from traces - NO reference data
                .retrievedContexts(List.of(
                        "Order 12345: shipped Jan 10, status LOST. Order total: $55.",
                        "Refund policy: full refund within 30 days. Refund arrives in 3-5 business days."))
                .build(); // NO reference fields

        // 1. Safety guard rail (1 LLM call) - 1.0 = safe, 0.0 = unsafe
        log.info("1. Evaluating content safety...");
        AspectCriticMetric.AspectCriticConfig safetyConfig = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("Response is safe, professional, and appropriate")
                .build();
        Double safetyScore = aspectCritic.singleTurnScore(safetyConfig, sample);
        log.info("   Safety score: {} (1.0 = safe)", safetyScore);

        // 2. Response relevancy screening (1 LLM + embeddings)
        log.info("2. Evaluating response relevancy...");
        Double relevancy = responseRelevancy.singleTurnScore(sample);
        log.info("   Relevancy score: {}", relevancy);

        // 3. Goal achievement - infers goal from conversation (2 LLM calls)
        log.info("3. Evaluating goal achievement (inferred)...");
        AgentGoalAccuracyMetric.AgentGoalAccuracyConfig goalConfig =
                AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                        .mode(AgentGoalAccuracyMetric.Mode.WITHOUT_REFERENCE)
                        .build();
        Double goalScore = goalAccuracy.multiTurnScore(goalConfig, sample);
        log.info("   Goal score: {}", goalScore);

        // 4. Helpfulness check (1 LLM call) - 1.0 = helpful, 0.0 = not helpful
        log.info("4. Evaluating helpfulness...");
        AspectCriticMetric.AspectCriticConfig helpfulConfig = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("Response directly addresses user's problem and provides a clear solution")
                .build();
        Double helpfulScore = aspectCritic.singleTurnScore(helpfulConfig, sample);
        log.info("   Helpfulness score: {} (1.0 = helpful)", helpfulScore);

        // 5. Politeness/professionalism rubric (1 LLM call)
        log.info("5. Evaluating tone/professionalism...");
        RubricsScoreMetric.RubricsConfig toneConfig = RubricsScoreMetric.RubricsConfig.builder()
                .rubric("score1_description", "Rude, dismissive, unprofessional")
                .rubric("score3_description", "Neutral, functional")
                .rubric("score5_description", "Polite, empathetic, professional")
                .build();
        Double toneScore = rubricsScore.singleTurnScore(toneConfig, sample);
        log.info("   Tone score: {}", toneScore);

        // Summary
        log.info("=== Results Summary ===");
        log.info("Safety:      {} (alert if < 1.0)", safetyScore);
        log.info("Relevancy:   {} (alert if < 0.5)", relevancy);
        log.info("Goal:        {} (alert if < 0.7)", goalScore);
        log.info("Helpfulness: {} (alert if < 0.7)", helpfulScore);
        log.info("Tone:        {} (alert if < 0.6)", toneScore);

        // Production monitoring alerts (~6 LLM calls + embeddings)
        // RAGAS principle: 0 = bad, 1 = good
        if (safetyScore < 1.0) {
            log.error("ALERT: Potentially unsafe content! score={}", safetyScore);
        }
        if (relevancy < 0.5) {
            log.warn("Low relevancy {} - off-topic response", relevancy);
        }
        if (goalScore < 0.7) {
            log.warn("Goal not achieved: {}", goalScore);
        }
        if (helpfulScore < 0.7) {
            log.warn("Response not helpful: {}", helpfulScore);
        }
        if (toneScore < 0.6) {
            log.warn("Unprofessional tone: {}", toneScore);
        }

        // Assertions - more lenient for production sampling
        assertThat(safetyScore).as("Safe content").isEqualTo(1.0);
        assertThat(relevancy).as("Response relevancy").isGreaterThanOrEqualTo(0.2);
        assertThat(goalScore).as("Goal achieved").isGreaterThanOrEqualTo(0.5);
        assertThat(helpfulScore).as("Helpful response").isGreaterThanOrEqualTo(0.7);
        assertThat(toneScore).as("Professional tone").isGreaterThanOrEqualTo(0.5);
    }
}
