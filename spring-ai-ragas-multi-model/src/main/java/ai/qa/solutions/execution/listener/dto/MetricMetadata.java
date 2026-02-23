package ai.qa.solutions.execution.listener.dto;

/**
 * Marker interface for typed metric evaluation metadata.
 * <p>
 * All metric metadata records implement this interface to enable
 * type-safe handling in listeners and report generators.
 * <p>
 * Implementors are typically Java records that capture metric-specific
 * intermediate results (e.g., per-model verdicts, extracted statements).
 */
public interface MetricMetadata {}
