package ai.qa.solutions.metrics.retrieval.ru;

import static org.junit.jupiter.api.Assertions.*;

import ai.qa.solutions.metrics.retrieval.ContextRecallMetric;
import ai.qa.solutions.sample.Sample;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("Интеграционные тесты метрики Context Recall с русскоязычными примерами")
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".*")
@SpringBootTest(classes = RuContextRecallIntegrationIT.ContextRecallIntegrationTestConfiguration.class)
class RuContextRecallIntegrationIT {

    @Configuration
    public static class ContextRecallIntegrationTestConfiguration {}

    @Autowired
    private ContextRecallMetric contextRecallMetric;

    // ==================== ТЕСТЫ ОСНОВНОЙ ФУНКЦИОНАЛЬНОСТИ ====================

    @Test
    @DisplayName("Context Recall: Идеальный отзыв - вся информация доступна")
    void testContextRecall_PerfectRecall() {
        log.info("=== Тест Context Recall - Идеальный отзыв ===");

        Sample sample = Sample.builder()
                .userInput("Что вы можете рассказать об Альберте Эйнштейне?")
                .reference(
                        "Альберт Эйнштейн родился 14 марта 1879 года. Он был немецким физиком-теоретиком. Он получил Нобелевскую премию по физике в 1921 году.")
                .retrievedContexts(
                        List.of(
                                "Альберт Эйнштейн (14 марта 1879 — 18 апреля 1955) был немецким физиком-теоретиком, который считается одним из величайших ученых всех времен.",
                                "Он получил Нобелевскую премию по физике в 1921 году за свои заслуги в области теоретической физики, особенно за открытие закона фотоэлектрического эффекта.",
                                "Наиболее известен разработкой теории относительности, он также внес важный вклад в квантовую механику."))
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        Double score = contextRecallMetric.singleTurnScore(config, sample);

        // ИСПРАВЛЕНО: правильное логирование и проверка
        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Эталон: {}", sample.getReference());
        log.info("Оценка Context Recall (идеальный отзыв): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.9, "Ожидается очень высокая оценка для идеального отзыва, получен: " + score);
    }

    @Test
    @DisplayName("Граничный случай: Эталон из одного предложения")
    void testContextRecall_SingleSentenceReference() {
        log.info("=== Тест граничного случая - Эталон из одного предложения ===");

        Sample sample = Sample.builder()
                .userInput("Где находится Статуя Свободы?")
                .reference("Статуя Свободы находится в гавани Нью-Йорка.")
                .retrievedContexts(List.of(
                        "Статуя Свободы — неоклассическая скульптура на острове Свободы в гавани Нью-Йорка.",
                        "Это был подарок от Франции Соединенным Штатам в 1886 году."))
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        Double score = contextRecallMetric.singleTurnScore(config, sample);

        log.info("Оценка для эталона из одного предложения: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.8, "Ожидается высокая оценка для одного поддерживаемого предложения, получен: " + score);
    }

    // ==================== СЛОЖНЫЕ СЦЕНАРИИ ====================

    @Test
    @DisplayName("Сложный сценарий: Научная информация с множественными фактами")
    void testContextRecall_ScientificInformation() {
        log.info("=== Тест сложного сценария - Научная информация ===");

        Sample sample = Sample.builder()
                .userInput("Объясните фотосинтез")
                .reference(
                        "Фотосинтез — это процесс, при котором растения преобразуют световую энергию в химическую. Он происходит в хлоропластах. Процесс требует углекислого газа, воды и солнечного света. Кислород выделяется как побочный продукт.")
                .retrievedContexts(List.of(
                        "Фотосинтез — это биологический процесс, при котором растения используют солнечный свет для преобразования углекислого газа и воды в глюкозу.",
                        "Хлоропласты — это органеллы, находящиеся в растительных клетках, где происходит фотосинтез.",
                        "Во время фотосинтеза кислород производится как отходный продукт и выделяется в атмосферу.",
                        "Световая энергия захватывается пигментами хлорофилла в хлоропластах."))
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        Double score = contextRecallMetric.singleTurnScore(config, sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Оценка Context Recall (научная информация): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(
                score >= 0.8, "Ожидается высокая оценка для хорошо поддерживаемых научных фактов, получен: " + score);
    }

    @Test
    @DisplayName("Сложный сценарий: Историческая информация с датами")
    void testContextRecall_HistoricalInformation() {
        log.info("=== Тест сложного сценария - Историческая информация ===");

        Sample sample = Sample.builder()
                .userInput("Расскажите о Великой Отечественной войне")
                .reference(
                        "Великая Отечественная война длилась с 1941 по 1945 год. Она была частью Второй мировой войны. Война началась с нападения Германии на СССР. Война закончилась победой Советского Союза в мае 1945 года.")
                .retrievedContexts(List.of(
                        "Великая Отечественная война — война СССР против нацистской Германии и её союзников в 1941—1945 годах.",
                        "Война началась 22 июня 1941 года с вторжения Германии в Советский Союз.",
                        "Великая Отечественная война была частью более широкой Второй мировой войны.",
                        "Война закончилась 9 мая 1945 года капитуляцией Германии и победой СССР.",
                        "Этот конфликт стал одним из самых разрушительных в истории человечества."))
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        Double score = contextRecallMetric.singleTurnScore(config, sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Оценка Context Recall (историческая информация): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(
                score >= 0.9,
                "Ожидается очень высокая оценка для хорошо документированных исторических фактов, получен: " + score);
    }

    @Test
    @DisplayName("Сложный сценарий: Смешанные релевантные и нерелевантные контексты")
    void testContextRecall_MixedContexts() {
        log.info("=== Тест сложного сценария - Смешанные контексты ===");

        Sample sample = Sample.builder()
                .userInput("Что вызывает изменение климата?")
                .reference(
                        "Изменение климата в первую очередь вызвано человеческой деятельностью. Сжигание ископаемого топлива выделяет парниковые газы. Вырубка лесов снижает поглощение углерода. Промышленные процессы способствуют выбросам.")
                .retrievedContexts(
                        List.of(
                                "Человеческая деятельность, такая как сжигание ископаемого топлива, является основной причиной современного изменения климата.",
                                "Парниковые газы, такие как CO₂, задерживают тепло в атмосфере Земли.",
                                "Сегодня солнечная погода с максимальной температурой 24 градуса.",
                                "Вырубка лесов сокращает способность Земли поглощать углекислый газ из атмосферы.",
                                "Популярные места отдыха включают тропические пляжи и горные курорты.",
                                "Промышленная деятельность выделяет различные загрязнители, включая парниковые газы, в атмосферу."))
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        Double score = contextRecallMetric.singleTurnScore(config, sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Оценка Context Recall (смешанные контексты): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.7, "Ожидается хорошая оценка несмотря на нерелевантные контексты, получен: " + score);
    }

    // ==================== АСИНХРОННЫЕ ТЕСТЫ ====================

    @Test
    @DisplayName("Асинхронная оценка")
    void testContextRecall_AsyncEvaluation() throws Exception {
        log.info("=== Тест асинхронной оценки ===");

        Sample sample = Sample.builder()
                .userInput("Что такое технология блокчейн?")
                .reference(
                        "Блокчейн — это технология распределенного реестра. Она использует криптографическое хеширование. Транзакции записываются в блоки. Каждый блок связан с предыдущим.")
                .retrievedContexts(List.of(
                        "Технология блокчейн — это распределенный реестр, который поддерживает постоянно растущий список записей.",
                        "Каждый блок содержит криптографический хеш предыдущего блока, связывая их вместе.",
                        "Транзакции группируются и записываются в блоки в блокчейне.",
                        "Технология обеспечивает безопасное и прозрачное ведение записей без центрального органа."))
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        long startTime = System.currentTimeMillis();
        CompletableFuture<Double> asyncScore = contextRecallMetric.singleTurnScoreAsync(config, sample);
        Double score = asyncScore.get();
        long endTime = System.currentTimeMillis();

        log.info("Время выполнения асинхронной оценки: {} мс", (endTime - startTime));
        log.info("Результат асинхронной оценки: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(
                score >= 0.7,
                "Ожидается высокая оценка для хорошо поддерживаемых фактов о блокчейне, получен: " + score);
    }

    @Test
    @DisplayName("Параллельная оценка с несколькими образцами")
    void testContextRecall_ParallelEvaluation() {
        log.info("=== Тест параллельной оценки ===");

        Sample sample1 = Sample.builder()
                .userInput("Что такое искусственный интеллект?")
                .reference(
                        "ИИ — это симуляция человеческого интеллекта в машинах. Он включает машинное обучение и обработку естественного языка.")
                .retrievedContexts(
                        List.of(
                                "Искусственный интеллект (ИИ) относится к симуляции человеческого интеллекта в машинах.",
                                "Машинное обучение — это подраздел искусственного интеллекта.",
                                "Обработка естественного языка — это технология ИИ, которая помогает компьютерам понимать человеческий язык."))
                .build();

        Sample sample2 = Sample.builder()
                .userInput("Объясните возобновляемую энергию")
                .reference(
                        "Возобновляемая энергия происходит из природных источников. Солнечная энергия использует солнечный свет. Ветровая энергия использует движение воздуха.")
                .retrievedContexts(List.of(
                        "Возобновляемая энергия — это энергия, которая поступает из природных источников, которые восполняются сами собой.",
                        "Солнечная энергия использует энергию солнца с помощью фотоэлектрических элементов.",
                        "Ветровая энергия генерирует электричество, используя ветер для вращения турбин."))
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        long startTime = System.currentTimeMillis();

        CompletableFuture<Double> score1Future = contextRecallMetric.singleTurnScoreAsync(config, sample1);
        CompletableFuture<Double> score2Future = contextRecallMetric.singleTurnScoreAsync(config, sample2);

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(score1Future, score2Future);
        allFutures.join();
        long endTime = System.currentTimeMillis();

        Double score1 = score1Future.join();
        Double score2 = score2Future.join();

        log.info("Время параллельного выполнения: {} мс", (endTime - startTime));
        log.info("Образец 1 Context Recall: {}", score1);
        log.info("Образец 2 Context Recall: {}", score2);

        assertNotNull(score1);
        assertNotNull(score2);
        assertTrue(score1 >= 0.0 && score1 <= 1.0);
        assertTrue(score2 >= 0.0 && score2 <= 1.0);
        assertTrue(score1 >= 0.6, "Ожидается высокая оценка для образца ИИ");
        assertTrue(score2 >= 0.6, "Ожидается высокая оценка для образца возобновляемой энергии");
    }

    // ==================== ТЕСТЫ ОЦЕНКИ КАЧЕСТВА ====================

    @Test
    @DisplayName("Оценка качества: Краткий эталон")
    void testContextRecall_BriefReference() {
        log.info("=== Тест оценки качества - Краткий эталон ===");

        Sample sample = Sample.builder()
                .userInput("Что такое гравитация?")
                .reference("Гравитация — это сила, которая притягивает объекты друг к другу.")
                .retrievedContexts(List.of(
                        "Гравитация — это фундаментальная сила природы, которая заставляет объекты с массой притягиваться друг к другу.",
                        "Сила гравитации зависит от массы объектов и расстояния между ними.",
                        "Закон всемирного тяготения Ньютона описывает, как работает гравитация."))
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        Double score = contextRecallMetric.singleTurnScore(config, sample);

        log.info("Оценка краткого эталона: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.7, "Ожидается высокая оценка для краткого, но поддерживаемого эталона");
    }

    @Test
    @DisplayName("Оценка качества: Подробный эталон")
    void testContextRecall_DetailedReference() {
        log.info("=== Тест оценки качества - Подробный эталон ===");

        Sample sample = Sample.builder()
                .userInput("Что такое гравитация?")
                .reference(
                        "Гравитация — это фундаментальная сила природы. Она заставляет объекты с массой притягиваться друг к другу. Сила зависит от массы и расстояния. Ньютон описал это своим законом всемирного тяготения. Эйнштейн позже объяснил гравитацию через общую теорию относительности.")
                .retrievedContexts(List.of(
                        "Гравитация — это фундаментальная сила природы, которая заставляет объекты с массой притягиваться друг к другу.",
                        "Сила гравитации зависит от массы объектов и расстояния между ними.",
                        "Закон всемирного тяготения Ньютона описывает, как работает гравитация.",
                        "Теория общей относительности Эйнштейна дает более полное понимание гравитации."))
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        Double score = contextRecallMetric.singleTurnScore(config, sample);

        log.info("Оценка подробного эталона: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.6, "Ожидается хорошая оценка для подробного эталона");
    }

    @Test
    @DisplayName("Оценка качества: Полные контексты")
    void testContextRecall_CompleteContexts() {
        log.info("=== Тест оценки качества - Полные контексты ===");

        String reference =
                "Вода кипит при 100 градусах Цельсия. Это происходит при давлении на уровне моря. Точка кипения изменяется с высотой.";

        Sample sample = Sample.builder()
                .userInput("При какой температуре кипит вода?")
                .reference(reference)
                .retrievedContexts(List.of(
                        "Вода кипит при 100 градусах Цельсия (212 градусов по Фаренгейту) на уровне моря.",
                        "Точка кипения воды зависит от атмосферного давления.",
                        "На больших высотах, где атмосферное давление ниже, вода кипит при более низких температурах."))
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        Double score = contextRecallMetric.singleTurnScore(config, sample);

        log.info("Оценка полных контекстов: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.6, "Ожидается высокая оценка для полных контекстов");
    }

    @Test
    @DisplayName("Оценка качества: Неполные контексты")
    void testContextRecall_IncompleteContexts() {
        log.info("=== Тест оценки качества - Неполные контексты ===");

        String reference =
                "Вода кипит при 100 градусах Цельсия. Это происходит при давлении на уровне моря. Точка кипения изменяется с высотой.";

        Sample sample = Sample.builder()
                .userInput("При какой температуре кипит вода?")
                .reference(reference)
                .retrievedContexts(List.of(
                        "Вода состоит из атомов водорода и кислорода.",
                        "Кипение — это фазовый переход из жидкости в газ."))
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        Double score = contextRecallMetric.singleTurnScore(config, sample);

        log.info("Оценка неполных контекстов: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score <= 0.3, "Ожидается низкая оценка для неполных контекстов");
    }

    @Test
    @DisplayName("Сравнение с Context Precision: Разные метрики для одних данных")
    void testContextRecall_VsPrecisionComparison() {
        log.info("=== Сравнение Context Recall vs Context Precision ===");

        Sample sample = Sample.builder()
                .userInput("Что вызывает землетрясения?")
                .reference(
                        "Землетрясения вызываются движением тектонических плит. Плиты внезапно сдвигаются и высвобождают энергию. Это создает сейсмические волны.")
                .retrievedContexts(List.of(
                        "Тектонические плиты — это большие участки земной коры, которые медленно движутся.",
                        "Когда тектонические плиты сталкиваются или скользят друг мимо друга, они могут вызывать землетрясения.",
                        "Сейсмические волны — это энергия, высвобождающаяся во время землетрясения.",
                        "Лучшее время для посещения Японии — сезон цветения сакуры.",
                        "Геологические процессы формируют поверхность Земли на протяжении миллионов лет."))
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        Double recallScore = contextRecallMetric.singleTurnScore(config, sample);

        log.info("Context Recall Score: {}", recallScore);
        log.info("Этот тест показывает, как Context Recall измеряет полноту информации в найденных контекстах");
        log.info("В отличие от Context Precision, который оценивает релевантность ранжирования,");
        log.info("Context Recall проверяет, можно ли все утверждения в эталоне подтвердить найденными контекстами");

        assertNotNull(recallScore);
        assertTrue(recallScore >= 0.0 && recallScore <= 1.0);
        assertTrue(recallScore >= 0.6, "Ожидается разумная оценка для поддерживаемых утверждений о землетрясениях");
    }

    @Test
    @DisplayName("Качественная оценка: Технические термины и определения")
    void testContextRecall_TechnicalTerms() {
        log.info("=== Тест качественной оценки - Технические термины ===");

        Sample sample = Sample.builder()
                .userInput("Что такое криптография?")
                .reference(
                        "Криптография — это наука о методах обеспечения конфиденциальности. Она использует математические алгоритмы для шифрования данных. Симметричное шифрование использует один ключ. Асимметричное шифрование использует пару ключей.")
                .retrievedContexts(
                        List.of(
                                "Криптография — это область математики и информатики, которая изучает методы обеспечения конфиденциальности, целостности и аутентификации информации.",
                                "Шифрование — это процесс преобразования читаемых данных в нечитаемую форму с использованием математических алгоритмов.",
                                "В симметричной криптографии один и тот же ключ используется для шифрования и расшифрования.",
                                "Асимметричная криптография использует пару ключей: открытый ключ для шифрования и закрытый ключ для расшифрования."))
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        Double score = contextRecallMetric.singleTurnScore(config, sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Оценка Context Recall (технические термины): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(
                score >= 0.9,
                "Ожидается очень высокая оценка для хорошо поддерживаемых технических определений, получен: " + score);
    }

    @Test
    @DisplayName("Производительность: Большой эталонный ответ")
    void testContextRecall_LargeReference() {
        log.info("=== Тест производительности - Большой эталонный ответ ===");

        Sample sample = Sample.builder()
                .userInput("Расскажите подробно о процессе эволюции")
                .reference("Эволюция — это процесс изменения живых организмов с течением времени. "
                        + "Теория эволюции была предложена Чарльзом Дарвином. "
                        + "Естественный отбор является основным механизмом эволюции. "
                        + "Мутации создают генетическую изменчивость в популяциях. "
                        + "Адаптации помогают организмам выживать в окружающей среде. "
                        + "Видообразование происходит когда популяции становятся генетически изолированными. "
                        + "Ископаемые останки предоставляют доказательства эволюции. "
                        + "Сравнительная анатомия показывает родственные связи между видами.")
                .retrievedContexts(List.of(
                        "Эволюция — это научная теория, объясняющая, как виды изменяются с течением времени через процессы наследственности и естественного отбора.",
                        "Чарльз Дарвин в 1859 году опубликовал 'Происхождение видов', где изложил теорию эволюции путем естественного отбора.",
                        "Естественный отбор — это процесс, при котором организмы с благоприятными признаками имеют больше шансов выжить и размножиться.",
                        "Генетические мутации создают изменчивость в популяциях, предоставляя материал для естественного отбора.",
                        "Адаптация — это процесс, посредством которого организмы становятся лучше приспособленными к своей среде обитания.",
                        "Видообразование — это эволюционный процесс образования новых видов из существующих.",
                        "Палеонтология изучает ископаемые останки, которые являются важными доказательствами эволюционных процессов.",
                        "Сравнительная анатомия исследует сходства и различия в строении разных видов животных."))
                .build();

        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        long startTime = System.currentTimeMillis();
        Double score = contextRecallMetric.singleTurnScore(config, sample);
        long endTime = System.currentTimeMillis();

        log.info("Время обработки большого эталона: {} мс", (endTime - startTime));
        log.info("Оценка Context Recall (большой эталон): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(
                score >= 0.8,
                "Ожидается высокая оценка для хорошо поддерживаемого большого эталона, получен: " + score);
        assertTrue((endTime - startTime) < 120000, "Обработка должна завершиться в разумное время");
    }

    @Test
    @DisplayName("Context Recall: Кастомный список моделей - одна модель")
    void testContextRecall_CustomModelList_SingleModel() {
        log.info("=== Context Recall: Тест с кастомным списком моделей - одна модель ===");

        Sample sample = Sample.builder()
                .userInput("Какова скорость света?")
                .reference(
                        "Скорость света в вакууме составляет примерно 299 792 458 метров в секунду. Это часто округляется до 300 000 км/с для простоты.")
                .retrievedContexts(List.of(
                        "Скорость света в вакууме составляет ровно 299 792 458 метров в секунду.",
                        "Свет распространяется со скоростью примерно 300 000 километров в секунду.",
                        "Скорость света является фундаментальной константой в физике."))
                .build();

        ContextRecallMetric.ContextRecallConfig config = ContextRecallMetric.ContextRecallConfig.builder()
                .model("google/gemini-2.5-flash") // Использовать только эту модель
                .build();

        Double score = contextRecallMetric.singleTurnScore(config, sample);
        log.info("Оценка с кастомным списком моделей: {}", score);

        assertTrue(score >= 0.0 && score <= 1.0, "Оценка должна быть в диапазоне 0-1");
        assertTrue(score >= 0.8, "Ожидается высокая оценка для хорошо поддерживаемых фактов");
    }

    @Test
    @DisplayName("Context Recall: Кастомный список моделей - все модели")
    void testContextRecall_CustomModelList_AllModels() {
        log.info("=== Context Recall: Тест с кастомным списком моделей - все модели ===");

        Sample sample = Sample.builder()
                .userInput("Какова скорость света?")
                .reference(
                        "Скорость света в вакууме составляет примерно 299 792 458 метров в секунду. Это часто округляется до 300 000 км/с для простоты.")
                .retrievedContexts(List.of(
                        "Скорость света в вакууме составляет ровно 299 792 458 метров в секунду.",
                        "Свет распространяется со скоростью примерно 300 000 километров в секунду.",
                        "Скорость света является фундаментальной константой в физике."))
                .build();

        // Тест с пустым списком моделей - должны использоваться все доступные модели
        ContextRecallMetric.ContextRecallConfig config =
                ContextRecallMetric.ContextRecallConfig.builder().build();

        Double score = contextRecallMetric.singleTurnScore(config, sample);
        log.info("Оценка со всеми моделями: {}", score);

        assertTrue(score >= 0.0 && score <= 1.0, "Оценка должна быть в диапазоне 0-1");
        assertTrue(score >= 0.6, "Ожидается хорошая оценка для хорошо поддерживаемых фактов");
    }
}
