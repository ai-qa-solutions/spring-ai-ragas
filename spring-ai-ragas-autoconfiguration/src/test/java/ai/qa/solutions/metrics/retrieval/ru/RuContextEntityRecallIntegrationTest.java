package ai.qa.solutions.metrics.retrieval.ru;

import static org.junit.jupiter.api.Assertions.*;

import ai.qa.solutions.metrics.retrieval.ContextEntityRecallMetric;
import ai.qa.solutions.sample.Sample;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("Интеграционные тесты метрики Context Entity Recall с русскоязычными примерами")
@SpringBootTest(classes = RuContextEntityRecallIntegrationTest.ContextEntityRecallIntegrationTestConfiguration.class)
class RuContextEntityRecallIntegrationTest {

    @Configuration
    public static class ContextEntityRecallIntegrationTestConfiguration {}

    @Autowired
    private ContextEntityRecallMetric contextEntityRecallMetric;

    // ==================== ТЕСТЫ ОСНОВНОЙ ФУНКЦИОНАЛЬНОСТИ ====================

    @Test
    @DisplayName("Context Entity Recall: Идеальный отзыв сущностей - все сущности покрыты")
    void testContextEntityRecall_PerfectRecall() {
        log.info("=== Тест Context Entity Recall - Идеальный отзыв ===");

        Sample sample = Sample.builder()
                .reference(
                        "Эйфелева башня находится в Париже, Франция. Она была завершена в 1889 году для Всемирной выставки.")
                .retrievedContexts(
                        List.of(
                                "Эйфелева башня, расположенная в Париже, Франция, является одной из самых знаковых достопримечательностей мира.",
                                "Завершенная в 1889 году, она была построена к Всемирной выставке 1889 года.",
                                "Миллионы посетителей приезжают к ней каждый год, чтобы насладиться захватывающими видами на город."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Эталон: {}", sample.getReference());
        log.info("Оценка Context Entity Recall (идеальный отзыв): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.7, "Ожидается высокая оценка для идеального покрытия сущностей, получен: " + score);
    }

    @Test
    @DisplayName("Context Entity Recall: Частичное наличие сущностей - некоторые сущности отсутствуют")
    void testContextEntityRecall_PartialRecall() {
        log.info("=== Тест Context Entity Recall - Частичный отзыв ===");

        Sample sample = Sample.builder()
                .reference(
                        "Тадж-Махал находится в Агре, Индия. Он был построен Шах-Джаханом в 1631 году для его жены Мумтаз-Махал.")
                .retrievedContexts(List.of(
                        "Тадж-Махал — прекрасный памятник в Индии.",
                        "Он был построен могольским императором Шах-Джаханом.",
                        "Строение выполнено из белого мрамора и привлекает миллионы туристов."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Эталон: {}", sample.getReference());
        log.info("Оценка Context Entity Recall (частичный отзыв): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(
                score >= 0.2 && score <= 0.8,
                "Ожидается умеренная оценка для частичного покрытия сущностей, получен: " + score);
    }

    @Test
    @DisplayName("Context Entity Recall: Плохой отзыв сущностей - большинство сущностей отсутствует")
    void testContextEntityRecall_PoorRecall() {
        log.info("=== Тест Context Entity Recall - Плохой отзыв ===");

        Sample sample = Sample.builder()
                .reference(
                        "Альберт Эйнштейн родился в Ульме, Германия, 14 марта 1879 года. Он получил Нобелевскую премию по физике в 1921 году.")
                .retrievedContexts(List.of(
                        "Физика — фундаментальная наука, изучающая материю и энергию.",
                        "Ученые сделали много важных открытий на протяжении истории.",
                        "Награды и премии признают выдающиеся достижения в различных областях."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Эталон: {}", sample.getReference());
        log.info("Оценка Context Entity Recall (плохой отзыв): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score <= 0.3, "Ожидается низкая оценка для плохого покрытия сущностей, получен: " + score);
    }

    // ==================== ГРАНИЧНЫЕ СЛУЧАИ ====================

    @Test
    @DisplayName("Граничный случай: Пустые найденные контексты")
    void testContextEntityRecall_EmptyContexts() {
        log.info("=== Тест граничного случая - Пустые найденные контексты ===");

        Sample sample = Sample.builder()
                .reference("Статуя Свободы находится в гавани Нью-Йорка.")
                .retrievedContexts(List.of())
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Оценка для пустых контекстов: {}", score);

        assertNotNull(score);
        assertEquals(0.0, score, "Ожидается 0.0 для пустых найденных контекстов, получен: " + score);
    }

    @Test
    @DisplayName("Граничный случай: Пустой эталон")
    void testContextEntityRecall_EmptyReference() {
        log.info("=== Тест граничного случая - Пустой эталон ===");

        Sample sample = Sample.builder()
                .reference("")
                .retrievedContexts(
                        List.of("Париж — столица Франции.", "Эйфелева башня — знаменитая достопримечательность."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Оценка для пустого эталона: {}", score);

        assertNotNull(score);
        assertEquals(0.0, score, "Ожидается 0.0 для пустого эталона, получен: " + score);
    }

    @Test
    @DisplayName("Граничный случай: Отсутствие сущностей в эталоне")
    void testContextEntityRecall_NoEntitiesInReference() {
        log.info("=== Тест граничного случая - Отсутствие сущностей в эталоне ===");

        Sample sample = Sample.builder()
                .reference("Это очень важно и интересно.")
                .retrievedContexts(List.of(
                        "Эйфелева башня находится в Париже, Франция.",
                        "Наполеон Бонапарт был французским военным лидером."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Оценка для эталона без сущностей: {}", score);

        assertNotNull(score);
        assertEquals(0.0, score, "Ожидается 0.0 когда в эталоне нет сущностей, получен: " + score);
    }

    // ==================== СЛОЖНЫЕ СЦЕНАРИИ ====================

    @Test
    @DisplayName("Сложный сценарий: Исторические сущности и даты")
    void testContextEntityRecall_HistoricalEntities() {
        log.info("=== Тест сложного сценария - Исторические сущности ===");

        Sample sample = Sample.builder()
                .reference(
                        "Великая Отечественная война длилась с 1941 по 1945 год. Адольф Гитлер возглавлял нацистскую Германию, а Иосиф Сталин руководил Советским Союзом.")
                .retrievedContexts(List.of(
                        "Великая Отечественная война была частью Второй мировой войны и длилась с 1941 по 1945 год.",
                        "Адольф Гитлер был лидером нацистской Германии во время войны.",
                        "Иосиф Сталин руководил Советским Союзом в период войны.",
                        "Война привела к значительным потерям и изменила ход истории."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Эталон: {}", sample.getReference());
        log.info("Оценка Context Entity Recall (исторические сущности): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(
                score >= 0.7, "Ожидается высокая оценка для хорошо покрытых исторических сущностей, получен: " + score);
    }

    @Test
    @DisplayName("Сложный сценарий: Географические сущности")
    void testContextEntityRecall_GeographicEntities() {
        log.info("=== Тест сложного сценария - Географические сущности ===");

        Sample sample = Sample.builder()
                .reference(
                        "Река Волга протекает через Россию, Казахстан и впадает в Каспийское море. Её длина составляет приблизительно 3530 километров.")
                .retrievedContexts(List.of(
                        "Река Волга — самая длинная река в Европе.",
                        "Она протекает через территорию России и частично Казахстана.",
                        "Волга впадает в Каспийское море в дельте.",
                        "Длина реки составляет около 3530 километров."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Эталон: {}", sample.getReference());
        log.info("Оценка Context Entity Recall (географические сущности): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.4, "Ожидается нормальная оценка для географических сущностей, получен: " + score);
    }

    @Test
    @DisplayName("Сложный сценарий: Научные сущности и измерения")
    void testContextEntityRecall_ScientificEntities() {
        log.info("=== Тест сложного сценария - Научные сущности ===");

        Sample sample = Sample.builder()
                .reference(
                        "Скорость света составляет 299 792 458 метров в секунду. Альберт Эйнштейн открыл это в своей теории относительности в 1905 году.")
                .retrievedContexts(List.of(
                        "Скорость света в вакууме составляет приблизительно 299 792 458 метров в секунду.",
                        "Альберт Эйнштейн разработал теорию относительности.",
                        "Эйнштейн опубликовал свою специальную теорию относительности в 1905 году.",
                        "Это открытие революционизировало наше понимание пространства и времени."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Эталон: {}", sample.getReference());
        log.info("Оценка Context Entity Recall (научные сущности): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(
                score >= 0.2, "Ожидается нормальная оценка для хорошо покрытых научных сущностей, получен: " + score);
    }

    @Test
    @DisplayName("Сложный сценарий: Смешанные релевантные и нерелевантные контексты")
    void testContextEntityRecall_MixedContexts() {
        log.info("=== Тест сложного сценария - Смешанные контексты ===");

        Sample sample = Sample.builder()
                .reference(
                        "Миссия Аполлон-11 стартовала 16 июля 1969 года. Нил Армстронг и Базз Олдрин высадились на Луне.")
                .retrievedContexts(List.of(
                        "Миссия Аполлон-11 была исторической космической миссией.",
                        "Нил Армстронг был первым человеком, ступившим на Луну.",
                        "Сегодня солнечная и приятная погода.",
                        "Базз Олдрин сопровождал Армстронга на лунной поверхности.",
                        "Многим людям нравится смотреть фильмы о космических исследованиях.",
                        "Миссия стартовала 16 июля 1969 года с космического центра Кеннеди."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Эталон: {}", sample.getReference());
        log.info("Оценка Context Entity Recall (смешанные контексты): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.7, "Ожидается хорошая оценка несмотря на нерелевантные контексты, получен: " + score);
    }

    // ==================== АСИНХРОННЫЕ ТЕСТЫ ====================

    @Test
    @DisplayName("Асинхронная оценка")
    void testContextEntityRecall_AsyncEvaluation() throws Exception {
        log.info("=== Тест асинхронной оценки ===");

        Sample sample = Sample.builder()
                .reference(
                        "Великая Китайская стена протянулась на 21 196 километров. Она была построена во времена династии Мин.")
                .retrievedContexts(List.of(
                        "Великая Китайская стена — одна из самых известных достопримечательностей в мире.",
                        "Она протянулась на 21 196 километров через северный Китай.",
                        "Большая часть стены, которая существует сегодня, была построена во времена династии Мин.",
                        "Она была построена для защиты от вторжений с севера."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        long startTime = System.currentTimeMillis();
        CompletableFuture<Double> asyncScore = contextEntityRecallMetric.singleTurnScoreAsync(config, sample);
        Double score = asyncScore.get();
        long endTime = System.currentTimeMillis();

        log.info("Время выполнения асинхронной оценки: {} мс", (endTime - startTime));
        log.info("Результат асинхронной оценки: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.6, "Ожидается высокая оценка для хорошо покрытых сущностей, получен: " + score);
    }

    @Test
    @DisplayName("Параллельная оценка с несколькими образцами")
    void testContextEntityRecall_ParallelEvaluation() {
        log.info("=== Тест параллельной оценки ===");

        Sample sample1 = Sample.builder()
                .reference("Лувр находится в Париже, Франция. В нем хранится Мона Лиза, написанная Леонардо да Винчи.")
                .retrievedContexts(List.of(
                        "Лувр — один из крупнейших художественных музеев в мире.",
                        "Он находится в Париже, столице Франции.",
                        "В музее хранится знаменитая картина Мона Лиза.",
                        "Леонардо да Винчи создал этот шедевр в начале 16 века."))
                .build();

        Sample sample2 = Sample.builder()
                .reference("Эверест находится в Гималаях между Непалом и Тибетом. Его высота составляет 8848 метров.")
                .retrievedContexts(List.of(
                        "Эверест — самая высокая гора в мире.",
                        "Она находится в Гималаях на границе между Непалом и Тибетом.",
                        "Гора достигает высоты 8848 метров над уровнем моря.",
                        "Многие альпинисты пытаются покорить её вершину каждый год."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        long startTime = System.currentTimeMillis();

        CompletableFuture<Double> score1Future = contextEntityRecallMetric.singleTurnScoreAsync(config, sample1);
        CompletableFuture<Double> score2Future = contextEntityRecallMetric.singleTurnScoreAsync(config, sample2);

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(score1Future, score2Future);
        allFutures.join();
        long endTime = System.currentTimeMillis();

        Double score1 = score1Future.join();
        Double score2 = score2Future.join();

        log.info("Время параллельного выполнения: {} мс", (endTime - startTime));
        log.info("Образец 1 Context Entity Recall: {}", score1);
        log.info("Образец 2 Context Entity Recall: {}", score2);

        assertNotNull(score1);
        assertNotNull(score2);
        assertTrue(score1 >= 0.0 && score1 <= 1.0);
        assertTrue(score2 >= 0.0 && score2 <= 1.0);
        assertTrue(score1 >= 0.8, "Ожидается высокая оценка для образца Лувра");
        assertTrue(score2 >= 0.8, "Ожидается высокая оценка для образца Эвереста");
    }

    // ==================== ОЦЕНКА ПОКРЫТИЯ СУЩНОСТЕЙ ====================

    @Test
    @DisplayName("Покрытие сущностей: Туристический случай использования")
    void testContextEntityRecall_TourismUseCase() {
        log.info("=== Тест покрытия сущностей - Туристический случай ===");

        Sample sample = Sample.builder()
                .reference(
                        "Посетите Колизей в Риме, Италия. Он был построен императором Веспасианом в 70 году н.э. и завершен Титом в 80 году н.э.")
                .retrievedContexts(List.of(
                        "Колизей — древний амфитеатр в Риме, столице Италии.",
                        "Строительство Колизея началось при императоре Веспасиане около 70 года н.э.",
                        "Строение было завершено во время правления его сына Тита в 80 году н.э.",
                        "Он мог вместить от 50 000 до 80 000 зрителей для гладиаторских игр."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Эталон: {}", sample.getReference());
        log.info("Оценка Context Entity Recall (туристический случай): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(
                score >= 0.24, "Ожидается нормальная оценка для покрытия туристических сущностей, получен: " + score);
    }

    @Test
    @DisplayName("Покрытие сущностей: Исторический QA случай использования")
    void testContextEntityRecall_HistoricalQAUseCase() {
        log.info("=== Тест покрытия сущностей - Исторический QA случай ===");

        Sample sample = Sample.builder()
                .reference(
                        "Бородинское сражение произошло 7 сентября 1812 года. Наполеон Бонапарт сражался против Михаила Кутузова. Битва стала поворотным моментом Отечественной войны 1812 года.")
                .retrievedContexts(List.of(
                        "Бородинское сражение состоялось 7 сентября 1812 года.",
                        "Наполеон Бонапарт командовал французской армией в этой битве.",
                        "Михаил Кутузов был главнокомандующим русской армией.",
                        "Эта битва стала ключевым событием Отечественной войны 1812 года."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Эталон: {}", sample.getReference());
        log.info("Оценка Context Entity Recall (исторический QA): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.8, "Ожидается высокая оценка для покрытия исторических сущностей, получен: " + score);
    }

    @Test
    @DisplayName("Покрытие сущностей: Различные типы сущностей")
    void testContextEntityRecall_DifferentEntityTypes() {
        log.info("=== Тест покрытия сущностей - Различные типы сущностей ===");

        Sample sample = Sample.builder()
                .reference(
                        "Роскосмос запустил телескоп Хаббл 24 апреля 1990 года. Он вращается вокруг Земли на высоте 547 километров и сделал более 1,5 миллиона наблюдений.")
                .retrievedContexts(List.of(
                        "Роскосмос, российское космическое агентство, участвовал в космических программах.",
                        "Телескоп Хаббл был запущен 24 апреля 1990 года на борту космического шаттла.",
                        "Хаббл вращается вокруг Земли на высоте приблизительно 547 километров.",
                        "Телескоп сделал более 1,5 миллиона научных наблюдений с момента развертывания."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double score = contextEntityRecallMetric.singleTurnScore(config, sample);

        log.info("Эталон: {}", sample.getReference());
        log.info("Оценка Context Entity Recall (различные типы сущностей): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.5, "Ожидается хорошая оценка для покрытия различных типов сущностей, получен: " + score);
    }

    @Test
    @DisplayName("Сравнение эффективности: Высокое vs низкое покрытие сущностей")
    void testContextEntityRecall_HighVsLowCoverage() {
        log.info("=== Тест сравнения эффективности - Высокое vs низкое покрытие ===");

        String reference =
                "Петр I был российским императором с 1682 по 1725 год. Он основал Санкт-Петербург в 1703 году.";

        // Высокое покрытие сущностей
        Sample highCoverageSample = Sample.builder()
                .reference(reference)
                .retrievedContexts(List.of(
                        "Петр I, также известный как Петр Великий, был российским императором.",
                        "Он правил Россией с 1682 по 1725 год.",
                        "Петр I основал город Санкт-Петербург в 1703 году.",
                        "Санкт-Петербург стал новой столицей Российской империи."))
                .build();

        // Низкое покрытие сущностей
        Sample lowCoverageSample = Sample.builder()
                .reference(reference)
                .retrievedContexts(List.of(
                        "Российская империя была могущественным государством.",
                        "Императоры играли важную роль в истории России.",
                        "Многие города были основаны в период империи."))
                .build();

        ContextEntityRecallMetric.ContextEntityRecallConfig config =
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

        Double highScore = contextEntityRecallMetric.singleTurnScore(config, highCoverageSample);
        Double lowScore = contextEntityRecallMetric.singleTurnScore(config, lowCoverageSample);

        log.info("Оценка высокого покрытия: {}", highScore);
        log.info("Оценка низкого покрытия: {}", lowScore);

        assertNotNull(highScore);
        assertNotNull(lowScore);
        assertTrue(highScore >= 0.0 && highScore <= 1.0);
        assertTrue(lowScore >= 0.0 && lowScore <= 1.0);

        assertTrue(highScore >= 0.8, "Ожидается высокая оценка для хорошего покрытия сущностей");
        assertTrue(lowScore <= 0.3, "Ожидается низкая оценка для плохого покрытия сущностей");
        assertTrue(highScore > lowScore, "Высокое покрытие должно получать более высокую оценку, чем низкое");

        log.info("Результат показывает, что механизм поиска с высоким покрытием сущностей ({}) лучше");
        log.info("механизма с низким покрытием ({}) для случаев, где важны сущности", highScore, lowScore);
    }
}
