# Context Precision Metric

## Glossary

- **Context** — information fragments retrieved by the search system and provided to the agentic system
- **Relevance** — correspondence of the context to the user's original question
- **Agentic System** — an LLM-based system that generates responses
- **Average Precision** — mean precision accounting for positions of relevant results

## Description

The metric evaluates the quality of context ranking by the retrieval system. It checks whether the most relevant contexts appear at the top positions. Relevant contexts at the beginning of the list increase the score; at the end — decrease it.

## Example

**User Question:** "When was the Eiffel Tower built?"

**Retrieved Contexts (in order):**
1. "The Eiffel Tower was completed in 1889 for the World's Fair." — **RELEVANT**
2. "It stands 330 meters tall." — **NOT RELEVANT** to the question
3. "Construction began in 1887." — **RELEVANT**

**Calculation:**
- Position 1: Relevant → Precision@1 = 1/1 = 1.00
- Position 2: Not relevant → skip
- Position 3: Relevant → Precision@3 = 2/3 = 0.67

**Score:** (1.00 + 0.67) / 2 = **0.83 (83%)**

## Score Interpretation

|  Score  |                     Meaning                     |
|---------|-------------------------------------------------|
| 90-100% | Relevant contexts at top positions              |
| 70-90%  | Most relevant contexts near the top of the list |
| 50-70%  | Relevant contexts distributed unevenly          |
| 0-50%   | Relevant contexts at bottom positions           |

## Algorithm

1. **Relevance Check** — LLM determines relevance of each context to the question
2. **Precision Calculation at Positions** — precision is calculated at each relevant context position
3. **Averaging** — mean value of all precisions is computed

## Formula

```
score = Average Precision (AP) = Σ(precision@k × relevance@k) / total_relevant
```

Where:
- `precision@k` — fraction of relevant contexts in top-k positions
- `relevance@k` — 1 if context at position k is relevant, otherwise 0
- `total_relevant` — total number of relevant contexts

## References

- [RAGAS Documentation](https://github.com/vibrantlabsai/ragas/blob/main/docs/concepts/metrics/available_metrics/context_precision.md)
- [Mean Average Precision (MAP)](https://en.wikipedia.org/wiki/Evaluation_measures_(information_retrieval)#Mean_average_precision)

