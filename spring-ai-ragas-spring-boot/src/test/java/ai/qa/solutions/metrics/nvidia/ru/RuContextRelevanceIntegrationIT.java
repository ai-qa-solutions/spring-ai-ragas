package ai.qa.solutions.metrics.nvidia.ru;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.nvidia.ContextRelevanceMetric;
import ai.qa.solutions.sample.Sample;
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
 * Интеграционные тесты метрики Context Relevance (NVIDIA-стиль) - Русский язык.
 * <p>
 * Тестируют оценку релевантности извлечённых контекстов запросу пользователя.
 * <p>
 * Ключевые характеристики:
 * - Шкала оценки 0-2, нормализованная к 0-1
 * - 0: Нерелевантный, 1: Частично релевантный, 2: Полностью релевантный
 * - Возвращает усреднённую оценку по всем контекстам
 */
@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("Метрика Context Relevance - Валидация на русском языке")
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".*")
@SpringBootTest(classes = RuContextRelevanceIntegrationIT.ContextRelevanceIntegrationTestConfiguration.class)
class RuContextRelevanceIntegrationIT {

    @Configuration
    public static class ContextRelevanceIntegrationTestConfiguration {}

    @Autowired
    private ContextRelevanceMetric contextRelevanceMetric;

    @Nested
    @DisplayName("Тесты оценки релевантности")
    class RelevanceEvaluationTests {

        @Test
        @DisplayName("Высоко релевантный контекст - ОЖИДАЕТСЯ ВЫСОКИЙ БАЛЛ")
        void testHighlyRelevantContext() {
            log.info("=== Тест: высоко релевантный контекст ===");

            final Sample sample = Sample.builder()
                    .userInput("Что такое машинное обучение и как оно работает?")
                    .retrievedContexts(
                            List.of("Машинное обучение — это подраздел искусственного интеллекта, позволяющий системам "
                                    + "автоматически обучаться и совершенствоваться на основе опыта без явного программирования. "
                                    + "Оно работает с помощью алгоритмов, которые итеративно обучаются на данных, позволяя "
                                    + "компьютерам находить скрытые закономерности без указания, где именно искать. Процесс "
                                    + "обучения начинается с наблюдений или данных, таких как примеры, прямой опыт или инструкции, "
                                    + "чтобы находить паттерны в данных и принимать лучшие решения в будущем."))
                    .build();

            final ContextRelevanceMetric.ContextRelevanceConfig config =
                    ContextRelevanceMetric.ContextRelevanceConfig.builder().build();

            final Double score = contextRelevanceMetric.singleTurnScore(config, sample);

            log.info("Вопрос: Что такое машинное обучение и как оно работает?");
            log.info("Оценка: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.7, "Высоко релевантный контекст должен дать высокий балл. Получено: " + score);
        }

        @Test
        @DisplayName("Частично релевантный контекст - ОЖИДАЕТСЯ УМЕРЕННЫЙ БАЛЛ")
        void testPartiallyRelevantContext() {
            log.info("=== Тест: частично релевантный контекст ===");

            final Sample sample = Sample.builder()
                    .userInput("Какова польза зелёного чая для здоровья?")
                    .retrievedContexts(
                            List.of("Чай — один из самых популярных напитков в мире. Он бывает разных видов: чёрный, "
                                    + "зелёный, белый и улун. Каждый вид имеет свой уникальный вкусовой профиль и "
                                    + "производится разными методами обработки. Зелёный чай зародился в Китае и "
                                    + "употребляется уже тысячи лет."))
                    .build();

            final ContextRelevanceMetric.ContextRelevanceConfig config =
                    ContextRelevanceMetric.ContextRelevanceConfig.builder().build();

            final Double score = contextRelevanceMetric.singleTurnScore(config, sample);

            log.info("Вопрос: Какова польза зелёного чая для здоровья?");
            log.info("Оценка: {}", score);

            assertNotNull(score);
            assertTrue(
                    score >= 0.2 && score <= 0.8,
                    "Частично релевантный контекст должен дать умеренный балл. Получено: " + score);
        }

        @Test
        @DisplayName("Нерелевантный контекст - ОЖИДАЕТСЯ НИЗКИЙ БАЛЛ")
        void testIrrelevantContext() {
            log.info("=== Тест: нерелевантный контекст ===");

            final Sample sample = Sample.builder()
                    .userInput("Как испечь шоколадное печенье?")
                    .retrievedContexts(List.of(
                            "Фондовый рынок сегодня испытал значительную волатильность на фоне реакции инвесторов "
                                    + "на новые экономические данные. Основные индексы колебались в течение торговой сессии: "
                                    + "акции технологических компаний лидировали по росту, тогда как энергетический сектор "
                                    + "испытывал давление. Аналитики внимательно следят за предстоящим решением ЦБ."))
                    .build();

            final ContextRelevanceMetric.ContextRelevanceConfig config =
                    ContextRelevanceMetric.ContextRelevanceConfig.builder().build();

            final Double score = contextRelevanceMetric.singleTurnScore(config, sample);

            log.info("Вопрос: Как испечь шоколадное печенье?");
            log.info("Оценка: {}", score);

            assertNotNull(score);
            assertTrue(score <= 0.4, "Нерелевантный контекст должен дать низкий балл. Получено: " + score);
        }
    }

