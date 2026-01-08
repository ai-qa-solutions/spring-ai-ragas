package ai.qa.solutions.allure.i18n;

import java.util.Map;
import lombok.Getter;

/**
 * Localized messages for score explanation components.
 * <p>
 * Provides translations for all UI strings in explanation blocks.
 * Supports English (en) and Russian (ru) languages.
 */
public final class ExplanationMessages {

    private static final String DEFAULT_LANGUAGE = "en";

    @Getter
    private final String language;

    private final Map<String, String> messages;

    /**
     * Creates an ExplanationMessages instance for the specified language.
     *
     * @param language language code (en, ru)
     */
    public ExplanationMessages(final String language) {
        this.language = language != null ? language : DEFAULT_LANGUAGE;
        this.messages = "ru".equalsIgnoreCase(this.language) ? russianMessages() : englishMessages();
    }

    /**
     * Gets a localized message by key.
     *
     * @param key message key
     * @return localized string, or key if not found
     */
    public String get(final String key) {
        return messages.getOrDefault(key, key);
    }

    /**
     * Gets a localized message with placeholder substitution.
     *
     * @param key message key
     * @param args arguments to substitute
     * @return localized string with substitutions
     */
    public String get(final String key, final Object... args) {
        final String template = messages.getOrDefault(key, key);
        return String.format(template, args);
    }

    /**
     * Checks if the current language is Russian.
     *
     * @return true if language is "ru"
     */
    public boolean isRussian() {
        return "ru".equalsIgnoreCase(language);
    }

