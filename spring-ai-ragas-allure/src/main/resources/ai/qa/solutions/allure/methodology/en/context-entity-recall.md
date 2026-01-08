# Context Entity Recall Metric

## Glossary

- **Entity** — a named object: person's name, place name, date, organization
- **Reference (Ground Truth)** — the expected correct answer to the user's question
- **Context** — information fragments retrieved by the search system
- **Agentic System** — an LLM-based system that generates responses

## Description

The metric evaluates the completeness of coverage of named entities from the reference in the context. Unlike Context Recall, it checks not sentences but specific objects: people's names, place names, dates, organizations. Missing key entities in the context prevents the agentic system from including them in the response.

## Example

**Reference (Ground Truth):** "Gustave Eiffel designed the tower in Paris in 1889."

**Entities in Reference:**
- Gustave Eiffel (person)
- Paris (place)
- 1889 (date)

**Context:** "The tower in Paris was completed in 1889 and remains a popular landmark."

**Checking entities:**
1. "Gustave Eiffel" → not found ✗
2. "Paris" → found ✓
3. "1889" → found ✓

**Score:** 2/3 = **0.67 (67%)**

## Score Interpretation

|  Score  |                        Meaning                         |
|---------|--------------------------------------------------------|
| 90-100% | All key entities from reference are present in context |
| 70-90%  | Most entities found in context                         |
| 50-70%  | Some entities missing from context                     |
| 0-50%   | Context lacks most key entities                        |

## Algorithm

1. **Entity Extraction** — LLM extracts named entities from the reference
2. **Matching** — presence of each entity in context is verified
3. **Score Calculation** — ratio of found entities to total count

## Formula

```
score = entities_found / total_entities
```

Where:
- `entities_found` — number of reference entities found in context
- `total_entities` — total number of entities in the reference

## References

- [RAGAS Documentation](https://github.com/vibrantlabsai/ragas/blob/main/docs/concepts/metrics/available_metrics/context_entities_recall.md)

