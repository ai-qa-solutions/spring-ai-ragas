# chrF Score Metric

## Glossary

- **chrF** — Character n-gram F-score
- **chrF++** — chrF with additional word n-grams
- **Character n-gram** — a contiguous sequence of n characters
- **Word n-gram** — a contiguous sequence of n words
- **Beta** — weight parameter favoring recall over precision

## Description

chrF (Character n-gram F-score) measures text similarity at the character level, computing F-score over character n-grams. chrF++ extends this by also including word n-grams. Character-level metrics are particularly useful for:

- **Morphologically rich languages** — captures partial word matches
- **Languages without clear word boundaries** — Chinese, Japanese, Thai
- **Typo detection** — small spelling variations still score well
- **Translation evaluation** — robust to morphological variations

This metric is:
- **Non-LLM** — no language model calls required
- **Robust to typos** — partial character matches contribute to score
- **Language-flexible** — works well even without tokenization
- **Deterministic** — same inputs always produce the same score

## Example

**Reference:** "The quick brown fox jumps over the lazy dog."

**AI Response:** "The quick brown fox jumps over the lazy dog."

**chrF Score:** identical texts = **1.00 (100%)**

---

**British vs American spelling:**

**Reference:** "colour behaviour favour"

**AI Response:** "color behavior favor"

**chrF Score:** high character overlap despite different words = **0.75 (75%)**

---

**Common typos:**

**Reference:** "receive occasion accommodate"

**AI Response:** "recieve occassion accomodate"

**chrF Score:** robust to typos = **0.80 (80%)**

## Score Interpretation

|  Score  |               Meaning               |
|---------|-------------------------------------|
| 90-100% | Nearly identical at character level |
| 70-90%  | High character overlap              |
| 50-70%  | Moderate overlap                    |
| 30-50%  | Some character sequences match      |
| 0-30%   | Little character-level similarity   |

## Important Notes

- **Character-level granularity**: Even partial word matches contribute to the score
- **Beta=2 by default**: Weights recall twice as important as precision
- **chrF++ recommended**: Including word n-grams generally improves correlation with human judgments
- **No tokenization needed**: Works directly on character sequences

## Algorithm

1. **Extract Character N-grams** — for n from 1 to charNgramOrder, extract all character n-grams
2. **Extract Word N-grams (chrF++)** — if wordNgramOrder > 0, also extract word n-grams
3. **Count Matches** — count overlapping n-grams between response and reference
4. **Compute Precision and Recall** — for each n-gram order
5. **Combine with F-score** — using beta parameter to weight recall

## Formula

```
chrF_β = (1 + β²) × (P × R) / (β² × P + R)
```

Where:
- `P` — average precision across all character (and word) n-gram orders
- `R` — average recall across all character (and word) n-gram orders
- `β` — weight parameter (default 2.0, favors recall)

## Configuration

|   Parameter    |  Type   | Default |                       Description                       |
|----------------|---------|---------|---------------------------------------------------------|
| charNgramOrder | Integer | 6       | Maximum character n-gram order                          |
| wordNgramOrder | Integer | 0       | Word n-gram order (0=chrF, >0=chrF++)                   |
| beta           | Double  | 2.0     | F-score beta parameter (higher = more weight on recall) |

## References

- [chrF Paper (Popović, 2015)](https://aclanthology.org/W15-3049.pdf)
- [chrF++ Paper (Popović, 2017)](https://aclanthology.org/W17-4770.pdf)

