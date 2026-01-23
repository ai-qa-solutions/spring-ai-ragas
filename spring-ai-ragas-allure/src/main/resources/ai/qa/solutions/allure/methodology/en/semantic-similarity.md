# Semantic Similarity Metric

## Glossary

- **Semantic Similarity** — measure of how close two texts are in meaning
- **Embedding** — numerical vector representation of text
- **Cosine Similarity** — measure of closeness between two vectors (from -1 to 1, typically 0 to 1 for text)
- **Reference** — the ground truth or expected answer

## Description

The metric evaluates how semantically similar the AI response is to the reference answer. Unlike metrics that use LLM judges, this metric relies solely on embedding models to compute vector representations of both texts and then calculates the cosine similarity between them.

This makes the metric:
- **Fast** — no LLM inference required, only embedding computation
- **Cost-effective** — embeddings are typically cheaper than LLM calls
- **Deterministic** — same texts always produce the same score (given the same embedding model)

## Example

**Reference:** "Paris is the capital of France."

**AI Response:** "The capital city of France is Paris."

**Score:** high similarity = **0.95 (95%)**

---

**Semantically different response:**

**AI Response:** "France is a country in Western Europe known for wine and cheese."

**Score:** moderate similarity = **0.65 (65%)**

---

**Completely unrelated response:**

**AI Response:** "Machine learning is a subset of artificial intelligence."

**Score:** low similarity = **0.25 (25%)**

## Score Interpretation

|  Score  |                   Meaning                   |
|---------|---------------------------------------------|
| 90-100% | Nearly identical meaning                    |
| 70-90%  | Similar meaning, same core information      |
| 50-70%  | Somewhat related, partial overlap           |
| 0-50%   | Different meanings, little semantic overlap |

## Important Notes

- **Contradictions may score high**: Contradictory statements about the same topic can have high semantic similarity because they discuss the same concepts. "The Earth is flat" and "The Earth is spherical" will score moderately high because both discuss the shape of Earth.
- **Length matters**: Very long responses compared to short references may have lower similarity due to embedding averaging effects.
- **Language consistency**: For best results, response and reference should be in the same language.

## Algorithm

1. **Input Texts** — receive response and reference texts
2. **Compute Embeddings** — convert both texts to vector representations using embedding models
3. **Compute Cosine Similarity** — calculate cosine similarity between the two vectors
4. **Apply Threshold (optional)** — if threshold is set, convert to binary 1.0 (pass) or 0.0 (fail)

## Formula

```
score = cosine_similarity(embed(response), embed(reference))
```

Where:
- `embed(text)` — vector representation of text from embedding model
- `cosine_similarity(a, b)` — (a · b) / (||a|| × ||b||)

With optional threshold:

```
final_score = raw_score >= threshold ? 1.0 : 0.0
```

## Configuration

| Parameter |  Type  |    Default    |                                  Description                                   |
|-----------|--------|---------------|--------------------------------------------------------------------------------|
| threshold | Double | null          | Optional threshold for binary pass/fail. If null, returns raw similarity score |
| models    | List   | all available | Embedding models to use for multi-model evaluation                             |

## References

- [Sentence Transformers Paper](https://arxiv.org/pdf/2108.06130.pdf)
- [RAGAS Documentation](https://github.com/explodinggradients/ragas)

