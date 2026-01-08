# Response Relevancy Metric

## Glossary

- **Relevancy** — correspondence of the response to the user's original question
- **Embedding** — numerical vector representation of text
- **Cosine Similarity** — measure of closeness between two vectors (from 0 to 1)
- **Agentic System** — an LLM-based system that generates responses

## Description

The metric evaluates how well the agentic system's response corresponds to the user's question. For evaluation, hypothetical questions that this response could answer are generated from the response. The similarity of these questions to the original user question is then measured.

## Example

**User Question:** "When was the Eiffel Tower built?"

**Agentic System Response:** "The Eiffel Tower was completed in 1889 for the World's Fair in Paris."

**Generated Questions (from response):**
- "When was the Eiffel Tower built?" — matches the original
- "What year was the Eiffel Tower completed?" — close to the original
- "Why was the Eiffel Tower built?" — partial match

**Score:** high similarity = **0.92 (92%)**

---

**Irrelevant response:**

**Agentic System Response:** "The Eiffel Tower is 330 meters tall and has 3 floors."

**Generated Questions:**
- "How tall is the Eiffel Tower?" — does not match the original
- "How many floors does the Eiffel Tower have?" — does not match

**Score:** low similarity = **0.35 (35%)**

## Score Interpretation

|  Score  |                    Meaning                    |
|---------|-----------------------------------------------|
| 90-100% | Response directly answers the user's question |
| 70-90%  | Response mostly corresponds to the question   |
| 50-70%  | Response partially addresses the question     |
| 0-50%   | Response does not correspond to the question  |

## Algorithm

1. **Question Generation** — LLM creates questions that the given response could answer
2. **Embedding Creation** — questions are converted to numerical vectors
3. **Comparison** — cosine similarity between original and generated questions is calculated
4. **Averaging** — mean similarity value is computed

## Formula

```
score = mean(cosine_similarity(original_question, generated_questions))
```

Where:
- `original_question` — user's question
- `generated_questions` — hypothetical questions generated from the response
- `cosine_similarity` — similarity measure of two embeddings (from 0 to 1)

## References

- [RAGAS Documentation](https://github.com/vibrantlabsai/ragas/blob/main/docs/concepts/metrics/available_metrics/answer_relevance.md)

