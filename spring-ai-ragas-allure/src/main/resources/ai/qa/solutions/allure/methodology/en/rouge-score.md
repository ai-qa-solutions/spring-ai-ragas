# ROUGE Score Metric

## Glossary

- **ROUGE** — Recall-Oriented Understudy for Gisting Evaluation
- **ROUGE-1** — unigram (single word) overlap
- **ROUGE-2** — bigram (two consecutive words) overlap
- **ROUGE-L** — longest common subsequence (LCS) overlap
- **Precision** — how much of the response matches the reference
- **Recall** — how much of the reference is covered by the response
- **F-measure** — harmonic mean of precision and recall

## Description

ROUGE (Recall-Oriented Understudy for Gisting Evaluation) is a set of metrics for evaluating text similarity by measuring overlap between a candidate text and reference text. It was originally designed for evaluating automatic summarization but is widely used for various text generation tasks.

This metric is:
- **Non-LLM** — no language model calls required
- **Configurable** — supports different overlap types (ROUGE-1, ROUGE-2, ROUGE-L)
- **Recall-focused** — designed to measure how much of the reference is captured
- **Deterministic** — same inputs always produce the same score

## Example

**Reference:** "The quick brown fox jumps over the lazy dog."

**AI Response:** "The quick brown fox jumps over the lazy dog."

**ROUGE-L Score:** identical texts = **1.00 (100%)**

---

**Partial match:**

**AI Response:** "The quick brown fox runs."

**ROUGE-L Score:** partial sequence match = **0.55 (55%)**

---

**Reordered content:**

**AI Response:** "The lazy dog is jumped over by the quick brown fox."

**ROUGE-1 Score:** high word overlap = **0.89 (89%)**
**ROUGE-L Score:** lower LCS = **0.56 (56%)**

## Score Interpretation

|  Score  |         Meaning         |
|---------|-------------------------|
| 90-100% | Nearly complete overlap |
| 70-90%  | High overlap            |
| 50-70%  | Moderate overlap        |
| 30-50%  | Some overlap            |
| 0-30%   | Little to no overlap    |

## Important Notes

- **ROUGE-1 vs ROUGE-L**: ROUGE-1 ignores word order, ROUGE-L considers sequence
- **Precision vs Recall**: Short responses have high precision but low recall; long responses the opposite
- **F-measure balances both**: Typically the best single metric to report
- **Not semantic**: Like BLEU, measures surface-level overlap, not meaning

## Algorithm

### ROUGE-1 / ROUGE-2

1. **Tokenize** — split both texts into tokens
2. **Extract N-grams** — collect unigrams (ROUGE-1) or bigrams (ROUGE-2)
3. **Count Matches** — count overlapping n-grams
4. **Compute Scores** — calculate precision, recall, and F-measure

### ROUGE-L

1. **Tokenize** — split both texts into tokens
2. **Find LCS** — compute longest common subsequence
3. **Compute Scores** — precision = LCS/resp_len, recall = LCS/ref_len

## Formula

**Precision:**

```
P = |matched_ngrams| / |response_ngrams|
```

**Recall:**

```
R = |matched_ngrams| / |reference_ngrams|
```

**F-measure:**

```
F1 = 2 × (P × R) / (P + R)
```

## Configuration

| Parameter | Type | Default  |                Description                |
|-----------|------|----------|-------------------------------------------|
| rougeType | Enum | ROUGE_L  | Type of ROUGE: ROUGE_1, ROUGE_2, ROUGE_L  |
| mode      | Enum | FMEASURE | Scoring mode: PRECISION, RECALL, FMEASURE |

## References

- [ROUGE Paper (Lin, 2004)](https://aclanthology.org/W04-1013.pdf)
- [ROUGE Wikipedia](https://en.wikipedia.org/wiki/ROUGE_(metric))

