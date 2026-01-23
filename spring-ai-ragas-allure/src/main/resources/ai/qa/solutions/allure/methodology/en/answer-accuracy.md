# Answer Accuracy Metric

## Overview

Answer Accuracy is a NVIDIA-style evaluation metric that measures how accurately an AI response matches a reference (ground truth) answer. It uses a 0-2 scoring scale that is normalized to 0-1 for consistency with other metrics.

## How It Works

### Scoring Scale

| Score |       Level       |                           Description                            |
|-------|-------------------|------------------------------------------------------------------|
| 0     | Incorrect         | Response is factually wrong or contradicts the reference         |
| 1     | Partially Correct | Response is partially correct but incomplete or has minor errors |
| 2     | Fully Correct     | Response accurately matches the reference answer                 |

### Algorithm

1. **Initial Judgment**: An LLM evaluates the response against the reference, assigning a score (0-2) with reasoning
2. **Confirmation Judgment** (optional): When dual-judge mode is enabled, a second evaluation confirms or adjusts the initial assessment
3. **Score Normalization**: The raw score (0-2) is divided by 2 to produce a 0-1 normalized score

### Formula

```
normalized_score = raw_score / 2
```

Where:
- `raw_score` is the LLM's judgment (0, 1, or 2)
- `normalized_score` is the final metric value (0.0 to 1.0)

## Interpretation

| Score Range |   Level   |                       Meaning                       |
|-------------|-----------|-----------------------------------------------------|
| 90-100%     | Excellent | Response accurately matches the reference           |
| 70-89%      | Good      | Response mostly matches the reference               |
| 50-69%      | Moderate  | Response partially matches the reference            |
| 0-49%       | Poor      | Response is inaccurate or contradicts the reference |

## Configuration Options

|   Parameter    |     Type     | Default |                  Description                  |
|----------------|--------------|---------|-----------------------------------------------|
| `useDualJudge` | boolean      | false   | Enable dual-judge mode for higher reliability |
| `temperature`  | double       | 0.1     | LLM temperature (lower = more deterministic)  |
| `models`       | List<String> | -       | Specific models to use (optional)             |

## Example Usage

```java
Sample sample = Sample.builder()
    .response("Paris is the capital of France.")
    .reference("Paris is the capital city of France.")
    .build();

AnswerAccuracyConfig config = AnswerAccuracyConfig.builder()
    .useDualJudge(false)
    .temperature(0.1)
    .build();

Double score = answerAccuracyMetric.singleTurnScore(config, sample);
```

## Dual-Judge Mode

When `useDualJudge` is enabled:

1. Initial LLM evaluates response vs reference
2. Second LLM reviews the initial assessment
3. The confirmation score is used if available
4. Falls back to initial score if confirmation fails

This mode improves reliability but increases latency and cost.

## Required Sample Fields

- `response` - The AI-generated response to evaluate
- `reference` - The ground truth/expected answer

## Comparison with Other Metrics

|       Metric        |          Purpose           |          Method          |
|---------------------|----------------------------|--------------------------|
| **Answer Accuracy** | Matches reference answer   | LLM judgment (0-2 scale) |
| Semantic Similarity | Embedding-based similarity | Cosine similarity        |
| Factual Correctness | Claims decomposition       | NLI verification         |

## Best Practices

1. **Clear References**: Provide clear, unambiguous reference answers
2. **Dual-Judge for Critical**: Use dual-judge mode for high-stakes evaluations
3. **Consistent Format**: Keep response and reference in similar formats
4. **Multiple Models**: Use multiple judge models for consensus

## Limitations

- LLM-based evaluation can have biases
- Sensitive to phrasing differences between response and reference
- May not capture nuanced correctness in complex domains
- Subjective interpretation of "partially correct"

