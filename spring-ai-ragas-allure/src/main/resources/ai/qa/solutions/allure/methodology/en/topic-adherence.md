# Topic Adherence Metric

## Glossary

- **Agent** — an AI-powered system capable of executing multi-turn conversations
- **Topic** — a distinct subject or theme discussed in a conversation
- **Reference Topics** — the expected/allowed topics that the conversation should cover
- **On Topic** — a topic that matches or relates to a reference topic
- **Off Topic** — a topic that does not relate to any reference topic

## Description

The metric evaluates whether conversation topics adhere to expected reference topics. It extracts topics from the conversation using an LLM and classifies each topic against reference topics to determine if the discussion stayed on track.

The metric supports three scoring modes:

- **F1** — Harmonic mean of precision and recall (default, balanced)
- **PRECISION** — Focus on avoiding off-topic discussions
- **RECALL** — Focus on covering all reference topics

## Example

**Conversation:**

```
User: I'd like to book a flight to Paris for next week.
Assistant: I'd be happy to help you book a flight to Paris. What dates are you looking at?
User: I'll fly from New York, departing Monday and returning Friday.
Assistant: Found several options. There's a direct Air France flight at 7 PM for $850. Book it?
```

**Reference Topics:**
- flight booking
- travel arrangements
- airline tickets

**Extracted Topics:**
- flight booking to Paris
- flight search
- Air France options

**Analysis:**
- 3 topics extracted from conversation
- 3 topics are on topic (match reference topics)
- Precision: 3/3 = 1.0
- Recall: 3/3 = 1.0 (all reference topics covered)

**Score:** **1.0 (100%)** — Perfect Adherence

## Score Interpretation

|  Score  |                    Meaning                    |
|---------|-----------------------------------------------|
| 90-100% | Excellent — all topics aligned with reference |
| 70-89%  | Good — most topics on target                  |
| 50-69%  | Moderate — some topic drift                   |
| 0-49%   | Poor — significant off-topic discussions      |

## Algorithm

1. **Extract Topics** — LLM analyzes the conversation to identify all discussed topics
2. **Classify Topics** — Each extracted topic is classified as on-topic or off-topic against reference topics
3. **Compute Score** — Calculate precision, recall, and final score based on mode

### Score Calculation

```
Precision = on-topic count / total extracted topics
Recall = covered reference topics / total reference topics
F1 = 2 × (precision × recall) / (precision + recall)
```

## Formula

**F1 Mode (default):**

```
Score = 2 × (Precision × Recall) / (Precision + Recall)
```

**Precision Mode:**

```
Score = Precision = on-topic / total extracted
```

**Recall Mode:**

```
Score = Recall = covered reference / total reference
```

## Configuration

```java
TopicAdherenceConfig config = TopicAdherenceConfig.builder()
    .mode(Mode.F1)  // or Mode.PRECISION, Mode.RECALL
    .build();
```

| Parameter |  Type  | Default |              Description               |
|-----------|--------|---------|----------------------------------------|
| `mode`    | `Mode` | `F1`    | Scoring mode: F1, PRECISION, or RECALL |
| `models`  | `List` | all     | Model IDs to use for evaluation        |

## Sample Requirements

|       Field       | Required |                  Description                  |
|-------------------|----------|-----------------------------------------------|
| `messages`        | Yes      | List of conversation messages (role, content) |
| `referenceTopics` | Yes      | List of expected/allowed topics               |

### Message Structure

```java
public record Message(String role, String content) {}
```

## Use Cases

- Evaluating chatbot focus on designated topics
- Testing customer support agents stay on-topic
- Measuring educational assistants covering required material
- Validating specialized domain agents (legal, medical, technical)

## References

- [RAGAS Documentation](https://docs.ragas.io/)
- [Topic Adherence in Python RAGAS](https://docs.ragas.io/en/latest/concepts/metrics/topic_adherence.html)