    @Nested
    @DisplayName("Тесты с несколькими контекстами")
    class MultipleContextsTests {

        @Test
        @DisplayName("Контексты смешанной релевантности - ОЖИДАЕТСЯ УМЕРЕННЫЙ БАЛЛ")
        void testMixedRelevanceContexts() {
            log.info("=== Тест: контексты смешанной релевантности ===");

            final Sample sample = Sample.builder()
                    .userInput("Что такое язык программирования Python?")
                    .retrievedContexts(List.of(
                            "Python — высокоуровневый интерпретируемый язык программирования, известный своей "
                                    + "простотой и читаемостью. Он был создан Гвидо ван Россумом и впервые выпущен в 1991 году. "
                                    + "Python поддерживает несколько парадигм программирования, включая процедурное, "
                                    + "объектно-ориентированное и функциональное программирование.",
                            "Прогноз погоды на завтра: переменная облачность с максимальной температурой +24°C. "
                                    + "Вероятность дождя во второй половине дня в западных регионах составляет 20%.",
                            "Синтаксис Python делает акцент на читаемости кода с использованием значимых отступов. "
                                    + "Философия дизайна языка подчёркивает читаемость кода за счёт использования "
                                    + "значимых пробелов."))
                    .build();

            final ContextRelevanceMetric.ContextRelevanceConfig config =
                    ContextRelevanceMetric.ContextRelevanceConfig.builder().build();

            final Double score = contextRelevanceMetric.singleTurnScore(config, sample);

            log.info("Вопрос: Что такое язык программирования Python?");
            log.info("Оценка: {}", score);

            assertNotNull(score);
            // 2 релевантных + 1 нерелевантный контекст должны дать умеренное среднее
            assertTrue(
                    score >= 0.4 && score <= 0.9, "Смешанные контексты должны дать умеренный балл. Получено: " + score);
        }

        @Test
        @DisplayName("Все контексты высоко релевантны - ОЖИДАЕТСЯ ВЫСОКИЙ БАЛЛ")
        void testAllHighlyRelevantContexts() {
            log.info("=== Тест: все контексты высоко релевантны ===");

            final Sample sample = Sample.builder()
                    .userInput("Каковы симптомы дефицита витамина D?")
                    .retrievedContexts(
                            List.of(
                                    "Симптомы дефицита витамина D включают усталость, боль в костях, мышечную слабость "
                                            + "и изменения настроения, включая депрессию. Люди с низким уровнем витамина D "
                                            + "также могут чаще болеть, а раны заживают медленнее.",
                                    "Факторы риска дефицита витамина D включают ограниченное пребывание на солнце, тёмную кожу, "
                                            + "ожирение и возраст старше 65 лет. Некоторые заболевания, такие как целиакия "
                                            + "и воспалительные заболевания кишечника, также могут влиять на усвоение витамина D."))
                    .build();

            final ContextRelevanceMetric.ContextRelevanceConfig config =
                    ContextRelevanceMetric.ContextRelevanceConfig.builder().build();

            final Double score = contextRelevanceMetric.singleTurnScore(config, sample);

            log.info("Вопрос: Каковы симптомы дефицита витамина D?");
            log.info("Оценка: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.6, "Все релевантные контексты должны дать высокий балл. Получено: " + score);
        }
    }