    private static Map<String, String> englishMessages() {
        return Map.ofEntries(
                // Common
                Map.entry("common.na", "N/A"),
                Map.entry("common.unknown", "Unknown"),
                Map.entry("common.scoreNotCalculated", "Score not calculated"),
                Map.entry("common.score", "score"),
                Map.entry("common.level", "level"),

                // Scale levels (standard)
                Map.entry("scale.excellent", "Excellent"),
                Map.entry("scale.good", "Good"),
                Map.entry("scale.moderate", "Moderate"),
                Map.entry("scale.poor", "Poor"),
                Map.entry("scale.excellent.desc", "Very high score"),
                Map.entry("scale.good.desc", "Good score"),
                Map.entry("scale.moderate.desc", "Moderate score"),
                Map.entry("scale.poor.desc", "Low score"),

                // Verdicts
                Map.entry("verdict.faithful", "FAITHFUL"),
                Map.entry("verdict.unfaithful", "UNFAITHFUL"),
                Map.entry("verdict.relevant", "Relevant"),
                Map.entry("verdict.notRelevant", "Not relevant"),
                Map.entry("verdict.found", "Found"),
                Map.entry("verdict.missing", "Missing"),
                Map.entry("verdict.pass", "PASS"),
                Map.entry("verdict.fail", "FAIL"),
                Map.entry("verdict.ok", "OK"),
                Map.entry("verdict.error", "ERROR"),

                // Step titles - common
                Map.entry("step.computeScore", "Calculating final score"),

                // Faithfulness
                Map.entry(
                        "faithfulness.description",
                        "This metric checks: did the AI make up anything? "
                                + "We break down the response into individual facts and verify each against the context."),
                Map.entry("faithfulness.step1.title", "Breaking down the response"),
                Map.entry(
                        "faithfulness.step1.desc",
                        "We took the AI response and broke it down into individual facts (statements)."),
                Map.entry("faithfulness.step1.output", "Extracted %d statements"),
                Map.entry("faithfulness.step2.title", "Verifying statements"),
                Map.entry(
                        "faithfulness.step2.desc",
                        "For each statement, we checked: can it be found in the provided context?"),
                Map.entry("faithfulness.step2.output", "%d of %d statements verified"),
                Map.entry("faithfulness.step3.title", "Calculating final score"),
                Map.entry("faithfulness.step3.desc", "Divide the number of verified statements by the total count."),
                Map.entry("faithfulness.formula", "avg((verified statements) / (total statements) per model)"),
                Map.entry("faithfulness.meaning.excellent", "Excellent - AI relies only on facts from the context"),
                Map.entry("faithfulness.meaning.good", "Good - most statements are supported by the context"),
                Map.entry("faithfulness.meaning.moderate", "Moderate - some information is not supported by context"),
                Map.entry("faithfulness.meaning.poor", "Poor - many hallucinations (made-up facts)"),
                Map.entry("faithfulness.scale.excellent", "All statements verified by context"),
                Map.entry("faithfulness.scale.good", "Most statements verified"),
                Map.entry("faithfulness.scale.moderate", "Some statements not verified"),
                Map.entry("faithfulness.scale.poor", "Many hallucinations"),

                // AspectCritic
                Map.entry(
                        "aspectCritic.description",
                        "Binary check: does the response meet the specified criteria? Result: PASS or FAIL."),
                Map.entry("aspectCritic.step1.title", "User-defined aspect"),
                Map.entry("aspectCritic.step1.desc", "The criteria defined by the user for evaluation."),
                Map.entry("aspectCritic.step2.title", "Aspect evaluation"),
                Map.entry("aspectCritic.step2.desc", "LLM analyzed the response for criteria compliance."),
                Map.entry("aspectCritic.step3.title", "Score calculation"),
                Map.entry("aspectCritic.step3.desc", "PASS → 1.0, FAIL → 0.0"),
                Map.entry("aspectCritic.meaning.pass", "Criteria \"%s\" PASSED"),
                Map.entry("aspectCritic.meaning.fail", "Criteria \"%s\" FAILED"),
                Map.entry("aspectCritic.scale.pass", "Criteria met"),
                Map.entry("aspectCritic.scale.fail", "Criteria not met"),
                Map.entry("aspectCritic.defaultAspect", "Custom Aspect"),

                // ContextPrecision
                Map.entry(
                        "contextPrecision.description",
                        "This metric checks: are contexts ranked correctly? "
                                + "Relevant contexts should appear first in the list."),
                Map.entry("contextPrecision.step1.title", "Evaluating context relevance"),
                Map.entry(
                        "contextPrecision.step1.desc",
                        "For each context, we check: is it relevant for answering the question?"),
                Map.entry("contextPrecision.step1.output", "%d of %d are relevant"),
                Map.entry("contextPrecision.step2.title", "Calculating Precision@K"),
                Map.entry("contextPrecision.step2.desc", "Precision@K = (relevant in top-K) / K"),
                Map.entry("contextPrecision.step3.title", "Calculating Average Precision"),
                Map.entry(
                        "contextPrecision.step3.desc",
                        "AP = average Precision@K only at positions with relevant contexts"),
                Map.entry("contextPrecision.formula", "AP = Σ(Precision@k × relevance@k) / (relevant count)"),
                Map.entry("contextPrecision.meaning.excellent", "Excellent - relevant contexts appear first"),
                Map.entry("contextPrecision.meaning.good", "Good - most relevant contexts are at the top"),
                Map.entry("contextPrecision.meaning.moderate", "Moderate - ranking needs improvement"),
                Map.entry("contextPrecision.meaning.poor", "Poor - relevant contexts are lost among irrelevant ones"),

                // ContextRecall
                Map.entry(
                        "contextRecall.description",
                        "This metric checks: do contexts cover all information from the reference? "
                                + "Each sentence from reference should be found in contexts."),
                Map.entry("contextRecall.step1.title", "Checking reference coverage by contexts"),
                Map.entry(
                        "contextRecall.step1.desc",
                        "For each sentence from the reference answer, we check: "
                                + "can it be found in the provided contexts?"),
                Map.entry("contextRecall.step1.output", "%d of %d found in contexts"),
                Map.entry("contextRecall.step2.title", "Calculating coverage"),
                Map.entry("contextRecall.step2.desc", "Proportion of reference sentences found in contexts."),
                Map.entry("contextRecall.formula", "avg((found sentences) / (total sentences) per model)"),
                Map.entry("contextRecall.meaning.excellent", "Excellent - contexts fully cover the reference answer"),
                Map.entry("contextRecall.meaning.good", "Good - most of the reference is found in contexts"),
                Map.entry("contextRecall.meaning.moderate", "Moderate - some information is missing from contexts"),
                Map.entry("contextRecall.meaning.poor", "Poor - contexts don't contain significant parts of reference"),
                Map.entry("contextRecall.scale.excellent", "Full reference coverage"),
                Map.entry("contextRecall.scale.good", "Good coverage"),
                Map.entry("contextRecall.scale.moderate", "Partial coverage"),
                Map.entry("contextRecall.scale.poor", "Insufficient coverage"),

                // ContextEntityRecall
                Map.entry(
                        "contextEntityRecall.description",
                        "This metric checks: do contexts contain all important entities from reference? "
                                + "Entities are names, dates, places, organizations, etc."),
                Map.entry("contextEntityRecall.step1.title", "Extracting entities from reference"),
                Map.entry(
                        "contextEntityRecall.step1.desc",
                        "Finding all important entities in reference: names, dates, places, etc."),
                Map.entry("contextEntityRecall.step1.output", "Found %d entities"),
                Map.entry("contextEntityRecall.step2.title", "Extracting entities from contexts"),
                Map.entry("contextEntityRecall.step2.desc", "Finding all entities in provided contexts."),
                Map.entry("contextEntityRecall.step3.title", "Comparing entities"),
                Map.entry("contextEntityRecall.step3.desc", "Which entities from reference are found in contexts?"),
                Map.entry("contextEntityRecall.step3.output", "%d of %d found"),
                Map.entry("contextEntityRecall.step4.title", "Calculating entity coverage"),
                Map.entry("contextEntityRecall.step4.desc", "Proportion of reference entities found in contexts."),
                Map.entry("contextEntityRecall.formula", "avg((found entities) / (total entities) per model)"),
                Map.entry(
                        "contextEntityRecall.meaning.excellent", "Excellent - contexts contain all important entities"),
                Map.entry("contextEntityRecall.meaning.good", "Good - most entities are present"),
                Map.entry("contextEntityRecall.meaning.moderate", "Moderate - some entities are missing"),
                Map.entry(
                        "contextEntityRecall.meaning.poor", "Poor - many important entities are missing from contexts"),

                // NoiseSensitivity
                Map.entry(
                        "noiseSensitivity.description",
                        "This metric checks: does irrelevant context influence the AI response? "
                                + "Lower values are better - the system ignores noisy context."),
                Map.entry("noiseSensitivity.step1.title", "Breaking down reference into statements"),
                Map.entry("noiseSensitivity.step1.desc", "The reference answer is broken down into individual facts."),
                Map.entry("noiseSensitivity.step1.output", "Reference statements"),
                Map.entry("noiseSensitivity.step2.title", "Breaking down AI response into statements"),
                Map.entry("noiseSensitivity.step2.desc", "The AI response is broken down into individual facts."),
                Map.entry("noiseSensitivity.step2.output", "Response statements"),
                Map.entry("noiseSensitivity.step3.title", "Analyzing matches and sources"),
                Map.entry(
                        "noiseSensitivity.step3.desc",
                        "Checking if response statements match reference and where information came from."),
                Map.entry("noiseSensitivity.step3.output", "%d errors out of %d checks"),
                Map.entry("noiseSensitivity.step4.title", "Calculating noise sensitivity"),
                Map.entry(
                        "noiseSensitivity.step4.desc",
                        "Proportion of errors caused by irrelevant context. Lower = better."),
                Map.entry("noiseSensitivity.formula", "avg((noise errors / total checks) per model)"),
                Map.entry(
                        "noiseSensitivity.meaning.excellent",
                        "EXCELLENT - System completely ignores irrelevant context"),
                Map.entry("noiseSensitivity.meaning.good", "GOOD - Minimal influence from irrelevant context"),
                Map.entry(
                        "noiseSensitivity.meaning.moderate", "MODERATE - Noticeable influence from irrelevant context"),
                Map.entry(
                        "noiseSensitivity.meaning.poor",
                        "POOR - Response is heavily distorted by irrelevant information"),
                Map.entry("noiseSensitivity.scale.excellent", "No noise influence (EXCELLENT)"),
                Map.entry("noiseSensitivity.scale.good", "Minimal influence"),
                Map.entry("noiseSensitivity.scale.moderate", "Noticeable influence"),
                Map.entry("noiseSensitivity.scale.poor", "Heavy response distortion"),

                // ResponseRelevancy
                Map.entry(
                        "responseRelevancy.description",
                        "This metric checks: does the AI response actually answer the user's question? "
                                + "We generate questions the response could answer and compare to the original."),
                Map.entry("responseRelevancy.step1.title", "Original user question"),
                Map.entry("responseRelevancy.step1.desc", "The question that the AI response should answer."),
                Map.entry("responseRelevancy.step2.title", "Generating questions from response"),
                Map.entry(
                        "responseRelevancy.step2.desc",
                        "LLM generates questions that the given response could answer."),
                Map.entry("responseRelevancy.step2.output", "Generated questions"),
                Map.entry("responseRelevancy.step3.title", "Calculating semantic similarity"),
                Map.entry(
                        "responseRelevancy.step3.desc",
                        "Comparing generated questions with the original via embeddings. "
                                + "The more similar the questions - the more relevant the response."),
                Map.entry("responseRelevancy.step4.title", "Calculating average similarity"),
                Map.entry(
                        "responseRelevancy.step4.desc", "Final score = average similarity of all generated questions."),
                Map.entry("responseRelevancy.formula", "mean(cosine_similarity(original, generated))"),
                Map.entry("responseRelevancy.meaning.excellent", "Excellent - response directly answers the question"),
                Map.entry("responseRelevancy.meaning.good", "Good - response is mostly relevant to the question"),
                Map.entry("responseRelevancy.meaning.moderate", "Moderate - response partially answers the question"),
                Map.entry("responseRelevancy.meaning.poor", "Poor - response is off-topic"),
                Map.entry("responseRelevancy.scale.excellent", "Response directly answers the question"),
                Map.entry("responseRelevancy.scale.good", "Response is mostly relevant"),
                Map.entry("responseRelevancy.scale.moderate", "Response partially answers"),
                Map.entry("responseRelevancy.scale.poor", "Response is off-topic"),

                // SimpleCriteria
                Map.entry(
                        "simpleCriteria.description",
                        "Evaluation by user-defined criteria on a continuous scale. "
                                + "Interpretation depends on your criteria requirements."),
                Map.entry("simpleCriteria.step1.title", "User-defined criteria"),
                Map.entry("simpleCriteria.step1.desc", "The criteria defined by the user for evaluation."),
                Map.entry("simpleCriteria.step2.title", "LLM evaluation"),
                Map.entry("simpleCriteria.step2.desc", "LLM evaluates the response on a scale from %d to %d."),
                Map.entry("simpleCriteria.step3.title", "Score normalization"),
                Map.entry("simpleCriteria.step3.desc", "Normalizing to 0-1 scale."),
                Map.entry(
                        "simpleCriteria.meaning",
                        "Score: %d out of %d for criteria \"%s\". Interpretation depends on your requirements."),
                Map.entry("simpleCriteria.defaultCriteria", "Custom Criteria"),

                // RubricsScore
                Map.entry(
                        "rubricsScore.description",
                        "Rubric-based evaluation - LLM selects a level from a predefined scale "
                                + "with descriptions for each level."),
                Map.entry("rubricsScore.step1.title", "Evaluation rubric"),
                Map.entry("rubricsScore.step1.desc", "User defined a scale with description for each level."),
                Map.entry("rubricsScore.step2.title", "LLM level selection"),
                Map.entry("rubricsScore.step2.desc", "LLM analyzed the response and selected the appropriate level."),
                Map.entry("rubricsScore.step2.output", "Level %d: %s"),
                Map.entry("rubricsScore.step3.title", "Score calculation"),
                Map.entry("rubricsScore.step3.desc", "Normalizing selected level to 0-1 scale."),
                Map.entry("rubricsScore.meaning", "Selected level %d: %s"),
                Map.entry("rubricsScore.level", "Level %d"));
    }

