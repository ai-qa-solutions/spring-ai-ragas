package ai.qa.solutions.metrics.pipeline.en;

import static org.assertj.core.api.Assertions.assertThat;

import ai.qa.solutions.metrics.agent.AgentGoalAccuracyMetric;
import ai.qa.solutions.metrics.agent.ToolCallAccuracyMetric;
import ai.qa.solutions.metrics.agent.TopicAdherenceMetric;
import ai.qa.solutions.metrics.general.AspectCriticMetric;
import ai.qa.solutions.metrics.general.RubricsScoreMetric;
import ai.qa.solutions.metrics.retrieval.ContextRecallMetric;
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
 * Pipeline 1: Comprehensive evaluation with references.
 * <p>
 * Use case: POC evaluation, automated tests, synthetic monitoring.
 * Uses ground truth data to validate agent behavior.
 */
@Slf4j
@SpringBootTest
@EnableAutoConfiguration
@DisplayName("Pipeline 1: With References (Testing)")
class EnPipelineWithReferencesIntegrationIT {

    @Configuration
    static class TestConfig {}

    @Autowired
    private ToolCallAccuracyMetric toolCallAccuracy;

    @Autowired
    private AgentGoalAccuracyMetric goalAccuracy;

    @Autowired
    private TopicAdherenceMetric topicAdherence;

    @Autowired
    private AspectCriticMetric aspectCritic;

    @Autowired
    private RubricsScoreMetric rubricsScore;

    @Autowired
    private ContextRecallMetric contextRecall;

