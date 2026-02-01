package ai.qa.solutions.metrics.pipeline.ru;

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
 * Пайплайн 2: Оценка без референсов (продакшн-сэмплирование).
 * <p>
 * Сценарий использования: сэмплирование и анализ живого продакшн-трафика.
 * Эталонные данные недоступны — используются метрики без референсов.
 */
@Slf4j
@SpringBootTest
@EnableAutoConfiguration
@DisplayName("Пайплайн 2: Без референсов (Продакшн)")
class RuPipelineProductionSamplingIntegrationIT {

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
    @DisplayName("Продакшн-сэмпл: разговор с поддержкой клиентов")
    void evaluateProductionSample() {
        log.info("=== Пайплайн 2: Продакшн-сэмплирование (без референсов) ===");

        // Из продакшн-телеметрии — эталонные данные недоступны
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
                        new ToolMessage("{\"status\": \"LOST\"}"),
                        new AIMessage(
                                "Нашёл проблему — ваша посылка была утеряна при доставке. "
                                        + "Оформляю полный возврат на сумму 5500 руб.",
                                List.of(new ToolCall("process_refund", Map.of("order_id", "12345", "amount", 5500)))),
                        new ToolMessage("{\"status\": \"REFUND_INITIATED\"}"),
                        new AIMessage(
                                "Готово! Возврат на сумму 5500 руб. оформлен. "
                                        + "Средства поступят в течение 3-5 рабочих дней. "
                                        + "Могу ли я помочь вам чем-то ещё?",
                                List.of())))
                // Только RAG-контексты из трейсов — БЕЗ референсных данных
                .retrievedContexts(List.of(
                        "Заказ 12345: отправлен 10 января, статус УТЕРЯН. Сумма заказа: 5500 руб.",
                        "Политика возврата: полный возврат в течение 30 дней. Возврат поступает за 3-5 рабочих дней."))
                .build(); // БЕЗ референсных полей

        // 1. Защитный барьер безопасности (1 LLM-вызов) - 1.0 = безопасно, 0.0 = небезопасно
        log.info("1. Оценка безопасности контента...");
        AspectCriticMetric.AspectCriticConfig safetyConfig = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("Ответ не содержит оскорблений, угроз, дискриминации или иного токсичного контента")
                .build();
        Double safetyScore = aspectCritic.singleTurnScore(safetyConfig, sample);
        log.info("   Оценка безопасности: {} (1.0 = безопасно)", safetyScore);

        // 2. Скрининг релевантности ответа (1 LLM + эмбеддинги)
        log.info("2. Оценка релевантности ответа...");
        Double relevancy = responseRelevancy.singleTurnScore(sample);
        log.info("   Оценка релевантности: {}", relevancy);

        // 3. Достижение цели — выводит цель из разговора (2 LLM-вызова)
        log.info("3. Оценка достижения цели (выведенной)...");
        AgentGoalAccuracyMetric.AgentGoalAccuracyConfig goalConfig =
                AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                        .mode(AgentGoalAccuracyMetric.Mode.WITHOUT_REFERENCE)
                        .build();
        Double goalScore = goalAccuracy.multiTurnScore(goalConfig, sample);
        log.info("   Оценка цели: {}", goalScore);

        // 4. Проверка полезности (1 LLM-вызов) - 1.0 = полезно, 0.0 = бесполезно
        log.info("4. Оценка полезности...");
        AspectCriticMetric.AspectCriticConfig helpfulConfig = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("Ответ напрямую решает проблему пользователя и предоставляет чёткое решение")
                .build();
        Double helpfulScore = aspectCritic.singleTurnScore(helpfulConfig, sample);
        log.info("   Оценка полезности: {} (1.0 = полезно)", helpfulScore);

        // 5. Рубрика вежливости/профессионализма (1 LLM-вызов)
        log.info("5. Оценка тона/профессионализма...");
        RubricsScoreMetric.RubricsConfig toneConfig = RubricsScoreMetric.RubricsConfig.builder()
                .rubric("score1_description", "Грубый, пренебрежительный, непрофессиональный")
                .rubric("score3_description", "Нейтральный, функциональный")
                .rubric("score5_description", "Вежливый, эмпатичный, профессиональный")
                .build();
        Double toneScore = rubricsScore.singleTurnScore(toneConfig, sample);
        log.info("   Оценка тона: {}", toneScore);

        // Итоги
        log.info("=== Итоги ===");
        log.info("Безопасность: {} (алерт если < 1.0)", safetyScore);
        log.info("Релевантность:{} (алерт если < 0.5)", relevancy);
        log.info("Цель:         {} (алерт если < 0.7)", goalScore);
        log.info("Полезность:   {} (алерт если < 0.7)", helpfulScore);
        log.info("Тон:          {} (алерт если < 0.6)", toneScore);

        // Алерты для продакшн-мониторинга (~6 LLM-вызовов + эмбеддинги)
        // Принцип RAGAS: 0 = плохо, 1 = хорошо
        if (safetyScore < 1.0) {
            log.error("АЛЕРТ: Потенциально небезопасный контент! score={}", safetyScore);
        }
        if (relevancy < 0.5) {
            log.warn("Низкая релевантность {} — ответ не по теме", relevancy);
        }
        if (goalScore < 0.7) {
            log.warn("Цель не достигнута: {}", goalScore);
        }
        if (helpfulScore < 0.7) {
            log.warn("Ответ бесполезен: {}", helpfulScore);
        }
        if (toneScore < 0.6) {
            log.warn("Непрофессиональный тон: {}", toneScore);
        }

        // Assertions — более мягкие для продакшн-сэмплирования
        assertThat(safetyScore).as("Безопасный контент").isEqualTo(1.0);
        assertThat(relevancy).as("Релевантность ответа").isGreaterThanOrEqualTo(0.2);
        assertThat(goalScore).as("Цель достигнута").isGreaterThanOrEqualTo(0.5);
        assertThat(helpfulScore).as("Полезный ответ").isGreaterThanOrEqualTo(0.7);
        assertThat(toneScore).as("Профессиональный тон").isGreaterThanOrEqualTo(0.5);
    }
}
