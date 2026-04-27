package ai.qa.solutions.allure.listener;

/**
 * Configuration for selectively rendering report sections and attachments.
 * <p>
 * Used by {@link ai.qa.solutions.allure.template.FreemarkerTemplateEngine} and
 * {@link AllureAttachmentWriter} to toggle individual report parts on or off.
 * Templates read this record from the Freemarker data model under the key
 * {@code "renderConfig"} to decide which sections to emit.
 * <p>
 * <b>Breaking change vs. prior behavior:</b> {@link #methodology()} defaults to
 * {@code false} in {@link #defaults()}. Earlier listener implementations always
 * included the methodology block; consumers that relied on that behavior must
 * opt in explicitly via the builder.
 *
 * @param htmlAttachment     whether to attach the rendered HTML report to the step
 * @param markdownAttachment whether to attach the rendered Markdown report to the step
 * @param summary            whether to render the high-level summary section (scores,
 *                           timing, models)
 * @param explanation        whether to render the per-step / per-model explanation section
 * @param methodology        whether to render the metric methodology section. Defaults to
 *                           {@code false} (breaking change) to keep attachments compact —
 *                           opt in only when methodology context is needed
 * @param executionLog       whether to render the execution log / steps timeline section
 * @param excludedModels     whether to render the excluded models section (models filtered
 *                           out of aggregation due to errors or policy)
 */
public record RenderConfig(
        boolean htmlAttachment,
        boolean markdownAttachment,
        boolean summary,
        boolean explanation,
        boolean methodology,
        boolean executionLog,
        boolean excludedModels) {

    /**
     * Returns the default configuration: all attachments and sections enabled
     * <i>except</i> {@link #methodology()}, which defaults to {@code false}.
     *
     * @return default {@code RenderConfig}
     */
    public static RenderConfig defaults() {
        return new RenderConfig(true, true, true, true, false, true, true);
    }

    /**
     * @return {@code true} if at least one attachment format (HTML or Markdown) is enabled
     */
    public boolean anyAttachmentEnabled() {
        return htmlAttachment || markdownAttachment;
    }

    /**
     * @return {@code true} if at least one report section is enabled
     */
    public boolean anySectionEnabled() {
        return summary || explanation || methodology || executionLog || excludedModels;
    }
}