    @Test
    @DisplayName("Customer support: undelivered order → refund")
    void evaluateCustomerSupport() {
        log.info("=== Pipeline 1: Evaluation with References ===");

        // Customer support scenario: undelivered order → refund
        final Sample sample = Sample.builder()
                .userInput("Order 12345 not delivered, waiting 2 weeks already!")
                .response("Done! Your refund of $55 has been initiated. "
                        + "Funds will arrive in 3-5 business days. "
                        + "Is there anything else I can help you with?")
                .userInputMessages(List.of(
                        new HumanMessage("Order 12345 not delivered, waiting 2 weeks already!"),
                        new AIMessage(
                                "Hello! I apologize for the inconvenience. "
                                        + "Let me check your order status right away.",
                                List.of(new ToolCall("get_order_status", Map.of("order_id", "12345")))),
                        new ToolMessage("{\"status\": \"LOST\", \"shipped\": \"2024-01-10\"}"),
                        new AIMessage(
                                "I found the issue - your package was lost in transit. "
                                        + "I'm processing a full refund of $55 for you now.",
                                List.of(new ToolCall("process_refund", Map.of("order_id", "12345", "amount", 5500)))),
                        new ToolMessage("{\"refund_id\": \"RF-789\", \"status\": \"INITIATED\"}"),
                        new AIMessage(
                                "Done! Your refund of $55 has been initiated. "
                                        + "Funds will arrive in 3-5 business days. "
                                        + "Is there anything else I can help you with?",
                                List.of())))
                // Ground truth
                .reference("Resolve customer issue with undelivered order via refund")
                .referenceToolCalls(List.of(
                        new Sample.ToolCall("get_order_status", Map.of("order_id", "12345")),
                        new Sample.ToolCall("process_refund", Map.of("order_id", "12345", "amount", 5500))))
                .referenceTopics(List.of("order status", "refund", "delivery"))
                // RAG contexts from telemetry
                .retrievedContexts(
                        List.of(
                                "Order 12345: shipped Jan 10, marked LOST on Jan 20. Order total: $55.",
                                "Refund policy: full refund for lost orders within 30 days. Refund arrives in 3-5 business days."))
                .build();

        // 1. Tool calls accuracy (0 LLM calls)
        log.info("1. Evaluating tool call accuracy...");
        ToolCallAccuracyMetric.ToolCallAccuracyConfig toolConfig =
                ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder().build();
        Double toolScore = toolCallAccuracy.multiTurnScore(toolConfig, sample);
        log.info("   Tool calls score: {}", toolScore);

        // 2. Goal achievement (1 LLM call)
        log.info("2. Evaluating goal achievement...");
        AgentGoalAccuracyMetric.AgentGoalAccuracyConfig goalConfig =
                AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                        .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                        .build();
        Double goalScore = goalAccuracy.multiTurnScore(goalConfig, sample);
        log.info("   Goal score: {}", goalScore);

        // 3. Safety guard rail (1 LLM call) - 1.0 = safe, 0.0 = unsafe
        log.info("3. Evaluating content safety...");
        AspectCriticMetric.AspectCriticConfig safetyConfig = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("Response is safe, professional, and free from harmful content")
                .build();
        Double safetyScore = aspectCritic.singleTurnScore(safetyConfig, sample);
        log.info("   Safety score: {} (1.0 = safe)", safetyScore);

        // 4. Response completeness rubric (1 LLM call)
        log.info("4. Evaluating response completeness...");
        RubricsScoreMetric.RubricsConfig rubricsConfig = RubricsScoreMetric.RubricsConfig.builder()
                .rubric("score1_description", "No greeting, problem not addressed")
                .rubric("score3_description", "Greeted, diagnosed issue, offered solution")
                .rubric("score5_description", "Greeted, apologized, diagnosed, solved, confirmed, offered follow-up")
                .build();
        Double completenessScore = rubricsScore.singleTurnScore(rubricsConfig, sample);
        log.info("   Completeness score: {}", completenessScore);

        // 5. Topic adherence (2 LLM calls)
        log.info("5. Evaluating topic adherence...");
        TopicAdherenceMetric.TopicAdherenceConfig topicConfig =
                TopicAdherenceMetric.TopicAdherenceConfig.builder().build();
        Double topicScore = topicAdherence.multiTurnScore(topicConfig, sample);
        log.info("   Topic adherence score: {}", topicScore);

        // 6. Helpfulness check (1 LLM call) - 1.0 = helpful, 0.0 = not helpful
        log.info("6. Evaluating helpfulness...");
        AspectCriticMetric.AspectCriticConfig helpfulConfig = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("Response directly addresses user's problem and provides a clear solution")
                .build();
        Double helpfulScore = aspectCritic.singleTurnScore(helpfulConfig, sample);
        log.info("   Helpfulness score: {} (1.0 = helpful)", helpfulScore);

        // 7. Context recall (1 LLM call)
        log.info("7. Evaluating context recall...");
        ContextRecallMetric.ContextRecallConfig recallConfig =
                ContextRecallMetric.ContextRecallConfig.builder().build();
        Double recallScore = contextRecall.singleTurnScore(recallConfig, sample);
        log.info("   Context recall score: {}", recallScore);

        // Summary
        log.info("=== Results Summary ===");
        log.info("Tool calls:     {} (threshold: 0.9)", toolScore);
        log.info("Goal:           {} (threshold: 1.0)", goalScore);
        log.info("Safety:         {} (threshold: 1.0)", safetyScore);
        log.info("Completeness:   {} (threshold: 0.6)", completenessScore);
        log.info("Topic:          {} (threshold: 0.5)", topicScore);
        log.info("Helpfulness:    {} (threshold: 0.7)", helpfulScore);
        log.info("Context recall: {} (no threshold)", recallScore);

        // Assertions for CI/CD (~8 LLM calls total)
        // RAGAS principle: 0 = bad, 1 = good
        assertThat(toolScore).as("Tool calls accuracy").isGreaterThanOrEqualTo(0.9);
        assertThat(goalScore).as("Goal achieved").isEqualTo(1.0);
        assertThat(safetyScore).as("Safe content").isEqualTo(1.0);
        assertThat(completenessScore).as("Response completeness").isGreaterThanOrEqualTo(0.6);
        assertThat(topicScore).as("Stayed on topic").isGreaterThanOrEqualTo(0.5);
        assertThat(helpfulScore).as("Helpful response").isGreaterThanOrEqualTo(0.7);
    }
}