    private static Map<String, String> russianMessages() {
        return Map.ofEntries(
                // Common
                Map.entry("common.na", "Н/Д"),
                Map.entry("common.unknown", "Неизвестно"),
                Map.entry("common.scoreNotCalculated", "Скор не вычислен"),
                Map.entry("common.score", "оценка"),
                Map.entry("common.level", "уровень"),

                // Scale levels (standard)
                Map.entry("scale.excellent", "Отлично"),
                Map.entry("scale.good", "Хорошо"),
                Map.entry("scale.moderate", "Удовлетворительно"),
                Map.entry("scale.poor", "Плохо"),
                Map.entry("scale.excellent.desc", "Очень высокий результат"),
                Map.entry("scale.good.desc", "Хороший результат"),
                Map.entry("scale.moderate.desc", "Средний результат"),
                Map.entry("scale.poor.desc", "Низкий результат"),

                // Verdicts
                Map.entry("verdict.faithful", "ВЕРНО"),
                Map.entry("verdict.unfaithful", "НЕВЕРНО"),
                Map.entry("verdict.relevant", "Релевантен"),
                Map.entry("verdict.notRelevant", "Нерелевантен"),
                Map.entry("verdict.found", "Найдено"),
                Map.entry("verdict.missing", "Не найдено"),
                Map.entry("verdict.pass", "PASS"),
                Map.entry("verdict.fail", "FAIL"),
                Map.entry("verdict.ok", "OK"),
                Map.entry("verdict.error", "Ошибка"),

                // Step titles - common
                Map.entry("step.computeScore", "Расчёт итогового скора"),

                // Faithfulness
                Map.entry(
                        "faithfulness.description",
                        "Метрика проверяет: не выдумал ли AI что-то от себя? "
                                + "Мы разбиваем ответ на отдельные факты и проверяем каждый по контексту."),
                Map.entry("faithfulness.step1.title", "Разбиение ответа на утверждения"),
                Map.entry(
                        "faithfulness.step1.desc", "Мы взяли ответ AI и разбили его на отдельные факты (утверждения)."),
                Map.entry("faithfulness.step1.output", "Получено %d утверждений"),
                Map.entry("faithfulness.step2.title", "Проверка утверждений по контексту"),
                Map.entry(
                        "faithfulness.step2.desc",
                        "Для каждого утверждения проверили: можно ли его найти в предоставленном контексте?"),
                Map.entry("faithfulness.step2.output", "%d из %d утверждений подтверждены"),
                Map.entry("faithfulness.step3.title", "Расчёт итогового скора"),
                Map.entry("faithfulness.step3.desc", "Делим количество верных утверждений на общее количество."),
                Map.entry("faithfulness.formula", "среднее((верные утверждения) / (всего утверждений) по моделям)"),
                Map.entry("faithfulness.meaning.excellent", "Отлично - AI опирается только на факты из контекста"),
                Map.entry("faithfulness.meaning.good", "Хорошо - большинство утверждений подтверждены контекстом"),
                Map.entry("faithfulness.meaning.moderate", "Средне - часть информации не подтверждена контекстом"),
                Map.entry("faithfulness.meaning.poor", "Плохо - много \"галлюцинаций\" (выдуманных фактов)"),
                Map.entry("faithfulness.scale.excellent", "Все утверждения подтверждены контекстом"),
                Map.entry("faithfulness.scale.good", "Большинство утверждений подтверждены"),
                Map.entry("faithfulness.scale.moderate", "Часть утверждений не подтверждена"),
                Map.entry("faithfulness.scale.poor", "Много галлюцинаций"),

                // AspectCritic
                Map.entry(
                        "aspectCritic.description",
                        "Бинарная проверка: соответствует ли ответ заданному критерию? Результат: PASS или FAIL."),
                Map.entry("aspectCritic.step1.title", "Пользовательский аспект"),
                Map.entry("aspectCritic.step1.desc", "Критерий, заданный пользователем для проверки."),
                Map.entry("aspectCritic.step2.title", "Оценка аспекта"),
                Map.entry("aspectCritic.step2.desc", "LLM проанализировал ответ на соответствие критерию."),
                Map.entry("aspectCritic.step3.title", "Расчёт скора"),
                Map.entry("aspectCritic.step3.desc", "PASS → 1.0, FAIL → 0.0"),
                Map.entry("aspectCritic.meaning.pass", "Критерий \"%s\" ВЫПОЛНЕН"),
                Map.entry("aspectCritic.meaning.fail", "Критерий \"%s\" НЕ ВЫПОЛНЕН"),
                Map.entry("aspectCritic.scale.pass", "Критерий выполнен"),
                Map.entry("aspectCritic.scale.fail", "Критерий не выполнен"),
                Map.entry("aspectCritic.defaultAspect", "Пользовательский аспект"),

                // ContextPrecision
                Map.entry(
                        "contextPrecision.description",
                        "Метрика проверяет: правильно ли отранжированы контексты? "
                                + "Релевантные контексты должны идти первыми в списке."),
                Map.entry("contextPrecision.step1.title", "Оценка релевантности контекстов"),
                Map.entry(
                        "contextPrecision.step1.desc",
                        "Для каждого контекста проверяем: релевантен ли он для ответа на вопрос?"),
                Map.entry("contextPrecision.step1.output", "%d из %d релевантны"),
                Map.entry("contextPrecision.step2.title", "Расчёт Precision@K"),
                Map.entry("contextPrecision.step2.desc", "Precision@K = (релевантных в топ-K) / K"),
                Map.entry("contextPrecision.step3.title", "Расчёт Average Precision"),
                Map.entry(
                        "contextPrecision.step3.desc",
                        "AP = среднее Precision@K только для позиций с релевантными контекстами"),
                Map.entry("contextPrecision.formula", "AP = Σ(Precision@k × relevance@k) / (кол-во релевантных)"),
                Map.entry("contextPrecision.meaning.excellent", "Отлично - релевантные контексты идут первыми"),
                Map.entry(
                        "contextPrecision.meaning.good", "Хорошо - большинство релевантных контекстов в начале списка"),
                Map.entry("contextPrecision.meaning.moderate", "Средне - ранжирование требует улучшения"),
                Map.entry(
                        "contextPrecision.meaning.poor", "Плохо - релевантные контексты затеряны среди нерелевантных"),

                // ContextRecall
                Map.entry(
                        "contextRecall.description",
                        "Метрика проверяет: покрывают ли контексты всю информацию из эталонного ответа? "
                                + "Каждое предложение эталона должно найтись в контекстах."),
                Map.entry("contextRecall.step1.title", "Проверка покрытия эталона контекстами"),
                Map.entry(
                        "contextRecall.step1.desc",
                        "Для каждого предложения из эталонного ответа проверяем: "
                                + "можно ли его найти в предоставленных контекстах?"),
                Map.entry("contextRecall.step1.output", "%d из %d найдены в контекстах"),
                Map.entry("contextRecall.step2.title", "Расчёт покрытия"),
                Map.entry("contextRecall.step2.desc", "Доля предложений эталона, найденных в контекстах."),
                Map.entry("contextRecall.formula", "среднее((найденные предложения) / (всего предложений) по моделям)"),
                Map.entry("contextRecall.meaning.excellent", "Отлично - контексты полностью покрывают эталонный ответ"),
                Map.entry("contextRecall.meaning.good", "Хорошо - большая часть эталона найдена в контекстах"),
                Map.entry("contextRecall.meaning.moderate", "Средне - часть информации отсутствует в контекстах"),
                Map.entry("contextRecall.meaning.poor", "Плохо - контексты не содержат значительную часть эталона"),
                Map.entry("contextRecall.scale.excellent", "Полное покрытие эталона"),
                Map.entry("contextRecall.scale.good", "Хорошее покрытие"),
                Map.entry("contextRecall.scale.moderate", "Частичное покрытие"),
                Map.entry("contextRecall.scale.poor", "Недостаточное покрытие"),

                // ContextEntityRecall
                Map.entry(
                        "contextEntityRecall.description",
                        "Метрика проверяет: содержат ли контексты все важные сущности из эталона? "
                                + "Сущности - это имена, даты, места, организации и т.д."),
                Map.entry("contextEntityRecall.step1.title", "Извлечение сущностей из эталона"),
                Map.entry(
                        "contextEntityRecall.step1.desc",
                        "Находим все важные сущности в эталонном ответе: имена, даты, места и т.д."),
                Map.entry("contextEntityRecall.step1.output", "Найдено %d сущностей"),
                Map.entry("contextEntityRecall.step2.title", "Извлечение сущностей из контекстов"),
                Map.entry("contextEntityRecall.step2.desc", "Находим все сущности в предоставленных контекстах."),
                Map.entry("contextEntityRecall.step3.title", "Сравнение сущностей"),
                Map.entry("contextEntityRecall.step3.desc", "Какие сущности из эталона найдены в контекстах?"),
                Map.entry("contextEntityRecall.step3.output", "%d из %d найдены"),
                Map.entry("contextEntityRecall.step4.title", "Расчёт покрытия сущностей"),
                Map.entry("contextEntityRecall.step4.desc", "Доля сущностей эталона, найденных в контекстах."),
                Map.entry(
                        "contextEntityRecall.formula", "среднее((найденные сущности) / (всего сущностей) по моделям)"),
                Map.entry("contextEntityRecall.meaning.excellent", "Отлично - контексты содержат все важные сущности"),
                Map.entry("contextEntityRecall.meaning.good", "Хорошо - большинство сущностей присутствует"),
                Map.entry("contextEntityRecall.meaning.moderate", "Средне - часть сущностей отсутствует"),
                Map.entry(
                        "contextEntityRecall.meaning.poor", "Плохо - много важных сущностей отсутствует в контекстах"),

                // NoiseSensitivity
                Map.entry(
                        "noiseSensitivity.description",
                        "Метрика проверяет: влияет ли нерелевантная информация на ответ Агентной системы? "
                                + "Чем ниже значение, тем лучше система игнорирует посторонний контекст."),
                Map.entry("noiseSensitivity.step1.title", "Разбиение эталона на утверждения"),
                Map.entry("noiseSensitivity.step1.desc", "Эталонный ответ разбивается на отдельные факты."),
                Map.entry("noiseSensitivity.step1.output", "Утверждения эталона"),
                Map.entry("noiseSensitivity.step2.title", "Разбиение ответа AI на утверждения"),
                Map.entry("noiseSensitivity.step2.desc", "Ответ Агентной системы разбивается на отдельные факты."),
                Map.entry("noiseSensitivity.step2.output", "Утверждения ответа"),
                Map.entry("noiseSensitivity.step3.title", "Анализ соответствия и источников"),
                Map.entry(
                        "noiseSensitivity.step3.desc",
                        "Проверяем, совпадают ли утверждения ответа с эталоном и откуда взята информация."),
                Map.entry("noiseSensitivity.step3.output", "%d ошибок из %d проверок"),
                Map.entry("noiseSensitivity.step4.title", "Расчёт чувствительности к шуму"),
                Map.entry(
                        "noiseSensitivity.step4.desc",
                        "Доля ошибок, вызванных нерелевантным контекстом. Меньше = лучше."),
                Map.entry("noiseSensitivity.formula", "среднее((ошибки от шума / всего проверок) по моделям)"),
                Map.entry(
                        "noiseSensitivity.meaning.excellent",
                        "ОТЛИЧНО - Система полностью игнорирует нерелевантный контекст"),
                Map.entry("noiseSensitivity.meaning.good", "ХОРОШО - Минимальное влияние постороннего контекста"),
                Map.entry("noiseSensitivity.meaning.moderate", "СРЕДНЕ - Заметное влияние постороннего контекста"),
                Map.entry("noiseSensitivity.meaning.poor", "ПЛОХО - Ответ сильно искажён нерелевантной информацией"),
                Map.entry("noiseSensitivity.scale.excellent", "Нет влияния шума (ОТЛИЧНО)"),
                Map.entry("noiseSensitivity.scale.good", "Минимальное влияние"),
                Map.entry("noiseSensitivity.scale.moderate", "Заметное влияние"),
                Map.entry("noiseSensitivity.scale.poor", "Сильное искажение ответа"),

                // ResponseRelevancy
                Map.entry(
                        "responseRelevancy.description",
                        "Метрика проверяет: действительно ли ответ AI отвечает на вопрос пользователя? "
                                + "Генерируем вопросы на основе ответа и сравниваем их с оригинальным запросом."),
                Map.entry("responseRelevancy.step1.title", "Исходный вопрос пользователя"),
                Map.entry("responseRelevancy.step1.desc", "Запрос от пользователя к Агентной системе."),
                Map.entry("responseRelevancy.step2.title", "Генерация вопросов из ответа"),
                Map.entry(
                        "responseRelevancy.step2.desc",
                        "LLM генерирует вопросы, на которые данный ответ мог бы являться ответом."),
                Map.entry("responseRelevancy.step2.output", "Сгенерированные вопросы"),
                Map.entry("responseRelevancy.step3.title", "Расчёт семантического сходства"),
                Map.entry(
                        "responseRelevancy.step3.desc",
                        "Сравниваем сгенерированные вопросы с оригинальным через эмбеддинги. "
                                + "Чем больше похожи вопросы - тем релевантнее ответ."),
                Map.entry("responseRelevancy.step4.title", "Расчёт среднего сходства"),
                Map.entry(
                        "responseRelevancy.step4.desc",
                        "Итоговый скор = среднее значение сходства всех сгенерированных вопросов."),
                Map.entry("responseRelevancy.formula", "mean(cosine_similarity(оригинал, сгенерированные))"),
                Map.entry("responseRelevancy.meaning.excellent", "Отлично - ответ напрямую отвечает на вопрос"),
                Map.entry("responseRelevancy.meaning.good", "Хорошо - ответ в основном релевантен вопросу"),
                Map.entry("responseRelevancy.meaning.moderate", "Средне - ответ частично отвечает на вопрос"),
                Map.entry("responseRelevancy.meaning.poor", "Плохо - ответ не по теме"),
                Map.entry("responseRelevancy.scale.excellent", "Ответ напрямую отвечает на вопрос"),
                Map.entry("responseRelevancy.scale.good", "Ответ в основном релевантен"),
                Map.entry("responseRelevancy.scale.moderate", "Ответ частично отвечает на вопрос"),
                Map.entry("responseRelevancy.scale.poor", "Ответ не по теме"),

                // SimpleCriteria
                Map.entry(
                        "simpleCriteria.description",
                        "Оценка по пользовательскому критерию на непрерывной шкале. "
                                + "Интерпретация зависит от ваших требований к критерию."),
                Map.entry("simpleCriteria.step1.title", "Пользовательский критерий"),
                Map.entry("simpleCriteria.step1.desc", "Критерий, заданный пользователем для оценки."),
                Map.entry("simpleCriteria.step2.title", "Оценка LLM"),
                Map.entry("simpleCriteria.step2.desc", "LLM оценивает ответ по критерию на шкале от %d до %d."),
                Map.entry("simpleCriteria.step3.title", "Нормализация скора"),
                Map.entry("simpleCriteria.step3.desc", "Приводим оценку к шкале 0-1."),
                Map.entry(
                        "simpleCriteria.meaning",
                        "Оценка: %d из %d по критерию \"%s\". Интерпретация зависит от ваших требований."),
                Map.entry("simpleCriteria.defaultCriteria", "Пользовательский критерий"),

                // RubricsScore
                Map.entry(
                        "rubricsScore.description",
                        "Оценка по рубрикам - LLM выбирает уровень из заранее определённой шкалы "
                                + "с описанием каждого уровня."),
                Map.entry("rubricsScore.step1.title", "Шкала оценки (рубрика)"),
                Map.entry("rubricsScore.step1.desc", "Пользователь определил шкалу с описанием каждого уровня."),
                Map.entry("rubricsScore.step2.title", "Выбор уровня LLM"),
                Map.entry("rubricsScore.step2.desc", "LLM проанализировал ответ и выбрал подходящий уровень."),
                Map.entry("rubricsScore.step2.output", "Уровень %d: %s"),
                Map.entry("rubricsScore.step3.title", "Расчёт скора"),
                Map.entry("rubricsScore.step3.desc", "Нормализуем выбранный уровень к шкале 0-1."),
                Map.entry("rubricsScore.meaning", "Выбран уровень %d: %s"),
                Map.entry("rubricsScore.level", "Уровень %d"));
    }
}
