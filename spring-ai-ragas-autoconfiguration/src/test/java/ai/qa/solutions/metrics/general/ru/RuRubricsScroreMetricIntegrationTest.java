package ai.qa.solutions.metrics.general.ru;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.general.RubricsScoreMetric;
import ai.qa.solutions.sample.Sample;
import java.util.Map;
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
@DisplayName("Интеграционные тесты общих метрик с русскоязычными примерами")
@SpringBootTest(classes = RuRubricsScroreMetricIntegrationTest.GeneralMetricsIntegrationTestConfiguration.class)
class RuRubricsScroreMetricIntegrationTest {

    @Configuration
    public static class GeneralMetricsIntegrationTestConfiguration {}

    @Autowired
    private RubricsScoreMetric rubricsScoreMetric;

    @Test
    @DisplayName("RubricsScore: Позитивный тест - отличное объяснение")
    void testRubricsScorePositive_ExcellentExplanation() {
        log.info("=== RubricsScore: Позитивный тест ===");

        Sample sample = Sample.builder()
                .userInput("Объясните процесс фотосинтеза")
                .response("Фотосинтез — это сложный биохимический процесс, в ходе которого растения "
                        + "преобразуют световую энергию в химическую. Процесс происходит в хлоропластах "
                        + "и включает две основные стадии: световую и темновую фазы. В световой фазе "
                        + "хлорофилл поглощает солнечный свет, расщепляя молекулы воды и выделяя кислород. "
                        + "В темновой фазе (цикл Кальвина) углекислый газ из атмосферы превращается в глюкозу. "
                        + "Общее уравнение: 6CO₂ + 6H₂O + световая энергия → C₆H₁₂O₆ + 6O₂.")
                .reference("Фотосинтез - процесс образования органических веществ из CO₂ и воды "
                        + "с использованием световой энергии.")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubrics(createPhotosynthesisRubrics())
                .build();

        Double score = rubricsScoreMetric.singleTurnScore(config, sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Ответ: {}", sample.getResponse());
        log.info("Оценка по рубрикам: {}", score);

        assertNotNull(score);
        assertTrue(score >= 1.0 && score <= 5.0);
        assertTrue(score >= 4.0, "Ожидается высокая оценка для подробного научного объяснения, получен: " + score);
    }

    @Test
    @DisplayName("RubricsScore: Негативный тест - поверхностное объяснение")
    void testRubricsScoreNegative_SuperficialExplanation() {
        log.info("=== RubricsScore: Негативный тест ===");

        Sample sample = Sample.builder()
                .userInput("Объясните процесс фотосинтеза")
                .response("Фотосинтез это когда растения что-то делают со светом. "
                        + "Они как-то используют солнце для роста.")
                .reference("Фотосинтез - процесс образования органических веществ из CO₂ и воды "
                        + "с использованием световой энергии.")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubrics(createPhotosynthesisRubrics())
                .build();

        Double score = rubricsScoreMetric.singleTurnScore(config, sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Ответ: {}", sample.getResponse());
        log.info("Оценка по рубрикам: {}", score);

        assertNotNull(score);
        assertTrue(score >= 1.0 && score <= 5.0);
        assertTrue(score <= 2.0, "Ожидается низкая оценка для поверхностного объяснения, получен: " + score);
    }

    @Test
    @DisplayName("RubricsScore: Тест оценки эссе")
    void testRubricsScore_EssayEvaluation() {
        log.info("=== RubricsScore: Оценка эссе ===");

        // Хорошее эссе
        Sample goodEssay = Sample.builder()
                .userInput("Напишите эссе о влиянии технологий на общество")
                .response("Технологический прогресс кардинально изменил современное общество. "
                        + "С одной стороны, цифровые технологии обеспечили беспрецедентные возможности "
                        + "для коммуникации, образования и доступа к информации. Интернет объединил мир, "
                        + "позволив людям мгновенно обмениваться идеями независимо от географических границ. "
                        + "С другой стороны, возникли новые вызовы: цифровое неравенство, зависимость от "
                        + "технологий и вопросы приватности. Необходим баланс между инновациями и "
                        + "социальной ответственностью для устойчивого развития.")
                .reference("Эссе о влиянии технологий на общество с примерами и аргументацией")
                .build();

        // Слабое эссе
        Sample weakEssay = Sample.builder()
                .userInput("Напишите эссе о влиянии технологий на общество")
                .response("Технологии хорошие. Есть телефоны и компьютеры. " + "Люди используют интернет. Это удобно.")
                .reference("Эссе о влиянии технологий на общество с примерами и аргументацией")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubrics(createEssayRubrics())
                .build();

        Double goodScore = rubricsScoreMetric.singleTurnScore(config, goodEssay);
        Double weakScore = rubricsScoreMetric.singleTurnScore(config, weakEssay);

        log.info("Хорошее эссе - оценка: {}", goodScore);
        log.info("Слабое эссе - оценка: {}", weakScore);

        assertTrue(goodScore >= 3.0, "Хорошее эссе должно получить высокую оценку");
        assertTrue(weakScore <= 2.0, "Слабое эссе должно получить низкую оценку");
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================
    private Map<String, String> createPhotosynthesisRubrics() {
        return Map.of(
                "score1_description", "Полностью неверная или нерелевантная информация о фотосинтезе",
                "score2_description", "Базовое понимание с существенными пробелами или ошибками",
                "score3_description", "Общее понимание процесса, но отсутствуют важные детали",
                "score4_description", "Хорошее понимание с упоминанием основных этапов и компонентов",
                "score5_description", "Отличное объяснение с научными деталями, уравнением и примерами");
    }

    private Map<String, String> createEssayRubrics() {
        return Map.of(
                "score1_description", "Отсутствует структура, нет аргументов, множество ошибок",
                "score2_description", "Слабая структура, поверхностные аргументы, есть ошибки",
                "score3_description", "Базовая структура, некоторые аргументы, в целом понятно",
                "score4_description", "Хорошая структура, убедительные аргументы, качественное изложение",
                "score5_description", "Отличная структура, глубокий анализ, примеры, безупречное изложение");
    }
}
