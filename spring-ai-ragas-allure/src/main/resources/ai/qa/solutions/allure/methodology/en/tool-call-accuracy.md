# Tool Call Accuracy Metric

## Glossary

- **Agent** — an AI-powered system capable of executing multi-turn conversations and taking actions
- **Tool Call** — a structured invocation of an external function or API made by the agent
- **Reference Tool Calls** — the expected correct sequence of tool invocations
- **F1 Score** — harmonic mean of precision and recall, balancing both metrics

## Description

The metric evaluates the accuracy of an AI agent's tool calls by comparing them against expected reference tool calls. It supports two matching modes:

- **STRICT** — requires exact match of both tool names and all arguments
- **FLEXIBLE** — allows partial argument matching based on a configurable threshold

## Example

**Actual Tool Calls:**

```json
[
  {"name": "search_flights", "arguments": {"from": "NYC", "to": "LAX", "date": "2024-01-15"}},
  {"name": "book_flight", "arguments": {"flight_id": "UA123", "passengers": 1}}
]
```

**Reference Tool Calls:**

```json
[
  {"name": "search_flights", "arguments": {"from": "NYC", "to": "LAX", "date": "2024-01-15"}},
  {"name": "book_flight", "arguments": {"flight_id": "UA123", "passengers": 1}}
]
```

**Analysis:**
- Both tool names match
- All arguments match exactly
- Precision: 2/2 = 1.0
- Recall: 2/2 = 1.0

**Score:** **1.0 (100%)** — Perfect Match

## Score Interpretation

|  Score  |                          Meaning                          |
|---------|-----------------------------------------------------------|
| 90-100% | Excellent — all tool calls match expected calls perfectly |
| 70-89%  | Good — most tool calls are correct                        |
| 50-69%  | Moderate — some tool calls are incorrect or missing       |
| 0-49%   | Poor — many tool calls are incorrect or missing           |

## Algorithm

### STRICT Mode

1. **Align Tool Calls** — match actual calls with reference calls
2. **Exact Match** — tool name AND all arguments must match exactly
3. **Compute Score** — calculate F1 from precision and recall

### FLEXIBLE Mode

1. **Align Tool Calls** — match actual calls with reference calls by tool name
2. **Partial Match** — calculate argument overlap ratio
3. **Apply Threshold** — match if overlap >= threshold (default 0.8)
4. **Compute Score** — calculate F1 from precision and recall

## Formula

```
Precision = True Positives / Total Actual Calls
Recall = True Positives / Total Reference Calls
F1 = 2 × (Precision × Recall) / (Precision + Recall)
```

Where:
- `True Positives` — actual calls that correctly match reference calls
- `Total Actual Calls` — total number of tool calls made by the agent
- `Total Reference Calls` — total number of expected tool calls

## Configuration

```java
ToolCallAccuracyConfig config = ToolCallAccuracyConfig.builder()
    .mode(Mode.STRICT)  // or Mode.FLEXIBLE
    .argumentMatchThreshold(0.8)  // for FLEXIBLE mode
    .build();
```

|        Parameter         |   Type   | Default  |                        Description                         |
|--------------------------|----------|----------|------------------------------------------------------------|
| `mode`                   | `Mode`   | `STRICT` | Matching mode: STRICT or FLEXIBLE                          |
| `argumentMatchThreshold` | `Double` | `0.8`    | Threshold for argument matching in FLEXIBLE mode (0.0-1.0) |

## Sample Requirements

|        Field         | Required |               Description               |
|----------------------|----------|-----------------------------------------|
| `toolCalls`          | Yes      | List of actual tool calls made by agent |
| `referenceToolCalls` | Yes      | List of expected/correct tool calls     |

### ToolCall Structure

```java
public record ToolCall(String name, Map<String, Object> arguments) {}
```

## Use Cases

- Evaluating agent's ability to use correct tools
- Testing tool selection accuracy in multi-step workflows
- Validating argument passing correctness
- Measuring agent's understanding of task requirements

## References

- [RAGAS Documentation](https://docs.ragas.io/)
- [Tool Call Accuracy in Python RAGAS](https://docs.ragas.io/en/latest/concepts/metrics/tool_call_accuracy.html)

