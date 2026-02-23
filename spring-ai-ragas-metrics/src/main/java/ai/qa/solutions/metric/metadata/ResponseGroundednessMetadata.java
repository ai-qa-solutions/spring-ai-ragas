package ai.qa.solutions.metric.metadata;

import ai.qa.solutions.execution.listener.dto.MetricMetadata;

/**
 * Typed metadata for Response Groundedness (NVIDIA) metric evaluation results.
 * <p>
 * Captures heuristic and LLM-based grounding evaluation results.
 *
 * @param usedHeuristicShortcuts whether heuristic shortcuts were enabled
 * @param heuristicMatch         whether a heuristic shortcut matched (exact match or containment)
 */
public record ResponseGroundednessMetadata(boolean usedHeuristicShortcuts, boolean heuristicMatch)
        implements MetricMetadata {}
