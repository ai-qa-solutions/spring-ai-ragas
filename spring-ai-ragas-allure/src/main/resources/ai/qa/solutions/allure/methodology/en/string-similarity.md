# String Similarity Metric

## Glossary

- **Edit Distance** — minimum number of operations to transform one string into another
- **Levenshtein Distance** — edit distance allowing insert, delete, replace operations
- **Hamming Distance** — number of positions where characters differ (equal-length strings)
- **Jaro Similarity** — similarity based on matching characters and transpositions
- **Jaro-Winkler** — Jaro with bonus for matching prefix

## Description

String Similarity metrics measure how similar two strings are using various edit distance algorithms. These metrics are useful for exact or near-exact matching scenarios such as:

- **Entity matching** — comparing names, identifiers
- **Typo detection** — finding misspellings
- **Short answer evaluation** — comparing brief factual responses
- **Deduplication** — finding similar strings in datasets

This metric is:
- **Non-LLM** — no language model calls required
- **Fast** — simple string operations
- **Configurable** — multiple algorithms available
- **Deterministic** — same inputs always produce the same score

## Algorithms

### Levenshtein Distance

Counts minimum edit operations (insert, delete, replace) to transform one string into another.

**Example:** "hello" → "hallo" requires 1 operation (replace 'e' with 'a')
**Similarity:** 1 - (1/5) = 0.80 (80%)

### Jaro Similarity

Based on the number and order of matching characters. A character matches if it's the same and within a certain distance.

**Formula:** (m/|s₁| + m/|s₂| + (m-t)/m) / 3
- m = matching characters
- t = transpositions / 2

### Jaro-Winkler

Adds a prefix bonus to Jaro similarity for strings that match from the beginning. Useful for names where prefix typically carries more significance.

**Formula:** Jaro + (prefix_length × p × (1 - Jaro))
- p = scaling factor (typically 0.1)

### Hamming Distance

Simple position-wise comparison. Requires equal-length strings.

**Example:** "hello" vs "hallo" → 1 different position
**Similarity:** 1 - (1/5) = 0.80 (80%)

## Example

**Reference:** "hello"

**AI Response:** "hello"

**Score:** identical = **1.00 (100%)**

---

**Single character difference:**

**AI Response:** "hallo"

**Levenshtein:** 0.80 (80%)
**Jaro-Winkler:** 0.88 (88%) — prefix bonus for "h"

---

**Completely different:**

**AI Response:** "world"

**Levenshtein:** 0.20 (20%)
**Jaro:** 0.47 (47%)

## Score Interpretation

|  Score  |         Meaning          |
|---------|--------------------------|
| 90-100% | Nearly identical strings |
| 70-90%  | Very similar (few edits) |
| 50-70%  | Some similarity          |
| 30-50%  | Weak similarity          |
| 0-30%   | Very different strings   |

## Important Notes

- **Not semantic**: "happy" and "glad" score 0 despite same meaning
- **Case sensitivity**: By default case-insensitive; can be configured
- **Jaro-Winkler for names**: Best for comparing person/place names
- **Levenshtein for general use**: Most versatile algorithm

## Configuration

|    Parameter    |  Type   |   Default    |                     Description                     |
|-----------------|---------|--------------|-----------------------------------------------------|
| distanceMeasure | Enum    | JARO_WINKLER | Algorithm: LEVENSHTEIN, HAMMING, JARO, JARO_WINKLER |
| caseSensitive   | Boolean | false        | Whether comparison is case-sensitive                |

## References

- [Levenshtein Distance](https://en.wikipedia.org/wiki/Levenshtein_distance)
- [Jaro-Winkler](https://en.wikipedia.org/wiki/Jaro%E2%80%93Winkler_distance)

