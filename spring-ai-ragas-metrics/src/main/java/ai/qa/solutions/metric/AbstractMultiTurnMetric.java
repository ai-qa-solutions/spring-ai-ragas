package ai.qa.solutions.metric;

import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.ScoreAggregator;
import ai.qa.solutions.sample.Sample;
import ai.qa.solutions.sample.message.AIMessage;
import ai.qa.solutions.sample.message.BaseMessage;
import ai.qa.solutions.sample.message.HumanMessage;
import ai.qa.solutions.sample.message.ToolCall;
import ai.qa.solutions.sample.message.ToolMessage;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Abstract base class for multi-turn agent metrics.
 * <p>
 * Multi-turn metrics evaluate conversation history with multiple user/assistant
 * exchanges and tool calls. Examples include:
 * <ul>
 *   <li>Agent Goal Accuracy - Did the agent achieve its goal?</li>
 *   <li>Tool Call Accuracy - Were the correct tools called?</li>
 *   <li>Topic Adherence - Did the agent stay on topic?</li>
 * </ul>
 * <p>
 * This class:
 * <ul>
 *   <li>Returns {@code true} from {@link #supportsMultiTurn()}</li>
 *   <li>Delegates {@link #singleTurnScore} to {@link #multiTurnScore} for backward compatibility</li>
 *   <li>Provides helper methods for formatting conversation history</li>
 * </ul>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * public class AgentGoalAccuracyMetric extends AbstractMultiTurnMetric<Config> {
 *
 *     @Override
 *     public Double multiTurnScore(Config config, Sample sample) {
 *         List<BaseMessage> messages = sample.getUserInputMessages();
 *         String conversation = formatConversation(messages);
 *         // ... evaluate goal achievement ...
 *     }
 * }
 * }</pre>
 *
 * @param <T> the configuration type for this metric
 */
public abstract class AbstractMultiTurnMetric<T extends Metric.MetricConfiguration>
        extends AbstractMultiModelMetric<T> {

    /**
     * Creates a new multi-turn metric with the specified executor and AVERAGE aggregation.
     *
     * @param executor the multi-model executor for parallel evaluation
     */
    protected AbstractMultiTurnMetric(final MultiModelExecutor executor) {
        super(executor);
    }

    /**
     * Creates a new multi-turn metric with the specified executor and aggregation strategy.
     *
     * @param executor          the multi-model executor for parallel evaluation
     * @param defaultAggregator the default aggregation strategy
     */
    protected AbstractMultiTurnMetric(final MultiModelExecutor executor, final ScoreAggregator defaultAggregator) {
        super(executor, defaultAggregator);
    }

    // ==================== Multi-turn support ====================

    /**
     * Returns true as this is a multi-turn metric.
     *
     * @return always true
     */
    @Override
    public boolean supportsMultiTurn() {
        return true;
    }

    /**
     * Delegates single-turn evaluation to multi-turn for backward compatibility.
     * <p>
     * This allows existing code using {@code singleTurnScore()} to continue working
     * with multi-turn metrics.
     *
     * @param metricConfiguration the metric configuration
     * @param sample the sample to evaluate
     * @return the evaluation score from multiTurnScore
     */
    @Override
    public Double singleTurnScore(final T metricConfiguration, final Sample sample) {
        return multiTurnScore(metricConfiguration, sample);
    }

    /**
     * Delegates single-turn async evaluation to multi-turn for backward compatibility.
     *
     * @param metricConfiguration the metric configuration
     * @param sample the sample to evaluate
     * @return a CompletableFuture containing the evaluation score from multiTurnScoreAsync
     */
    @Override
    public CompletableFuture<Double> singleTurnScoreAsync(final T metricConfiguration, final Sample sample) {
        return multiTurnScoreAsync(metricConfiguration, sample);
    }

    // ==================== Multi-turn rich evaluation ====================

    /**
     * Evaluates a multi-turn sample and returns a rich result with score, explanation, and metadata.
     * <p>
     * Delegates to {@link #singleTurnEvaluate} which internally calls
     * {@link #singleTurnScore} -> {@link #multiTurnScore}, correctly running
     * the multi-turn evaluation pipeline.
     *
     * @param metricConfiguration the metric configuration
     * @param sample the sample to evaluate (must contain userInputMessages)
     * @return rich evaluation result with score, explanation, and metadata
     */
    @Override
    public EvaluationResult multiTurnEvaluate(final T metricConfiguration, final Sample sample) {
        return singleTurnEvaluate(metricConfiguration, sample);
    }

    /**
     * Evaluates a multi-turn sample asynchronously and returns a rich result.
     * <p>
     * Delegates to {@link #singleTurnEvaluateAsync}.
     *
     * @param metricConfiguration the metric configuration
     * @param sample the sample to evaluate (must contain userInputMessages)
     * @return a CompletableFuture containing the rich evaluation result
     */
    @Override
    public CompletableFuture<EvaluationResult> multiTurnEvaluateAsync(
            final T metricConfiguration, final Sample sample) {
        return singleTurnEvaluateAsync(metricConfiguration, sample);
    }

    // ==================== Conversation formatting helpers ====================

    /**
     * Formats a conversation history into a readable string.
     * <p>
     * Output format:
     * <pre>
     * [USER]: User message content
     * [ASSISTANT]: AI response content
     *   Tool calls:
     *     - tool_name({"arg": "value"})
     * [TOOL]: Tool result content
     * </pre>
     *
     * @param messages the list of typed messages
     * @return formatted conversation string
     */
    protected String formatConversation(final List<BaseMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        return messages.stream().map(this::formatMessage).collect(Collectors.joining("\n"));
    }

    /**
     * Formats a single message based on its type.
     *
     * @param message the message to format
     * @return formatted message string
     */
    protected String formatMessage(final BaseMessage message) {
        if (message instanceof HumanMessage h) {
            return "[USER]: " + h.content();
        } else if (message instanceof AIMessage a) {
            return formatAIMessage(a);
        } else if (message instanceof ToolMessage t) {
            return "[TOOL]: " + t.content();
        }
        return "[UNKNOWN]: " + message.content();
    }

    /**
     * Formats an AI message including any tool calls.
     *
     * @param ai the AI message to format
     * @return formatted message string with tool calls
     */
    private String formatAIMessage(final AIMessage ai) {
        final StringBuilder sb = new StringBuilder("[ASSISTANT]: ");
        sb.append(ai.content());

        final List<ToolCall> toolCalls = ai.toolCalls();
        if (toolCalls != null && !toolCalls.isEmpty()) {
            sb.append("\n  Tool calls:");
            for (final ToolCall tc : toolCalls) {
                sb.append("\n    - ")
                        .append(tc.name())
                        .append("(")
                        .append(tc.arguments())
                        .append(")");
            }
        }
        return sb.toString();
    }

    /**
     * Extracts the last user message from a conversation.
     *
     * @param messages the list of typed messages
     * @return the content of the last HumanMessage, or empty string if not found
     */
    protected String getLastUserMessage(final List<BaseMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i) instanceof HumanMessage h) {
                return h.content();
            }
        }
        return "";
    }

    /**
     * Extracts the last assistant message from a conversation.
     *
     * @param messages the list of typed messages
     * @return the content of the last AIMessage, or empty string if not found
     */
    protected String getLastAssistantMessage(final List<BaseMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i) instanceof AIMessage a) {
                return a.content();
            }
        }
        return "";
    }

    /**
     * Extracts all tool calls from AIMessages in the conversation.
     *
     * @param messages the list of typed messages
     * @return list of all tool calls made during the conversation
     */
    protected List<ToolCall> extractToolCalls(final List<BaseMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        return messages.stream()
                .filter(m -> m instanceof AIMessage)
                .map(m -> (AIMessage) m)
                .flatMap(a -> a.toolCalls().stream())
                .toList();
    }
}
