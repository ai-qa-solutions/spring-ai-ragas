package ai.qa.solutions.metrics.agent.ru;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.agent.AgentGoalAccuracyMetric;
import ai.qa.solutions.sample.Sample;
import ai.qa.solutions.sample.message.AIMessage;
import ai.qa.solutions.sample.message.HumanMessage;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

/**
 * Интеграционные тесты для метрики Agent Goal Accuracy - Русский язык.
 * <p>
 * Тестирует оценку достижения агентом поставленной цели
 * на основе анализа многоходового диалога.
 * <p>
 * Основные характеристики:
 * - Режим WITH_REFERENCE: сравнение результата с предоставленной ожидаемой целью
 * - Режим WITHOUT_REFERENCE: вывод цели из диалога и оценка её достижения
 * - Возвращает бинарный балл: 1.0 (достигнуто) или 0.0 (не достигнуто)
 */
@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("Метрика Agent Goal Accuracy - Валидация на русском языке")
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".*")
@SpringBootTest(classes = RuAgentGoalAccuracyIntegrationIT.AgentGoalAccuracyIntegrationTestConfiguration.class)
class RuAgentGoalAccuracyIntegrationIT {

    @Configuration
    public static class AgentGoalAccuracyIntegrationTestConfiguration {}

    @Autowired
    private AgentGoalAccuracyMetric agentGoalAccuracyMetric;

    @Nested
    @DisplayName("Тесты режима WITH_REFERENCE")
    class WithReferenceModeTests {

        @Test
        @DisplayName("Цель явно достигнута - ОЖИДАЕМЫЙ ВЫСОКИЙ БАЛЛ (1.0)")
        void testGoalClearlyAchieved() {
            log.info("=== Тест: Цель явно достигнута ===");

            Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage(
                                    "Мне нужно забронировать билет на поезд из Москвы в Санкт-Петербург на завтра."),
                            new AIMessage("Помогу вам с бронированием. Сейчас поищу доступные рейсы."),
                            new AIMessage("Нашёл несколько вариантов на завтра. Лучший вариант - Сапсан, "
                                    + "отправление в 8:00, стоимость 4500 рублей."),
                            new HumanMessage("Отлично, забронируйте этот рейс."),
                            new AIMessage("Готово! Я успешно забронировал для вас билет на Сапсан из Москвы "
                                    + "в Санкт-Петербург на завтра в 8:00. Номер бронирования: СП12345. "
                                    + "Подтверждение отправлено на вашу электронную почту.")))
                    .reference("Забронировать билет на поезд из Москвы в Санкт-Петербург")
                    .build();

            AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                            .build();

            Double score = agentGoalAccuracyMetric.singleTurnScore(config, sample);

            log.info("Цель: Забронировать билет на поезд из Москвы в Санкт-Петербург");
            log.info("Балл: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.5, "Явно достигнутая цель должна иметь высокий балл. Получено: " + score);
        }

