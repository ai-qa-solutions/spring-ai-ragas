# NLP Metrics

NLP metrics evaluate text similarity using traditional natural language processing algorithms. These are non-LLM metrics
that compute scores algorithmically, making them fast, deterministic, and cost-effective for large-scale evaluations.

## Overview

NLP metrics compare generated text against reference text using various similarity measures:

|      Metric       |   Type    |                Focus                 |
|-------------------|-----------|--------------------------------------|
| BLEU Score        | N-gram    | Precision-based n-gram overlap       |
| ROUGE Score       | N-gram    | Recall-based n-gram overlap          |
| chrF Score        | Character | Character n-gram similarity          |
| String Similarity | Distance  | Edit distance and character matching |

---

## BleuScore

> **Reference:** [BLEU: a Method for Automatic Evaluation of Machine Translation](https://aclanthology.org/P02-1040.pdf)

BLEU (Bilingual Evaluation Understudy) evaluates text quality by comparing n-gram precision with a reference.
Originally designed for machine translation, it's widely used for text generation evaluation.

### How It Works

1. **N-gram Extraction**: Extracts n-grams (n=1 to maxNgram) from response and reference
2. **Modified Precision**: Clips n-gram counts to avoid over-counting repeated n-grams
3. **Brevity Penalty**: Penalizes responses shorter than reference
4. **Geometric Mean**: Combines precisions across n-gram sizes

```java
// From BleuScoreMetric.java - BLEU calculation
class Example {
    double computeBleuScore(List<String> response, List<String> reference, int maxNgram) {
        // Compute modified n-gram precisions for each n
        List<Double> precisions = new ArrayList<>();
        for (int n = 1; n <= maxNgram; n++) {
            precisions.add(computeModifiedPrecision(response, reference, n));
        }

        // Brevity penalty
        double brevityPenalty = computeBrevityPenalty(response.size(), reference.size());

        // Geometric mean of precisions × brevity penalty
        double geometricMean = Math.exp(logSum / maxNgram);
        return brevityPenalty * geometricMean;
    }
}
```

### Example

```java
package ai.qa.solutions.metrics.nlp.en;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.nlp.BleuScoreMetric;
import ai.qa.solutions.sample.Sample;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Slf4j
class BleuScoreTest {

    private final BleuScoreMetric bleuScoreMetric = new BleuScoreMetric();

    @Test
    @DisplayName("BleuScore: High similarity translation")
    void testHighSimilarity() {
        Sample sample = Sample.builder()
                .response("The cat sat on the mat.")
                .reference("The cat is sitting on the mat.")
                .build();

        BleuScoreMetric.BleuScoreConfig config = BleuScoreMetric.BleuScoreConfig.builder()
                .maxNgram(4)
                .smoothing(true)
                .build();

        Double score = bleuScoreMetric.singleTurnScore(config, sample);

        log.info("BLEU Score: {}", score);
        assertTrue(score >= 0.5, "Expected moderate-high score for similar sentences");
    }

    @Test
    @DisplayName("BleuScore: Exact match")
    void testExactMatch() {
        Sample sample = Sample.builder()
                .response("Paris is the capital of France.")
                .reference("Paris is the capital of France.")
                .build();

        BleuScoreMetric.BleuScoreConfig config = BleuScoreMetric.BleuScoreConfig.builder()
                .maxNgram(4)
                .build();

        Double score = bleuScoreMetric.singleTurnScore(config, sample);

        log.info("BLEU Score: {}", score);
        assertTrue(score >= 0.99, "Expected perfect score for exact match");
    }

    @Test
    @DisplayName("BleuScore: No overlap")
    void testNoOverlap() {
        Sample sample = Sample.builder()
                .response("Hello world")
                .reference("Goodbye moon")
                .build();

        BleuScoreMetric.BleuScoreConfig config = BleuScoreMetric.BleuScoreConfig.builder()
                .maxNgram(4)
                .smoothing(false)
                .build();

        Double score = bleuScoreMetric.singleTurnScore(config, sample);

        log.info("BLEU Score: {}", score);
        assertTrue(score <= 0.1, "Expected low score for no overlap");
    }
}
```

### Configuration

|  Parameter  |  Type   | Required | Default |                    Description                    |
|-------------|---------|----------|---------|---------------------------------------------------|
| `maxNgram`  | int     | No       | 4       | Maximum n-gram size (standard BLEU uses 4)        |
| `smoothing` | boolean | No       | true    | Apply smoothing for short texts with zero n-grams |
| `language`  | String  | No       | `"en"`  | Language for explanations (`"en"`, `"ru"`)        |

### Score Interpretation

- **0.6-1.0**: High quality, very close to reference
- **0.4-0.6**: Good quality, captures main content
- **0.2-0.4**: Moderate quality, some overlap
- **0.0-0.2**: Poor quality, little similarity

### When to Use

- Machine translation evaluation
- Text summarization quality
- Paraphrase generation assessment
- Any precision-focused text comparison

---

## RougeScore

> **Reference:** [ROUGE: A Package for Automatic Evaluation of Summaries](https://aclanthology.org/W04-1013.pdf)

ROUGE (Recall-Oriented Understudy for Gisting Evaluation) measures text similarity using recall-based n-gram overlap.
It's the standard metric for text summarization evaluation.

### How It Works

Supports three variants:

1. **ROUGE-1**: Unigram overlap (single words)
2. **ROUGE-2**: Bigram overlap (word pairs)
3. **ROUGE-L**: Longest Common Subsequence

```java
// From RougeScoreMetric.java - ROUGE-L calculation
class Example {
    double computeRougeL(List<String> response, List<String> reference, Mode mode) {
        int lcsLength = computeLCS(response, reference);

        double recall = (double) lcsLength / reference.size();
        double precision = (double) lcsLength / response.size();

        return switch (mode) {
            case RECALL -> recall;
            case PRECISION -> precision;
            case FMEASURE -> 2 * precision * recall / (precision + recall);
        };
    }
}
```

### Example

```java
package ai.qa.solutions.metrics.nlp.en;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.nlp.RougeScoreMetric;
import ai.qa.solutions.sample.Sample;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Slf4j
class RougeScoreTest {

    private final RougeScoreMetric rougeScoreMetric = new RougeScoreMetric();

    @Test
    @DisplayName("RougeScore: ROUGE-L for summarization")
    void testRougeLSummarization() {
        Sample sample = Sample.builder()
                .response("AI helps doctors diagnose diseases faster.")
                .reference("Artificial intelligence is helping medical professionals diagnose diseases more quickly and accurately.")
                .build();

        RougeScoreMetric.RougeScoreConfig config = RougeScoreMetric.RougeScoreConfig.builder()
                .rougeType(RougeScoreMetric.RougeType.ROUGE_L)
                .mode(RougeScoreMetric.Mode.FMEASURE)
                .build();

        Double score = rougeScoreMetric.singleTurnScore(config, sample);

        log.info("ROUGE-L F1 Score: {}", score);
        assertTrue(score >= 0.3, "Expected moderate score for summarization");
    }

    @Test
    @DisplayName("RougeScore: ROUGE-1 unigram overlap")
    void testRouge1Unigram() {
        Sample sample = Sample.builder()
                .response("The quick brown fox jumps over the lazy dog")
                .reference("The fast brown fox leaps over a lazy dog")
                .build();

        RougeScoreMetric.RougeScoreConfig config = RougeScoreMetric.RougeScoreConfig.builder()
                .rougeType(RougeScoreMetric.RougeType.ROUGE_1)
                .mode(RougeScoreMetric.Mode.RECALL)
                .build();

        Double score = rougeScoreMetric.singleTurnScore(config, sample);

        log.info("ROUGE-1 Recall Score: {}", score);
        assertTrue(score >= 0.6, "Expected high recall for similar sentences");
    }

    @Test
    @DisplayName("RougeScore: ROUGE-2 bigram overlap")
    void testRouge2Bigram() {
        Sample sample = Sample.builder()
                .response("machine learning algorithms")
                .reference("machine learning algorithms and deep learning models")
                .build();

        RougeScoreMetric.RougeScoreConfig config = RougeScoreMetric.RougeScoreConfig.builder()
                .rougeType(RougeScoreMetric.RougeType.ROUGE_2)
                .mode(RougeScoreMetric.Mode.PRECISION)
                .build();

        Double score = rougeScoreMetric.singleTurnScore(config, sample);

        log.info("ROUGE-2 Precision Score: {}", score);
        assertTrue(score >= 0.5, "Expected high precision for subset bigrams");
    }
}
```

### Configuration

|  Parameter  |   Type    | Required | Default  |                 Description                 |
|-------------|-----------|----------|----------|---------------------------------------------|
| `rougeType` | RougeType | No       | ROUGE_L  | ROUGE_1, ROUGE_2, or ROUGE_L variant        |
| `mode`      | Mode      | No       | FMEASURE | RECALL, PRECISION, or FMEASURE scoring mode |
| `language`  | String    | No       | `"en"`   | Language for explanations (`"en"`, `"ru"`)  |

### When to Use

- Text summarization evaluation
- Document similarity assessment
- Content coverage measurement
- Any recall-focused text comparison

---

## ChrfScore

> **Reference:** [chrF: character n-gram F-score for automatic MT evaluation](https://aclanthology.org/W15-3049.pdf)

chrF (Character n-gram F-score) evaluates text using character-level n-grams, making it more robust to morphological
variations, typos, and agglutinative languages than word-based metrics.

### How It Works

1. **Character N-grams**: Extracts character n-grams (default n=1 to 6)
2. **Word N-grams (optional)**: chrF++ mode adds word n-grams
3. **F-beta Score**: Computes weighted F-score with configurable beta

```java
// From ChrfScoreMetric.java - chrF calculation
class Example {
    double computeChrfScore(String response, String reference, int charNgramOrder, int wordNgramOrder, double beta) {
        // Character n-gram F-score
        double charFScore = 0.0;
        for (int n = 1; n <= charNgramOrder; n++) {
            charFScore += computeCharNgramFScore(response, reference, n, beta);
        }
        charFScore /= charNgramOrder;

        // Optional word n-grams for chrF++ mode
        if (wordNgramOrder > 0) {
            double wordFScore = computeWordNgramFScore(responseTokens, referenceTokens, wordNgramOrder, beta);
            return (charFScore + wordFScore) / 2.0;
        }

        return charFScore;
    }
}
```

### Example

```java
package ai.qa.solutions.metrics.nlp.en;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.nlp.ChrfScoreMetric;
import ai.qa.solutions.sample.Sample;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Slf4j
class ChrfScoreTest {

    private final ChrfScoreMetric chrfScoreMetric = new ChrfScoreMetric();

    @Test
    @DisplayName("ChrfScore: Robust to minor typos")
    void testRobustToTypos() {
        Sample sample = Sample.builder()
                .response("The qucik brown fox jumps over the lazy dog")  // typo: qucik
                .reference("The quick brown fox jumps over the lazy dog")
                .build();

        ChrfScoreMetric.ChrfScoreConfig config = ChrfScoreMetric.ChrfScoreConfig.builder()
                .charNgramOrder(6)
                .beta(2.0)
                .build();

        Double score = chrfScoreMetric.singleTurnScore(config, sample);

        log.info("chrF Score: {}", score);
        assertTrue(score >= 0.9, "Expected high score despite minor typo");
    }

    @Test
    @DisplayName("ChrfScore: chrF++ mode with word n-grams")
    void testChrfPlusPlus() {
        Sample sample = Sample.builder()
                .response("Natural language processing is fascinating")
                .reference("Natural language processing is interesting and fascinating")
                .build();

        ChrfScoreMetric.ChrfScoreConfig config = ChrfScoreMetric.ChrfScoreConfig.builder()
                .charNgramOrder(6)
                .wordNgramOrder(2)  // Enable chrF++ mode
                .beta(2.0)
                .build();

        Double score = chrfScoreMetric.singleTurnScore(config, sample);

        log.info("chrF++ Score: {}", score);
        assertTrue(score >= 0.7, "Expected good score for similar text");
    }

    @Test
    @DisplayName("ChrfScore: Morphological variations")
    void testMorphologicalVariations() {
        Sample sample = Sample.builder()
                .response("running quickly")
                .reference("runs quick")
                .build();

        ChrfScoreMetric.ChrfScoreConfig config = ChrfScoreMetric.ChrfScoreConfig.builder()
                .charNgramOrder(6)
                .build();

        Double score = chrfScoreMetric.singleTurnScore(config, sample);

        log.info("chrF Score: {}", score);
        assertTrue(score >= 0.5, "Expected moderate score for morphological variations");
    }
}
```

### Configuration

|    Parameter     |  Type  | Required | Default |                  Description                   |
|------------------|--------|----------|---------|------------------------------------------------|
| `charNgramOrder` | int    | No       | 6       | Maximum character n-gram order (standard is 6) |
| `wordNgramOrder` | int    | No       | 0       | Word n-gram order (0 = chrF, >0 = chrF++)      |
| `beta`           | double | No       | 2.0     | F-beta weight (higher = more recall weight)    |
| `language`       | String | No       | `"en"`  | Language for explanations (`"en"`, `"ru"`)     |

### When to Use

- Morphologically rich languages
- Texts with potential typos or OCR errors
- Agglutinative language evaluation
- Character-level text comparison

---

## StringSimilarity

StringSimilarity provides multiple classic string distance algorithms for text comparison.

### Available Algorithms

|  Algorithm   |                     Description                      |
|--------------|------------------------------------------------------|
| LEVENSHTEIN  | Edit distance (insertions, deletions, substitutions) |
| HAMMING      | Substitution-only distance (equal length strings)    |
| JARO         | Character matching with transposition handling       |
| JARO_WINKLER | Jaro with prefix bonus for matching starts           |

### How It Works

```java
// From StringSimilarityMetric.java - Levenshtein similarity
class Example {
    double computeLevenshteinSimilarity(String s1, String s2) {
        int distance = levenshteinDistance.apply(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());
        return 1.0 - ((double) distance / maxLength);
    }
}
```

### Example

```java
package ai.qa.solutions.metrics.nlp.en;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.nlp.StringSimilarityMetric;
import ai.qa.solutions.sample.Sample;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Slf4j
class StringSimilarityTest {

    private final StringSimilarityMetric stringSimilarityMetric = new StringSimilarityMetric();

    @Test
    @DisplayName("StringSimilarity: Levenshtein distance")
    void testLevenshtein() {
        Sample sample = Sample.builder()
                .response("kitten")
                .reference("sitting")
                .build();

        StringSimilarityMetric.StringSimilarityConfig config =
                StringSimilarityMetric.StringSimilarityConfig.builder()
                        .distanceMeasure(StringSimilarityMetric.DistanceMeasure.LEVENSHTEIN)
                        .caseSensitive(false)
                        .build();

        Double score = stringSimilarityMetric.singleTurnScore(config, sample);

        log.info("Levenshtein Similarity: {}", score);
        assertTrue(score >= 0.4 && score <= 0.6, "Expected moderate similarity");
    }

    @Test
    @DisplayName("StringSimilarity: Jaro-Winkler for names")
    void testJaroWinkler() {
        Sample sample = Sample.builder()
                .response("MARTHA")
                .reference("MARHTA")
                .build();

        StringSimilarityMetric.StringSimilarityConfig config =
                StringSimilarityMetric.StringSimilarityConfig.builder()
                        .distanceMeasure(StringSimilarityMetric.DistanceMeasure.JARO_WINKLER)
                        .caseSensitive(false)
                        .build();

        Double score = stringSimilarityMetric.singleTurnScore(config, sample);

        log.info("Jaro-Winkler Similarity: {}", score);
        assertTrue(score >= 0.9, "Expected high similarity for transposed characters");
    }

    @Test
    @DisplayName("StringSimilarity: Case sensitivity")
    void testCaseSensitivity() {
        Sample sample = Sample.builder()
                .response("Hello World")
                .reference("hello world")
                .build();

        // Case insensitive
        StringSimilarityMetric.StringSimilarityConfig insensitiveConfig =
                StringSimilarityMetric.StringSimilarityConfig.builder()
                        .distanceMeasure(StringSimilarityMetric.DistanceMeasure.LEVENSHTEIN)
                        .caseSensitive(false)
                        .build();

        Double insensitiveScore = stringSimilarityMetric.singleTurnScore(insensitiveConfig, sample);

        // Case sensitive
        StringSimilarityMetric.StringSimilarityConfig sensitiveConfig =
                StringSimilarityMetric.StringSimilarityConfig.builder()
                        .distanceMeasure(StringSimilarityMetric.DistanceMeasure.LEVENSHTEIN)
                        .caseSensitive(true)
                        .build();

        Double sensitiveScore = stringSimilarityMetric.singleTurnScore(sensitiveConfig, sample);

        log.info("Case insensitive: {}, Case sensitive: {}", insensitiveScore, sensitiveScore);
        assertTrue(insensitiveScore > sensitiveScore, "Case insensitive should score higher");
    }
}
```

### Configuration

|     Parameter     |      Type       | Required |   Default    |                Description                 |
|-------------------|-----------------|----------|--------------|--------------------------------------------|
| `distanceMeasure` | DistanceMeasure | No       | JARO_WINKLER | Algorithm to use for comparison            |
| `caseSensitive`   | boolean         | No       | false        | Whether comparison is case sensitive       |
| `language`        | String          | No       | `"en"`       | Language for explanations (`"en"`, `"ru"`) |

### Algorithm Selection Guide

|        Use Case         |  Algorithm   |                  Why                   |
|-------------------------|--------------|----------------------------------------|
| General text comparison | LEVENSHTEIN  | Most versatile, handles all edit types |
| Name matching           | JARO_WINKLER | Prefix bonus helps with name variants  |
| DNA sequences           | HAMMING      | Only substitutions, equal length       |
| Short strings           | JARO         | Good for short strings without prefix  |

---

## Choosing the Right Metric

|          Use Case          |      Metric       |                 Why                  |
|----------------------------|-------------------|--------------------------------------|
| Machine translation        | BLEU              | Standard MT evaluation metric        |
| Text summarization         | ROUGE             | Focus on recall of reference content |
| Multilingual/morphological | chrF              | Character-level robustness           |
| Exact string matching      | StringSimilarity  | Direct distance measurement          |
| Typo detection             | chrF, Levenshtein | Both handle character errors         |
| Name/entity matching       | Jaro-Winkler      | Optimized for name variations        |

---

## Sample Schema

All NLP metrics use `response` and `reference` fields:

```java
class Example {
    void createSample() {
        Sample sample = Sample.builder()
                .response("Generated text to evaluate")
                .reference("Reference (ground truth) text")
                .build();
    }
}
```

|    Field    |  Type  | Required |        Description         |
|-------------|--------|----------|----------------------------|
| `response`  | String | Yes      | Generated text to evaluate |
| `reference` | String | Yes      | Ground truth reference     |

---

## Rich Evaluation API

All NLP metrics support `singleTurnEvaluate()` returning `EvaluationResult` with score, explanation, and metadata — no LLM calls required:

```java
import ai.qa.solutions.metric.EvaluationResult;

// Instead of Double score = metric.singleTurnScore(config, sample);
EvaluationResult result = bleuScoreMetric.singleTurnEvaluate(config, sample);

log.info("Score: {}", result.getScore());
log.info("Explanation: {}", result.getExplanation().getSimpleDescription());
log.info("Duration: {}ms", result.getTotalDuration().toMillis());

// Async variant
CompletableFuture<EvaluationResult> future =
        bleuScoreMetric.singleTurnEvaluateAsync(config, sample);

// Russian language explanations
BleuScoreMetric.BleuScoreConfig ruConfig = BleuScoreMetric.BleuScoreConfig.builder()
        .maxNgram(4)
        .language("ru")
        .build();
EvaluationResult ruResult = bleuScoreMetric.singleTurnEvaluate(ruConfig, sample);
```

