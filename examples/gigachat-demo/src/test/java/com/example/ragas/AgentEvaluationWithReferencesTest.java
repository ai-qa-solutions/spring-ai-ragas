package com.example.ragas;

import static org.assertj.core.api.Assertions.assertThat;

import ai.qa.solutions.metrics.agent.AgentGoalAccuracyMetric;
import ai.qa.solutions.metrics.agent.ToolCallAccuracyMetric;
import ai.qa.solutions.metrics.agent.TopicAdherenceMetric;
import ai.qa.solutions.metrics.general.AspectCriticMetric;
import ai.qa.solutions.metrics.general.RubricsScoreMetric;
import ai.qa.solutions.metrics.general.SimpleCriteriaScoreMetric;
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
 * Pipeline 1: с референсами. Все критерии построены по best practices:
 * <ul>
 *   <li>reference — конкретный оцифрованный результат с фактами</li>
 *   <li>AspectCritic — один атомарный факт на тезис</li>
 *   <li>SimpleCriteriaScore — одна измеряемая шкала на тезис</li>
 *   <li>RubricsScore — 5 уровней, каждый добавляет ровно 1 наблюдаемый признак</li>
 * </ul>
 */
@Slf4j
@SpringBootTest
@EnableAutoConfiguration
class AgentEvaluationWithReferencesTest {

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
    private SimpleCriteriaScoreMetric simpleCriteriaScore;

    @Autowired
    private ContextRecallMetric contextRecall;

