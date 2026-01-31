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

            final Sample sample = Sample.builder()
                    .userInputMessages(
                            List.of(
                                    new HumanMessage("Нужен билет на поезд Москва-Питер на завтра"),
                                    new AIMessage("Проверяю доступные рейсы на завтра."),
                                    new AIMessage(
                                            """
                                    Нашёл два варианта: Сапсан в 8:00 за 4500 рублей \
                                    или Ласточка в 10:30 за 2800 рублей. Какой предпочитаете?"""),
                                    new HumanMessage("Давай Сапсан, мне пораньше надо"),
                                    new AIMessage(
                                            """
                                    Билет на Сапсан забронирован. Отправление завтра в 8:00, место 5В. \
                                    Номер брони: СП12345. Электронный билет отправлен на вашу почту.""")))
                    .reference("Забронировать билет на поезд из Москвы в Санкт-Петербург")
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                            .build();

            final Double score = agentGoalAccuracyMetric.singleTurnScore(config, sample);

            log.info("Цель: Забронировать билет на поезд из Москвы в Санкт-Петербург");
            log.info("Балл: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.5, "Явно достигнутая цель должна иметь высокий балл. Получено: " + score);
        }

        @Test
        @DisplayName("Цель не достигнута - ОЖИДАЕМЫЙ НИЗКИЙ БАЛЛ (0.0)")
        void testGoalNotAchieved() {
            log.info("=== Тест: Цель не достигнута ===");

            final Sample sample = Sample.builder()
                    .userInputMessages(
                            List.of(
                                    new HumanMessage("Забронируй мне поезд до Питера на завтра"),
                                    new AIMessage("Проверяю доступность билетов."),
                                    new AIMessage("К сожалению, система бронирования РЖД временно недоступна."),
                                    new HumanMessage("И что делать?"),
                                    new AIMessage(
                                            """
                                    Приношу извинения за неудобства. Рекомендую попробовать позже \
                                    или обратиться в кассу на вокзале.""")))
                    .reference("Забронировать билет на поезд из Москвы в Санкт-Петербург")
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                            .build();

            final Double score = agentGoalAccuracyMetric.singleTurnScore(config, sample);

            log.info("Цель: Забронировать билет на поезд из Москвы в Санкт-Петербург");
            log.info("Балл: {}", score);

            assertNotNull(score);
            assertTrue(score <= 0.5, "Недостигнутая цель должна иметь низкий балл. Получено: " + score);
        }

        @Test
        @DisplayName("Решение проблемы клиента - ОЖИДАЕМЫЙ ВЫСОКИЙ БАЛЛ")
        void testCustomerSupportResolution() {
            log.info("=== Тест: Решение проблемы клиента ===");

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("Алло, заказ 12345 где? Две недели жду уже!"),
                            new AIMessage("Приношу извинения за задержку. Сейчас проверю статус вашего заказа."),
                            new AIMessage(
                                    """
                                    Заказ найден. К сожалению, посылка была утеряна при сортировке. \
                                    Оформила полный возврат 5500 рублей — средства поступят в течение 3-5 рабочих дней."""),
                            new HumanMessage("Ну ладно, спасибо хоть за это"),
                            new AIMessage(
                                    "Ещё раз приношу извинения за ситуацию. Если возникнут вопросы — обращайтесь.")))
                    .reference("Решить проблему клиента с недоставленным заказом путём возврата или замены")
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                            .build();

            final Double score = agentGoalAccuracyMetric.singleTurnScore(config, sample);

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

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("Помоги накатать заявление на отпуск, а то сам не умею красиво"),
                            new AIMessage(
                                    """
                                    Конечно, подготовила для вас шаблон:

                                    Тема: Заявление на отпуск

                                    Уважаемый Иван Петрович,

                                    Прошу предоставить мне ежегодный отпуск с [дата] по [дата]. \
                                    Все текущие задачи будут завершены, на время отсутствия меня подстрахует коллега.

                                    С уважением,
                                    [Имя]"""),
                            new HumanMessage("Супер, то что надо!")))
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITHOUT_REFERENCE)
                            .build();

            final Double score = agentGoalAccuracyMetric.singleTurnScore(config, sample);

            log.info("Диалог: Помощь в написании письма об отпуске");
            log.info("Балл: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.5, "Выведенная и достигнутая цель должна иметь высокий балл. Получено: " + score);
        }

        @Test
        @DisplayName("Вывод цели и оценка - неуспешная задача")
        void testInferGoalFailedTask() {
            log.info("=== Тест: Вывод цели - неуспешная задача ===");

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("Переведи с французского, плиз"),
                            new AIMessage("С удовольствием помогу с переводом. Пришлите текст."),
                            new HumanMessage("Вот: 'Bonjour, je m'appelle Pierre et je travaille...'"),
                            new AIMessage(
                                    """
                                    Приношу извинения, но в данный момент возникли технические неполадки. \
                                    Не могу выполнить перевод. Попробуйте, пожалуйста, позже."""),
                            new HumanMessage("Блин, мне срочно было надо...")))
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITHOUT_REFERENCE)
                            .build();

            final Double score = agentGoalAccuracyMetric.singleTurnScore(config, sample);

            log.info("Диалог: Неудачный запрос на перевод");
            log.info("Балл: {}", score);

            assertNotNull(score);
            assertTrue(score <= 0.5, "Выведенная, но не достигнутая цель должна иметь низкий балл. Получено: " + score);
        }

        @Test
        @DisplayName("Многоэтапное выполнение задачи")
        void testMultiStepTaskCompletion() {
            log.info("=== Тест: Многоэтапное выполнение задачи ===");

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("Надо созвон с командой на следующую пятницу организовать"),
                            new AIMessage("Помогу организовать встречу. В какое время вам удобно?"),
                            new HumanMessage("Часа в 2 дня"),
                            new AIMessage("Принято. Кого пригласить на встречу?"),
                            new HumanMessage("Всех наших — Ваню, Аню и Мишу"),
                            new AIMessage(
                                    """
                                    Встреча создана на следующую пятницу в 14:00. \
                                    Приглашения отправлены Ивану, Анне и Михаилу. \
                                    Переговорная А забронирована. Добавить повестку?"""),
                            new HumanMessage("Не, всё норм, спасибо!")))
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITHOUT_REFERENCE)
                            .build();

            final Double score = agentGoalAccuracyMetric.singleTurnScore(config, sample);

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

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("Что-то меня на работе накрывает последнее время, не знаю что делать"),
                            new AIMessage(
                                    """
                                    Понимаю, это непросто. Хотите обсудить причины перегрузки \
                                    или предпочитаете практические советы по управлению нагрузкой?"""),
                            new HumanMessage("Давай советы какие-нибудь"),
                            new AIMessage(
                                    """
                                    Вот несколько эффективных стратегий:

                                    1. Приоритизируйте задачи по матрице Эйзенхауэра — сначала срочные и важные.
                                    2. Разбивайте крупные проекты на небольшие шаги.
                                    3. Учитесь говорить «нет» несущественным запросам.
                                    4. Делайте регулярные перерывы по технике Помодоро.
                                    5. Делегируйте где возможно.

                                    Рассказать подробнее о каком-либо пункте?"""),
                            new HumanMessage("О, клёво, попробую Помодоро")))
                    .reference("Помочь пользователю справиться со стрессом на работе практическими советами")
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                            .build();

            final Double score = agentGoalAccuracyMetric.singleTurnScore(config, sample);

            log.info("Цель: Помочь справиться со стрессом на работе");
            log.info("Балл: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.5, "Достигнутая неявная цель должна иметь высокий балл. Получено: " + score);
        }

        @Test
        @DisplayName("Асинхронное вычисление работает корректно")
        void testAsyncScoring() {
            log.info("=== Тест: Асинхронное вычисление ===");

            final Sample sample = Sample.builder()
                    .userInputMessages(
                            List.of(new HumanMessage("Сколько будет 2 + 2?"), new AIMessage("2 + 2 равно 4.")))
                    .reference("Дать правильный ответ на математический вопрос")
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                            .build();

            final Double score =
                    agentGoalAccuracyMetric.singleTurnScoreAsync(config, sample).join();

            log.info("Асинхронный балл: {}", score);

            assertNotNull(score);
            assertTrue(
                    score >= 0.5, "Асинхронное вычисление должно работать так же, как синхронное. Получено: " + score);
        }
    }
}