        @Test
        @DisplayName("Цель не достигнута - ОЖИДАЕМЫЙ НИЗКИЙ БАЛЛ (0.0)")
        void testGoalNotAchieved() {
            log.info("=== Тест: Цель не достигнута ===");

            Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage(
                                    "Мне нужно забронировать билет на поезд из Москвы в Санкт-Петербург на завтра."),
                            new AIMessage("Конечно, помогу вам. Сейчас проверю доступные рейсы."),
                            new AIMessage("К сожалению, в данный момент система бронирования недоступна. "
                                    + "Сервис временно не работает."),
                            new HumanMessage("Можете сделать что-то ещё?"),
                            new AIMessage("Увы, я не могу завершить бронирование сейчас. "
                                    + "Попробуйте позже или обратитесь напрямую в кассу.")))
                    .reference("Забронировать билет на поезд из Москвы в Санкт-Петербург")
                    .build();

            AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                            .build();

            Double score = agentGoalAccuracyMetric.singleTurnScore(config, sample);

            log.info("Цель: Забронировать билет на поезд из Москвы в Санкт-Петербург");
            log.info("Балл: {}", score);

            assertNotNull(score);
            assertTrue(score <= 0.5, "Недостигнутая цель должна иметь низкий балл. Получено: " + score);
        }

        @Test
        @DisplayName("Решение проблемы клиента - ОЖИДАЕМЫЙ ВЫСОКИЙ БАЛЛ")
        void testCustomerSupportResolution() {
            log.info("=== Тест: Решение проблемы клиента ===");

            Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("Мой заказ #12345 ещё не доставлен. Прошло уже 2 недели."),
                            new AIMessage("Приношу извинения за задержку. Сейчас проверю ваш заказ."),
                            new AIMessage("Нашёл ваш заказ. Произошла проблема с доставкой. Я оформил полный "
                                    + "возврат суммы 5500 рублей на ваш способ оплаты. Деньги поступят "
                                    + "в течение 3-5 рабочих дней."),
                            new HumanMessage("Спасибо! Это меня устраивает."),
                            new AIMessage("Рад помочь! Могу ещё чем-то быть полезен?")))
                    .reference("Решить проблему клиента с недоставленным заказом путём возврата или замены")
                    .build();

            AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                            .build();

            Double score = agentGoalAccuracyMetric.singleTurnScore(config, sample);

            log.info("Цель: Решить проблему клиента с недоставленным заказом");
            log.info("Балл: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.5, "Решённая проблема клиента должна иметь высокий балл. Получено: " + score);
        }
    }

    @Nested
    @DisplayName("Тесты режима WITHOUT_REFERENCE")
    class WithoutReferenceModeTests {

        @Test
        @DisplayName("Вывод цели и оценка - успешная задача")
        void testInferGoalSuccessfulTask() {
            log.info("=== Тест: Вывод цели - успешная задача ===");

            Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage(
                                    "Можете помочь написать письмо начальнику с просьбой об отпуске на следующей неделе?"),
                            new AIMessage("Конечно, помогу составить такое письмо. Вот профессиональная "
                                    + "просьба об отпуске:\n\n"
                                    + "Тема: Заявление на отпуск - следующая неделя\n\n"
                                    + "Уважаемый [Имя руководителя],\n\n"
                                    + "Прошу предоставить мне отпуск с [дата начала] по [дата окончания]. "
                                    + "Мои текущие проекты находятся в срок, и я договорился о подмене "
                                    + "на время моего отсутствия.\n\n"
                                    + "Пожалуйста, сообщите, если потребуется дополнительная информация.\n\n"
                                    + "С уважением,\n[Ваше имя]"),
                            new HumanMessage("Отлично! Спасибо большое!")))
                    .build();

            AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITHOUT_REFERENCE)
                            .build();

            Double score = agentGoalAccuracyMetric.singleTurnScore(config, sample);

            log.info("Диалог: Помощь в написании письма об отпуске");
            log.info("Балл: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.5, "Выведенная и достигнутая цель должна иметь высокий балл. Получено: " + score);
        }

        @Test
        @DisplayName("Вывод цели и оценка - неуспешная задача")
        void testInferGoalFailedTask() {
            log.info("=== Тест: Вывод цели - неуспешная задача ===");

            Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("Переведите этот документ с французского на русский."),
                            new AIMessage("Рад помочь с переводом. Пожалуйста, отправьте документ."),
                            new HumanMessage("Вот он: 'Bonjour, je m'appelle Pierre...' [остальной документ]"),
                            new AIMessage("Приношу извинения, но сейчас у меня возникли технические проблемы "
                                    + "и я не могу выполнить перевод. Попробуйте позже."),
                            new HumanMessage("Очень жаль. Мне это было нужно сегодня.")))
                    .build();

            AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITHOUT_REFERENCE)
                            .build();

            Double score = agentGoalAccuracyMetric.singleTurnScore(config, sample);

            log.info("Диалог: Неудачный запрос на перевод");
            log.info("Балл: {}", score);

            assertNotNull(score);
            assertTrue(score <= 0.5, "Выведенная, но не достигнутая цель должна иметь низкий балл. Получено: " + score);
        }

        @Test
        @DisplayName("Многоэтапное выполнение задачи")
        void testMultiStepTaskCompletion() {
            log.info("=== Тест: Многоэтапное выполнение задачи ===");

            Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("Хочу назначить встречу с командой на следующую пятницу."),
                            new AIMessage("Помогу организовать встречу. В какое время вам удобно?"),
                            new HumanMessage("Давайте в 14:00."),
                            new AIMessage("Хорошо. Кого пригласить на встречу?"),
                            new HumanMessage("Всю команду разработки - Ивана, Анну и Михаила."),
                            new AIMessage("Отлично. Я создал встречу на следующую пятницу в 14:00 с Иваном, Анной "
                                    + "и Михаилом. Приглашения отправлены всем участникам. Встреча состоится "
                                    + "в переговорной А. Хотите добавить повестку?"),
                            new HumanMessage("Нет, это всё, что нужно. Спасибо!")))
                    .build();

            AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITHOUT_REFERENCE)
                            .build();

            Double score = agentGoalAccuracyMetric.singleTurnScore(config, sample);

            log.info("Диалог: Многоэтапная организация встречи");
            log.info("Балл: {}", score);

            assertNotNull(score);
            assertTrue(
                    score >= 0.5,
                    "Успешно завершённая многоэтапная задача должна иметь высокий балл. Получено: " + score);
        }
    }

    @Nested
    @DisplayName("Граничные случаи")
    class EdgeCasesTests {

        @Test
        @DisplayName("Сложный диалог с неявной целью")
        void testComplexConversationImplicitGoal() {
            log.info("=== Тест: Сложный диалог с неявной целью ===");

            Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("Чувствую себя перегруженным на работе в последнее время."),
                            new AIMessage("Сожалею, что вы так себя чувствуете. Хотите поговорить о том, "
                                    + "что вызывает перегрузку, или предпочитаете практические советы "
                                    + "по управлению нагрузкой?"),
                            new HumanMessage("Практические советы были бы полезны."),
                            new AIMessage("Вот несколько стратегий, которые могут помочь:\n\n"
                                    + "1. Приоритизируйте задачи по матрице Эйзенхауэра - сначала срочные "
                                    + "и важные задачи.\n"
                                    + "2. Разбивайте крупные проекты на мелкие управляемые шаги.\n"
                                    + "3. Устанавливайте границы - учитесь говорить нет несущественным запросам.\n"
                                    + "4. Делайте регулярные перерывы по технике Помодоро.\n"
                                    + "5. Рассмотрите возможность делегирования.\n\n"
                                    + "Хотите, чтобы я подробнее рассказал о каком-либо пункте?"),
                            new HumanMessage("Очень полезно. Начну с техники Помодоро.")))
                    .reference("Помочь пользователю справиться со стрессом на работе практическими советами")
                    .build();

            AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                            .build();

            Double score = agentGoalAccuracyMetric.singleTurnScore(config, sample);

            log.info("Цель: Помочь справиться со стрессом на работе");
            log.info("Балл: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.5, "Достигнутая неявная цель должна иметь высокий балл. Получено: " + score);
        }

        @Test
        @DisplayName("Асинхронное вычисление работает корректно")
        void testAsyncScoring() {
            log.info("=== Тест: Асинхронное вычисление ===");

            Sample sample = Sample.builder()
                    .userInputMessages(
                            List.of(new HumanMessage("Сколько будет 2 + 2?"), new AIMessage("2 + 2 равно 4.")))
                    .reference("Дать правильный ответ на математический вопрос")
                    .build();

            AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                            .build();

            Double score =
                    agentGoalAccuracyMetric.singleTurnScoreAsync(config, sample).join();

            log.info("Асинхронный балл: {}", score);

            assertNotNull(score);
            assertTrue(
                    score >= 0.5, "Асинхронное вычисление должно работать так же, как синхронное. Получено: " + score);
        }
    }
}
