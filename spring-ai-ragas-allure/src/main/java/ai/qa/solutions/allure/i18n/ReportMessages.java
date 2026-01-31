package ai.qa.solutions.allure.i18n;

import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Localized messages for Allure report templates.
 * <p>
 * Provides translations for all UI strings in HTML and Markdown reports.
 * Supports English (en) and Russian (ru) languages.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReportMessages {

    private static final String DEFAULT_LANGUAGE = "en";

    /**
     * Returns localized messages for the specified language.
     *
     * @param language language code (en, ru)
     * @return map of message keys to localized strings
     */
    public static Map<String, String> forLanguage(final String language) {
        if ("ru".equalsIgnoreCase(language)) {
            return russianMessages();
        }
        return englishMessages();
    }

    private static Map<String, String> englishMessages() {
        return Map.ofEntries(
                // Report header
                Map.entry("report.title", "Evaluation Report"),
                Map.entry("report.duration", "Duration"),
                // Block headers
                Map.entry("block.summary", "Summary"),
                Map.entry("block.methodology", "Methodology"),
                Map.entry("block.execution", "Execution Log"),
                // Summary block
                Map.entry("summary.modelScores", "Model Scores"),
                Map.entry("summary.inputSample", "Input Sample"),
                Map.entry("summary.userInput", "User Input"),
                Map.entry("summary.response", "Response"),
                Map.entry("summary.reference", "Reference"),
                Map.entry("summary.retrievedContexts", "Retrieved Contexts"),
                Map.entry("summary.models", "Models"),
                Map.entry("summary.llmModels", "LLM Models"),
                Map.entry("summary.embeddingModels", "Embedding Models"),
                Map.entry("summary.excludedModels", "Excluded Models"),
                Map.entry("summary.configuration", "Configuration"),
                Map.entry("summary.executionTimeline", "Execution Timeline"),
                // Trace viewer
                Map.entry("trace.header", "Trace"),
                Map.entry("trace.selectItem", "Select an item from the trace"),
                Map.entry("trace.clickToView", "Click on a step or model in the tree to view details"),
                Map.entry("trace.input", "Input / Prompt"),
                Map.entry("trace.output", "Output / Response"),
                Map.entry("trace.error", "Error"),
                Map.entry("trace.stackTrace", "Stack Trace"),
                Map.entry("trace.scoreCalculation", "Score Calculation"),
                Map.entry(
                        "trace.computeInfo",
                        "This step aggregates results from previous LLM evaluations to compute the final score."),
                Map.entry("trace.modelsEvaluated", "Models evaluated"),
                Map.entry("trace.successful", "Successful"),
                Map.entry("trace.modelOutputs", "Model Outputs"),
                // Common
                Map.entry("common.nItems", "%d items"),
                // Status labels
                Map.entry("status.success", "Success"),
                Map.entry("status.error", "Error"),
                Map.entry("status.ok", "OK"),
                Map.entry("status.failed", "FAILED"),
                Map.entry("status.excluded", "EXCLUDED"),
                // Table headers
                Map.entry("table.model", "Model"),
                Map.entry("table.score", "Score"),
                Map.entry("table.result", "Result"),
                Map.entry("table.level", "Level"),
                Map.entry("table.status", "Status"),
                // Aggregation
                Map.entry("aggregation.average", "Average (%d models):"),
                Map.entry("table.failedStep", "Failed Step"),
                Map.entry("table.reason", "Reason"),
                // Meta labels
                Map.entry("meta.duration", "Duration"),
                Map.entry("meta.step", "Step"),
                // Excluded models section
                Map.entry("excluded.title", "Excluded Models"),
                // Step types (kept in English in both languages)
                Map.entry("stepType.llm", "LLM"),
                Map.entry("stepType.embedding", "EMB"),
                Map.entry("stepType.compute", "COMPUTE"),
                // Chart legend
                Map.entry("chart.llm", "LLM"),
                Map.entry("chart.embedding", "Embedding"),
                Map.entry("chart.compute", "Compute"),
                // Markdown specific
                Map.entry("md.timestamp", "Timestamp"),
                Map.entry("md.prompt", "Prompt"),
                Map.entry("md.noScores", "No scores available"),
                // Score Explanation block
                Map.entry("block.explanation", "Score Explanation"),
                Map.entry("explanation.whyScore", "Why this score?"),
                Map.entry("explanation.steps", "Steps"),
                Map.entry("explanation.step", "Step"),
                Map.entry("explanation.interpretation", "Interpretation"),
                Map.entry("explanation.formula", "Formula"),
                Map.entry("explanation.calculation", "Calculation"),
                Map.entry("explanation.result", "Result"),
                Map.entry("explanation.scale", "Scale"),
                Map.entry("explanation.currentLevel", "Current Level"),
                Map.entry("explanation.items", "Items"),
                Map.entry("explanation.verdict", "Verdict"),
                Map.entry("explanation.reason", "Reason"),
                Map.entry("explanation.source", "Source"),
                Map.entry("explanation.modelDisagreement", "Model Disagreement"),
                Map.entry("explanation.modelsAgree", "Models Agree"),
                Map.entry("explanation.modelsDisagree", "Models Disagree"),
                Map.entry("explanation.passed", "PASSED"),
                Map.entry("explanation.failed", "FAILED"),
                Map.entry("explanation.yourResult", "YOUR RESULT"),
                Map.entry("explanation.notAvailable", "Score explanation is not available for this metric."),
                // Input data labels
                Map.entry("input.questionAnswer", "Question + Reference"),
                // Conversation messages (agent metrics)
                Map.entry("summary.conversation", "Conversation"),
                Map.entry("message.type.human", "User"),
                Map.entry("message.type.ai", "Assistant"),
                Map.entry("message.type.tool", "Tool Result"),
                Map.entry("message.toolCalls", "Tool Calls"));
    }

    private static Map<String, String> russianMessages() {
        return Map.ofEntries(
                // Report header
                Map.entry("report.title", "Отчёт оценки"),
                Map.entry("report.duration", "Длительность"),
                // Block headers
                Map.entry("block.summary", "Результаты"),
                Map.entry("block.methodology", "Методология"),
                Map.entry("block.execution", "Журнал выполнения"),
                // Summary block
                Map.entry("summary.modelScores", "Оценки моделей"),
                Map.entry("summary.inputSample", "Входные данные"),
                Map.entry("summary.userInput", "Запрос пользователя"),
                Map.entry("summary.response", "Ответ"),
                Map.entry("summary.reference", "Эталон"),
                Map.entry("summary.retrievedContexts", "Полученные контексты"),
                Map.entry("summary.models", "Модели"),
                Map.entry("summary.llmModels", "LLM модели"),
                Map.entry("summary.embeddingModels", "Embedding модели"),
                Map.entry("summary.excludedModels", "Исключённые модели"),
                Map.entry("summary.configuration", "Конфигурация"),
                Map.entry("summary.executionTimeline", "Временная шкала"),
                // Trace viewer
                Map.entry("trace.header", "Трассировка"),
                Map.entry("trace.selectItem", "Выберите элемент"),
                Map.entry("trace.clickToView", "Нажмите на шаг или модель для просмотра деталей"),
                Map.entry("trace.input", "Вход / Промпт"),
                Map.entry("trace.output", "Выход / Ответ"),
                Map.entry("trace.error", "Ошибка"),
                Map.entry("trace.stackTrace", "Стек вызовов"),
                Map.entry("trace.scoreCalculation", "Расчёт оценки"),
                Map.entry(
                        "trace.computeInfo",
                        "Этот шаг агрегирует результаты предыдущих LLM оценок для вычисления итогового балла."),
                Map.entry("trace.modelsEvaluated", "Оценено моделей"),
                Map.entry("trace.successful", "Успешно"),
                Map.entry("trace.modelOutputs", "Ответы моделей"),
                // Common
                Map.entry("common.nItems", "%d элементов"),
                // Status labels
                Map.entry("status.success", "Успешно"),
                Map.entry("status.error", "Ошибка"),
                Map.entry("status.ok", "OK"),
                Map.entry("status.failed", "Ошибка"),
                Map.entry("status.excluded", "ИСКЛЮЧЁН"),
                // Table headers
                Map.entry("table.model", "Модель"),
                Map.entry("table.score", "Оценка"),
                Map.entry("table.result", "Результат"),
                Map.entry("table.level", "Уровень"),
                Map.entry("table.status", "Статус"),
                // Aggregation
                Map.entry("aggregation.average", "Среднее (%d моделей):"),
                Map.entry("table.failedStep", "Шаг ошибки"),
                Map.entry("table.reason", "Причина"),
                // Meta labels
                Map.entry("meta.duration", "Длительность"),
                Map.entry("meta.step", "Шаг"),
                // Excluded models section
                Map.entry("excluded.title", "Исключённые модели"),
                // Step types (kept in English in both languages for consistency)
                Map.entry("stepType.llm", "LLM"),
                Map.entry("stepType.embedding", "EMB"),
                Map.entry("stepType.compute", "COMPUTE"),
                // Chart legend
                Map.entry("chart.llm", "LLM"),
                Map.entry("chart.embedding", "Embedding"),
                Map.entry("chart.compute", "Compute"),
                // Markdown specific
                Map.entry("md.timestamp", "Время"),
                Map.entry("md.prompt", "Промпт"),
                Map.entry("md.noScores", "Нет оценок"),
                // Score Explanation block
                Map.entry("block.explanation", "Объяснение скора"),
                Map.entry("explanation.whyScore", "Почему такой скор?"),
                Map.entry("explanation.steps", "Шаги"),
                Map.entry("explanation.step", "Шаг"),
                Map.entry("explanation.interpretation", "Интерпретация"),
                Map.entry("explanation.formula", "Формула"),
                Map.entry("explanation.calculation", "Расчёт"),
                Map.entry("explanation.result", "Результат"),
                Map.entry("explanation.scale", "Шкала"),
                Map.entry("explanation.currentLevel", "Текущий уровень"),
                Map.entry("explanation.items", "Элементы"),
                Map.entry("explanation.verdict", "Вердикт"),
                Map.entry("explanation.reason", "Причина"),
                Map.entry("explanation.source", "Источник"),
                Map.entry("explanation.modelDisagreement", "Расхождение моделей"),
                Map.entry("explanation.modelsAgree", "Модели согласны"),
                Map.entry("explanation.modelsDisagree", "Модели не согласны"),
                Map.entry("explanation.passed", "ВЕРНО"),
                Map.entry("explanation.failed", "НЕВЕРНО"),
                Map.entry("explanation.yourResult", "ВАШ РЕЗУЛЬТАТ"),
                Map.entry("explanation.notAvailable", "Объяснение скора недоступно для этой метрики."),
                // Input data labels
                Map.entry("input.questionAnswer", "Вопрос + Эталон"),
                // Conversation messages (agent metrics)
                Map.entry("summary.conversation", "Диалог"),
                Map.entry("message.type.human", "Пользователь"),
                Map.entry("message.type.ai", "Ассистент"),
                Map.entry("message.type.tool", "Результат инструмента"),
                Map.entry("message.toolCalls", "Вызовы инструментов"));
    }
}
