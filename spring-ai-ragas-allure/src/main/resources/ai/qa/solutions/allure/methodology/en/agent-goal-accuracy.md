# Agent Goal Accuracy Metric

## Glossary

- **Agent** — an AI-powered system capable of executing multi-turn conversations and taking actions
- **Goal** — the intended outcome or objective the user wants to achieve through interaction with the agent
- **Multi-turn Conversation** — a dialogue consisting of multiple exchanges between user and agent

## Description

The metric evaluates whether an AI agent successfully achieved its intended goal based on multi-turn conversation analysis. It supports two evaluation modes:

- **WITH_REFERENCE** — compares the conversation outcome with a provided expected goal
- **WITHOUT_REFERENCE** — infers the goal from the conversation and evaluates if it was achieved

## Example

**Conversation:**

```
[USER]: I need to book a flight from New York to Los Angeles for tomorrow.
[ASSISTANT]: I'll help you book that flight. Let me search for available options.
[ASSISTANT]: I found several flights. The best option is United Airlines at 8:00 AM for $299.
[USER]: That sounds good, please book it.
[ASSISTANT]: Done! Confirmation number: UA12345. You'll receive an email shortly.
```

**Expected Outcome (Reference):** "Book a flight from New York to Los Angeles"

**Analysis:**
- The user requested a flight booking
- The agent found available flights
- The agent successfully completed the booking with a confirmation number

**Score:** **1.0 (100%)** — Goal Achieved

## Score Interpretation

|   Score    |                                         Meaning                                          |
|------------|------------------------------------------------------------------------------------------|
| 1.0 (100%) | Goal fully achieved — the agent successfully completed the intended task                 |
| 0.0 (0%)   | Goal not achieved — the agent failed to complete the task or only partially completed it |

## Algorithm

### WITH_REFERENCE Mode

1. **Compare Outcome** — LLM compares the conversation outcome with the provided expected goal
2. **Binary Verdict** — determines if the goal was achieved (1.0) or not (0.0)

### WITHOUT_REFERENCE Mode

1. **Infer Goal** — LLM analyzes the conversation to determine the user's primary objective
2. **Evaluate Outcome** — LLM evaluates if the agent's actions achieved the inferred goal
3. **Binary Verdict** — determines if the goal was achieved (1.0) or not (0.0)

## Formula

```
score = goalAchieved ? 1.0 : 0.0
```

Where:
- `goalAchieved` — boolean indicating whether the intended goal was fully accomplished

## Configuration

```java
AgentGoalAccuracyConfig config = AgentGoalAccuracyConfig.builder()
    .mode(Mode.WITH_REFERENCE)  // or Mode.WITHOUT_REFERENCE
    .build();
```

| Parameter |      Type      |      Default      |                     Description                      |
|-----------|----------------|-------------------|------------------------------------------------------|
| `mode`    | `Mode`         | `WITH_REFERENCE`  | Evaluation mode: WITH_REFERENCE or WITHOUT_REFERENCE |
| `models`  | `List<String>` | executor defaults | LLM models to use for evaluation                     |

## Sample Requirements

|    Field    | Required |                       Description                        |
|-------------|----------|----------------------------------------------------------|
| `messages`  | Yes      | List of conversation messages with role and content      |
| `reference` | No*      | Expected goal/outcome (required for WITH_REFERENCE mode) |

*If `reference` is not provided in WITH_REFERENCE mode, the metric falls back to WITHOUT_REFERENCE mode.

## Use Cases

- Evaluating task-oriented dialogue systems
- Measuring success rate of customer support agents
- Assessing multi-step workflow completion
- Testing conversational AI assistants

## References

- [RAGAS Documentation](https://docs.ragas.io/)
- [Agent Goal Accuracy in Python RAGAS](https://docs.ragas.io/en/latest/concepts/metrics/agent_goal_accuracy.html)