    @Nested
    @DisplayName("Крайние случаи")
    class EdgeCasesTests {

        @Test
        @DisplayName("Вопрос из одного слова с релевантным контекстом")
        void testSingleWordQuestion() {
            log.info("=== Тест: вопрос из одного слова ===");

            final Sample sample = Sample.builder()
                    .userInput("Фотосинтез?")
                    .retrievedContexts(List.of(
                            "Фотосинтез — это процесс, с помощью которого растения преобразуют световую энергию, "
                                    + "обычно от солнца, в химическую энергию, которая впоследствии может быть "
                                    + "высвобождена для обеспечения жизнедеятельности растения. Он происходит "
                                    + "преимущественно в листьях растений и включает преобразование углекислого газа "
                                    + "и воды в глюкозу и кислород."))
                    .build();

            final ContextRelevanceMetric.ContextRelevanceConfig config =
                    ContextRelevanceMetric.ContextRelevanceConfig.builder().build();

            final Double score = contextRelevanceMetric.singleTurnScore(config, sample);

            log.info("Вопрос: Фотосинтез?");
            log.info("Оценка: {}", score);

            assertNotNull(score);
            assertTrue(
                    score >= 0.5,
                    "Вопрос из одного слова с релевантным контекстом должен дать хороший балл. Получено: " + score);
        }

        @Test
        @DisplayName("Технический вопрос с техническим контекстом")
        void testTechnicalContent() {
            log.info("=== Тест: технический контент ===");

            final Sample sample = Sample.builder()
                    .userInput("Как работает трёхстороннее рукопожатие TCP/IP?")
                    .retrievedContexts(
                            List.of(
                                    "Трёхстороннее рукопожатие TCP — это метод, используемый TCP для установления соединения "
                                            + "между клиентом и сервером. Оно включает три шага: сначала клиент отправляет пакет "
                                            + "SYN (synchronize) серверу. Затем сервер отвечает пакетом SYN-ACK (synchronize-acknowledge). "
                                            + "Наконец, клиент отправляет пакет ACK (acknowledge) обратно серверу, устанавливая соединение."))
                    .build();

            final ContextRelevanceMetric.ContextRelevanceConfig config =
                    ContextRelevanceMetric.ContextRelevanceConfig.builder().build();

            final Double score = contextRelevanceMetric.singleTurnScore(config, sample);

            log.info("Вопрос: Как работает трёхстороннее рукопожатие TCP/IP?");
            log.info("Оценка: {}", score);

            assertNotNull(score);
            assertTrue(
                    score >= 0.7,
                    "Технический вопрос с соответствующим техническим контекстом должен дать высокий балл. Получено: "
                            + score);
        }

        @Test
        @DisplayName("Асинхронная оценка работает корректно")
        void testAsyncScoring() {
            log.info("=== Тест: асинхронная оценка ===");

            final Sample sample = Sample.builder()
                    .userInput("Какая столица Франции?")
                    .retrievedContexts(
                            List.of("Париж — столица и крупнейший город Франции. Он расположен в северо-центральной "
                                    + "части Франции вдоль реки Сена. Париж известен своими музеями, архитектурой "
                                    + "и культурным значением."))
                    .build();

            final ContextRelevanceMetric.ContextRelevanceConfig config =
                    ContextRelevanceMetric.ContextRelevanceConfig.builder().build();

            final Double score =
                    contextRelevanceMetric.singleTurnScoreAsync(config, sample).join();

            log.info("Асинхронная оценка: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.6, "Асинхронная оценка должна работать корректно. Получено: " + score);
        }
    }
}