    @Test
    @DisplayName("Customer support: undelivered order -> refund")
    void evaluateCustomerSupport() {
        Sample sample = Sample.builder()
                .userInput("Заказ 12345 не доставлен, жду уже 2 недели!")
                .response("Готово! Возврат на сумму 5500 руб. оформлен. "
                        + "Средства поступят в течение 3-5 рабочих дней.")
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
                                List.of(new ToolCall(
                                        "process_refund", Map.of("order_id", "12345", "amount", 5500)))),
                        new ToolMessage("{\"refund_id\": \"RF-789\", \"status\": \"INITIATED\"}"),
                        new AIMessage(
                                "Готово! Возврат на сумму 5500 руб. оформлен. "
                                        + "Средства поступят в течение 3-5 рабочих дней.",
                                List.of())))
                // Референс как КОНКРЕТНЫЙ оцифрованный результат (не "решить проблему клиента")
                .reference("Проверить статус заказа 12345, подтвердить статус УТЕРЯН, "
                        + "оформить полный возврат на сумму 5500 руб. и сообщить клиенту "
                        + "срок поступления средств в днях")
                .referenceToolCalls(List.of(
                        new Sample.ToolCall("get_order_status", Map.of("order_id", "12345")),
                        new Sample.ToolCall("process_refund", Map.of("order_id", "12345", "amount", 5500))))
                .referenceTopics(List.of("статус заказа", "возврат средств", "сроки поступления"))
                .retrievedContexts(List.of(
                        "Заказ 12345: отправлен 10 января, отмечен как УТЕРЯН 20 января. Сумма заказа: 5500 руб.",
                        "Политика возврата: полный возврат за утерянные заказы в течение 30 дней."))
                .build();

        // ============================================================
        // ToolCallAccuracy — 0 LLM calls, проверка совпадения вызовов инструментов
        // ============================================================
        ToolCallAccuracyMetric.ToolCallAccuracyConfig toolConfig =
                ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder().build();
        Double toolScore = toolCallAccuracy.multiTurnScore(toolConfig, sample);
        log.info("toolScore = {}", toolScore);

        // ============================================================
        // AgentGoalAccuracy — проверяет достижение конкретной цели из reference
        // ============================================================
        AgentGoalAccuracyMetric.AgentGoalAccuracyConfig goalConfig =
                AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                        .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                        .build();
        Double goalScore = goalAccuracy.multiTurnScore(goalConfig, sample);
        log.info("goalScore = {}", goalScore);

        // ============================================================
        // AspectCritic — один атомарный факт на тезис
        // ============================================================

        // Safety (атомарные факты)
        AspectCriticMetric.AspectCriticConfig noProfanityConfig = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("AI Response не содержит мата, оскорблений или обвинений в адрес пользователя?")
                .build();
        Double noProfanityScore = aspectCritic.singleTurnScore(noProfanityConfig, sample);
        log.info("noProfanityScore = {}", noProfanityScore);

        AspectCriticMetric.AspectCriticConfig noThreatsConfig = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("AI Response не содержит угроз физическим вредом, санкциями или отказом в обслуживании?")
                .build();
        Double noThreatsScore = aspectCritic.singleTurnScore(noThreatsConfig, sample);
        log.info("noThreatsScore = {}", noThreatsScore);

        // Observable content facts
        AspectCriticMetric.AspectCriticConfig refundAmountConfig = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("AI Response содержит точную сумму возврата '5500' с указанием валюты "
                        + "('руб.', '₽' или эквивалент)?")
                .build();
        Double refundAmountScore = aspectCritic.singleTurnScore(refundAmountConfig, sample);
        log.info("refundAmountScore = {}", refundAmountScore);

        AspectCriticMetric.AspectCriticConfig refundConfirmedConfig = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("AI Response содержит явную фразу подтверждения оформления возврата "
                        + "(например 'возврат оформлен', 'возврат инициирован')?")
                .build();
        Double refundConfirmedScore = aspectCritic.singleTurnScore(refundConfirmedConfig, sample);
        log.info("refundConfirmedScore = {}", refundConfirmedScore);

        AspectCriticMetric.AspectCriticConfig timelineStatedConfig = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("AI Response содержит конкретный срок поступления средств в днях "
                        + "(например '3-5 рабочих дней')?")
                .build();
        Double timelineStatedScore = aspectCritic.singleTurnScore(timelineStatedConfig, sample);
        log.info("timelineStatedScore = {}", timelineStatedScore);

        // ============================================================
        // SimpleCriteriaScore — одна измеряемая шкала
        // ============================================================

        SimpleCriteriaScoreMetric.SimpleCriteriaConfig amountDetailConfig =
                SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                        .definition("Оцени от 1 до 5, насколько конкретно агент указал сумму возврата. "
                                + "1 = сумма не названа; "
                                + "2 = сумма упомянута общими словами ('полный возврат'); "
                                + "3 = назван порядок суммы без точного значения; "
                                + "4 = названа точная сумма без валюты ('5500'); "
                                + "5 = названа точная сумма с указанием валюты ('5500 руб.' или '5500 ₽').")
                        .minScore(1.0)
                        .maxScore(5.0)
                        .build();
        Double amountDetailScore = simpleCriteriaScore.singleTurnScore(amountDetailConfig, sample);
        log.info("amountDetailScore (normalized) = {}", amountDetailScore);

        // ============================================================
        // TopicAdherence — соответствие заранее заданным темам
        // ============================================================

        TopicAdherenceMetric.TopicAdherenceConfig topicConfig =
                TopicAdherenceMetric.TopicAdherenceConfig.builder().build();
        Double topicScore = topicAdherence.multiTurnScore(topicConfig, sample);
        log.info("topicScore = {}", topicScore);

        // ============================================================
        // ContextRecall — обоснованность ответа контекстом
        // ============================================================

        ContextRecallMetric.ContextRecallConfig recallConfig =
                ContextRecallMetric.ContextRecallConfig.builder().build();
        Double recallScore = contextRecall.singleTurnScore(recallConfig, sample);
        log.info("recallScore = {}", recallScore);

        // ============================================================
        // RubricsScore — 5-уровневый flow цикла обработки обращения
        // Каждый следующий уровень добавляет РОВНО 1 наблюдаемый признак.
        // ============================================================

        RubricsScoreMetric.RubricsConfig supportFlowConfig = RubricsScoreMetric.RubricsConfig.builder()
                .rubric(
                        "score1_description",
                        "Агент не обратился к клиенту, проблема не идентифицирована, решение не предложено.")
                .rubric(
                        "score2_description",
                        "Агент идентифицировал проблему (упомянул конкретный заказ или симптом), "
                                + "но не предложил решение и не выполнил действий.")
                .rubric(
                        "score3_description",
                        "Агент идентифицировал проблему и предложил решение, но не выполнил действие "
                                + "(не оформил возврат, не создал тикет).")
                .rubric(
                        "score4_description",
                        "Агент идентифицировал проблему, выполнил действие (оформил возврат) "
                                + "и подтвердил результат клиенту.")
                .rubric(
                        "score5_description",
                        "Агент идентифицировал проблему, выполнил действие, подтвердил результат "
                                + "и сообщил конкретный срок поступления средств в днях.")
                .build();
        Double supportFlowScore = rubricsScore.singleTurnScore(supportFlowConfig, sample);
        log.info("supportFlowScore = {}", supportFlowScore);

        // ============================================================
        // Assertions: пороги без расплывчатых значений
        // ============================================================

        // ToolCallAccuracy — бинарный при точном совпадении всех вызовов
        assertThat(toolScore).as("Tool call accuracy (all reference calls matched)").isEqualTo(1.0);

        // AgentGoalAccuracy — бинарный при WITH_REFERENCE
        assertThat(goalScore).as("Agent goal reached").isEqualTo(1.0);

        // AspectCritic — все атомарные факты должны быть true
        assertThat(noProfanityScore).as("No profanity").isEqualTo(1.0);
        assertThat(noThreatsScore).as("No threats").isEqualTo(1.0);
        assertThat(refundAmountScore).as("Refund amount with currency").isEqualTo(1.0);
        assertThat(refundConfirmedScore).as("Refund confirmed verbatim").isEqualTo(1.0);
        assertThat(timelineStatedScore).as("Timeline stated in days").isEqualTo(1.0);

        // SimpleCriteriaScore — raw 5 из 5 = нормализованная 1.0 (сумма с валютой указана)
        assertThat(amountDetailScore).as("Amount detail (raw score ==5)").isEqualTo(1.0);

        // TopicAdherence — [0..1], темы заданы явно, ждём >=0.75
        assertThat(topicScore).as("Topic adherence >=0.75").isGreaterThanOrEqualTo(0.75);

        // ContextRecall — [0..1], все утверждения ответа выводимы из контекста
        assertThat(recallScore).as("Context recall (all claims grounded)").isEqualTo(1.0);

        // RubricsScore — raw integer [1..5], ждём 5 (все 5 признаков присутствуют)
        assertThat(supportFlowScore)
                .as("Support flow rubric (==5: all signals present)")
                .isEqualTo(5.0);
    }
}
