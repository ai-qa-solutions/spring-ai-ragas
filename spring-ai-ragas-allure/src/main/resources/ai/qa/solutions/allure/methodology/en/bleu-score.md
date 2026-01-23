# BLEU Score Metric

## Glossary

- **BLEU** — Bilingual Evaluation Understudy, a metric for evaluating text quality
- **N-gram** — a contiguous sequence of n items from a text (e.g., unigram=1 word, bigram=2 words)
- **Precision** — ratio of matching n-grams in response to total n-grams in response
- **Brevity Penalty** — penalty applied when response is shorter than reference

## Description

BLEU (Bilingual Evaluation Understudy) measures the similarity between a candidate text (response) and a reference text by computing n-gram precision scores. Originally designed for machine translation evaluation, it's widely used for text generation quality assessment.

This metric is:
- **Non-LLM** — no language model calls required, purely algorithmic
- **Fast** — simple n-gram counting operations
- **Deterministic** — same inputs always produce the same score
- **Language-agnostic** — works with any language that can be tokenized

## Example

**Reference:** "The quick brown fox jumps over the lazy dog."

**AI Response:** "The quick brown fox jumps over the lazy dog."

**Score:** identical texts = **1.00 (100%)**

---

**Similar response:**

**AI Response:** "The quick brown fox jumped over the lazy dog."

**Score:** one word different = **0.85 (85%)**

---

**Different response:**

**AI Response:** "Machine learning requires data."

**Score:** no overlap = **0.05 (5%)**

## Score Interpretation

|  Score  |        Meaning         |
|---------|------------------------|
| 90-100% | Nearly identical texts |
| 70-90%  | High n-gram overlap    |
| 50-70%  | Moderate overlap       |
| 30-50%  | Some common phrases    |
| 0-30%   | Little to no overlap   |

## Important Notes

- **Word order matters**: Unlike some metrics, BLEU considers word order through n-grams
- **Brevity is penalized**: Short responses are penalized even if all words match
- **Smoothing helps**: For short texts, smoothing prevents zero scores when higher-order n-grams don't match
- **Not semantic**: BLEU measures surface-level overlap, not meaning ("happy" and "glad" score 0)

## Algorithm

1. **Tokenize** — split both texts into tokens (words)
2. **Compute N-gram Precision** — for each n from 1 to max_ngram, calculate matching n-grams
3. **Apply Brevity Penalty** — if response is shorter than reference, apply penalty
4. **Combine Scores** — geometric mean of precisions multiplied by brevity penalty

## Formula

```
BLEU = BP × exp(Σ wₙ × log(pₙ))
```

Where:
- `BP` — brevity penalty = min(1, exp(1 - ref_len/resp_len))
- `pₙ` — modified precision for n-grams of length n
- `wₙ` — weight for each n-gram (typically 1/max_ngram)

## Configuration

| Parameter |  Type   | Default |             Description              |
|-----------|---------|---------|--------------------------------------|
| maxNgram  | Integer | 4       | Maximum n-gram order (1-4 typical)   |
| smoothing | Boolean | true    | Use smoothing for zero-count n-grams |

## References

- [BLEU Paper (Papineni et al., 2002)](https://aclanthology.org/P02-1040.pdf)
- [BLEU Wikipedia](https://en.wikipedia.org/wiki/BLEU)

