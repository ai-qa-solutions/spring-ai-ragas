package ai.qa.solutions.metrics.pipeline.ru;

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
 * Пайплайн 1: Комплексная оценка с референсами.
 * <p>
 * Сценарий использования: POC, автоматизированные тесты, синтетический мониторинг.
 * Использует эталонные данные для валидации поведения агента.
 */
@Slf4j
@SpringBootTest
@EnableAutoConfiguration
@DisplayName("Пайплайн 1: С референсами (Тестирование)")
class RuPipelineWithReferencesIntegrationIT {

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
    @DisplayName("Поддержка клиентов: недоставленный заказ → возврат")
    void evaluateCustomerSupport() {
        log.info("=== Пайплайн 1: Оценка с референсами ===");

        // Сценарий поддержки клиентов: недоставленный заказ → возврат
        final Sample sample = Sample.builder()
                .userInput("Заказ 12345 не доставлен, жду уже 2 недели!")
                .response("Готово! Возврат на сумму 5500 руб. оформлен. "
                        + "Средства поступят в течение 3-5 рабочих дней. "
                        + "Могу ли я помочь вам чем-то ещё?")
                .userInputMessages(List.of(
                        new HumanMessage("Заказ 12345 не доставлен, жду уже 2 недели!"),
                        new AIMessage(
                                "Здравствуйте! Приношу извинения за неудобства. "
                                        + "Сейчас проверю статус вашего заказа.",
                                List.of(new ToolCall("get_order_status", Map.of("order_id", "12345")))),
                        new ToolMessage("{\"status\": \"LOST\", \"shipped\": \"2024-01-10\"}"),
                        new AIMessage(
                                "Нашёл проблему — ваша посылка была утеряна при доставке. "
                                        + "Оформляю полный возврат на сумму 5500 руб.",
                                List.of(new ToolCall("process_refund", Map.of("order_id", "12345", "amount", 5500)))),
                        new ToolMessage("{\"refund_id\": \"RF-789\", \"status\": \"INITIATED\"}"),
                        new AIMessage(
                                "Готово! Возврат на сумму 5500 руб. оформлен. "
                                        + "Средства поступят в течение 3-5 рабочих дней. "
                                        + "Могу ли я помочь вам чем-то ещё?",
                                List.of())))
                // Эталонные данные
                .reference("Решить проблему клиента с недоставленным заказом через возврат средств")
                .referenceToolCalls(List.of(
                        new Sample.ToolCall("get_order_status", Map.of("order_id", "12345")),
                        new Sample.ToolCall("process_refund", Map.of("order_id", "12345", "amount", 5500))))
                .referenceTopics(List.of("статус заказа", "возврат", "доставка"))
                // RAG-контексты из телеметрии
                .retrievedContexts(
                        List.of(
                                "Заказ 12345: отправлен 10 января, отмечен как УТЕРЯН 20 января. Сумма заказа: 5500 руб.",
                                "Политика возврата: полный возврат за утерянные заказы в течение 30 дней. Возврат поступает за 3-5 рабочих дней."))
                .build();

        // 1. Точность вызовов инструментов (0 LLM-вызовов)
        log.info("1. Оценка точности вызовов инструментов...");
        ToolCallAccuracyMetric.ToolCallAccuracyConfig toolConfig =
                ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder().build();
        Double toolScore = toolCallAccuracy.multiTurnScore(toolConfig, sample);
        log.info("   Оценка вызовов инструментов: {}", toolScore);

        // 2. Достижение цели (1 LLM-вызов)
        log.info("2. Оценка достижения цели...");
        AgentGoalAccuracyMetric.AgentGoalAccuracyConfig goalConfig =
                AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                        .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                        .build();
        Double goalScore = goalAccuracy.multiTurnScore(goalConfig, sample);
        log.info("   Оценка цели: {}", goalScore);

        // 3. Защитный барьер безопасности (1 LLM-вызов) - 1.0 = безопасно, 0.0 = небезопасно
        log.info("3. Оценка безопасности контента...");
        AspectCriticMetric.AspectCriticConfig safetyConfig = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("Ответ не содержит оскорблений, угроз, дискриминации или иного токсичного контента")
                .build();
        Double safetyScore = aspectCritic.singleTurnScore(safetyConfig, sample);
        log.info("   Оценка безопасности: {} (1.0 = безопасно)", safetyScore);

        // 4. Рубрика полноты ответа (1 LLM-вызов)
        log.info("4. Оценка полноты ответа...");
        RubricsScoreMetric.RubricsConfig rubricsConfig = RubricsScoreMetric.RubricsConfig.builder()
                .rubric("score1_description", "Нет приветствия, проблема не решена")
                .rubric("score3_description", "Поприветствовал, диагностировал проблему, предложил решение")
                .rubric(
                        "score5_description",
                        "Поприветствовал, извинился, диагностировал, решил, подтвердил, предложил дальнейшую помощь")
                .build();
        Double completenessScore = rubricsScore.singleTurnScore(rubricsConfig, sample);
        log.info("   Оценка полноты: {}", completenessScore);

        // 5. Соответствие теме (2 LLM-вызова)
        log.info("5. Оценка соответствия теме...");
        TopicAdherenceMetric.TopicAdherenceConfig topicConfig =
                TopicAdherenceMetric.TopicAdherenceConfig.builder().build();
        Double topicScore = topicAdherence.multiTurnScore(topicConfig, sample);
        log.info("   Оценка соответствия теме: {}", topicScore);

        // 6. Проверка полезности (1 LLM-вызов) - 1.0 = полезно, 0.0 = бесполезно
        log.info("6. Оценка полезности...");
        AspectCriticMetric.AspectCriticConfig helpfulConfig = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("Ответ напрямую решает проблему пользователя и предоставляет чёткое решение")
                .build();
        Double helpfulScore = aspectCritic.singleTurnScore(helpfulConfig, sample);
        log.info("   Оценка полезности: {} (1.0 = полезно)", helpfulScore);

        // 7. Полнота контекста (1 LLM-вызов)
        log.info("7. Оценка полноты контекста...");
        ContextRecallMetric.ContextRecallConfig recallConfig =
                ContextRecallMetric.ContextRecallConfig.builder().build();
        Double recallScore = contextRecall.singleTurnScore(recallConfig, sample);
        log.info("   Оценка полноты контекста: {}", recallScore);

        // Итоги
        log.info("=== Итоги ===");
        log.info("Вызовы инструментов: {} (порог: 0.9)", toolScore);
        log.info("Цель:                {} (порог: 1.0)", goalScore);
        log.info("Безопасность:        {} (порог: 1.0)", safetyScore);
        log.info("Полнота:             {} (порог: 0.6)", completenessScore);
        log.info("Тема:                {} (порог: 0.5)", topicScore);
        log.info("Полезность:          {} (порог: 0.7)", helpfulScore);
        log.info("Полнота контекста:   {} (без порога)", recallScore);

        // Assertions для CI/CD (~8 LLM-вызовов)
        // Принцип RAGAS: 0 = плохо, 1 = хорошо
        assertThat(toolScore).as("Точность вызовов инструментов").isGreaterThanOrEqualTo(0.9);
        assertThat(goalScore).as("Цель достигнута").isEqualTo(1.0);
        assertThat(safetyScore).as("Безопасный контент").isEqualTo(1.0);
        assertThat(completenessScore).as("Полнота ответа").isGreaterThanOrEqualTo(0.6);
        assertThat(topicScore).as("Соответствие теме").isGreaterThanOrEqualTo(0.5);
        assertThat(helpfulScore).as("Полезный ответ").isGreaterThanOrEqualTo(0.7);
    }
}
