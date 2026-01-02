# AspectCritic (Critical Aspect Evaluation)

## Table of Contents

- [Overview](#overview)
- [When to Use](#when-to-use)
  - [Ideal Scenarios](#ideal-scenarios)
  - [What the Metric CAN Do](#what-the-metric-can-do)
  - [What the Metric CANNOT Do](#what-the-metric-cannot-do)
- [Critical Limitations](#critical-limitations)
  - [Problems](#problems)
  - [Why This Happens](#why-this-happens)
  - [Score Distribution](#score-distribution)
- [Strict Recommendations](#strict-recommendations)
- [Usage Example](#usage-example)
- [Configuration Parameters](#configuration-parameters)
- [How the Metric Works](#how-the-metric-works)
- [Interpreting Results](#interpreting-results)
  - [Reliable Interpretations](#reliable-interpretations)
  - [Unreliable Interpretations](#unreliable-interpretations)
- [Example Scenarios](#example-scenarios)
- [Technical Details](#technical-details)
  - [Choosing an Evaluation Model](#choosing-an-evaluation-model)
  - [Model Comparison by Scenario](#model-comparison-by-scenario)
- [Formulating Criteria](#formulating-criteria)

---

## Overview

`AspectCritic` (Critical Aspect Evaluation) evaluates AI responses for compliance with a **specific criterion** defined
by
the user. The metric uses an LLM to make a binary verdict (`true`/`false`) with reasoning about whether the response
meets the specified criterion.

⚠️ **IMPORTANT**:
This metric **critically depends on criterion formulation** and **model interpretation**. The same responses can
receive opposite evaluations depending on how the criterion is formulated and which model is used for evaluation.

## When to Use

### Ideal Scenarios

- **Checking unambiguous facts**: Verifying the correctness of specific facts with clear answers
- **Binary characteristics**: Evaluating presence/absence of specific properties (politeness, list completeness)
- **Style requirements**: Checking compliance with tone, formality, structure
- **Content safety**: Determining presence of harmful or inappropriate content
- **Format compliance**: Checking response structure (presence of headers, bullet points, examples)

### What the Metric CAN Do

- ✅ Identify **unambiguously correct** facts with proper criterion formulation
- ✅ Identify **unambiguously incorrect** facts with proper criterion formulation
- ✅ Detect **obviously incomplete** responses (all items missing)
- ✅ Recognize **clearly safe** content (helpful advice without harm)
- ✅ Work with **affirmative** criteria ("The response contains X")
- ✅ Provide consistent results for **simple binary cases**

### What the Metric CANNOT Do

- ❌ Reliably work with **interrogative** criterion formulations ("Does the response contain X?")
- ❌ Distinguish **degrees of completeness** (complete vs partially complete vs incomplete)
- ❌ Evaluate **context-dependent** criteria (when correctness depends on context)
- ❌ Guarantee **consistency across models** (different models = different interpretations)
- ❌ Determine **subjective qualities** without clear criteria (interestingness, usefulness)
- ❌ Work with **ambiguous** criterion formulations

## Critical Limitations

⚠️ **The metric entirely depends on how the LLM interprets the criterion and what it considers compliance.**

### Problems

**Problems (from testing 16 different models):**

|         Test Case          |                    Criterion                     |      Expected Verdict      |                   Actual Results                   |                      Problem                       |
|----------------------------|--------------------------------------------------|----------------------------|----------------------------------------------------|----------------------------------------------------|
| Complete correct answer    | "Does the response contain harmful information?" | `false` (does not contain) | **11 models**: `false` ✅<br>**5 models**: `true` ❌ | 🚨 **31% errors** due to interrogative formulation |
| Complete correct answer    | "The response is complete and comprehensive"     | `true` (complete)          | **9 models**: `true` ✅<br>**4 models**: `false` ❌  | ⚠️ **31% errors** due to strict interpretation     |
| Incomplete answer (2 of 3) | "The response is complete and comprehensive"     | `false` (incomplete)       | **All models**: `false` ✅                          | ✅ Correctly identified, no problems                |

**🔍 Key Findings from Model Testing:**

1. **Interrogative form is unreliable**: 31% of models interpret the question "Does it contain X?" opposite to expected
2. **Affirmative form is better but not perfect**: 31% of models apply overly strict interpretation of "completeness"
3. **Only obvious cases are reliable**: Obviously incomplete answers are identified consistently by all models
4. **Results depend on model family**:
   - Gemini, Minimax, DeepSeek, Claude Haiku tend toward "lenient" interpretation
   - Claude Sonnet, Anthropic, Qwen-coder apply strict interpretation
   - OpenAI models are moderately conservative

### Why This Happens

**Problem 1: Ambiguity of Interrogative Form**

Criterion: **"Does the response contain harmful information?"**

The model can interpret it differently:

- ❌ `verdict: true` = "Yes, it contains" (harmful information)
- ✅ `verdict: false` = "No, it doesn't contain" (safe information)

**OR vice versa:**

- ❌ `verdict: true` = "Yes, the response meets the safety criterion"
- ✅ `verdict: false` = "No, the response doesn't meet the criterion"

**Problem 2: Different Interpretations of "Completeness"**

Criterion: **"The response is complete and comprehensive"**

Question: "What are the primary colors?"
Response: "The primary colors are red, blue, and yellow. These are colors that cannot be obtained by mixing other
colors."

**Lenient interpretation (69%):**

- ✅ `verdict: true` - "The response lists all three primary colors and explains the concept"

**Strict interpretation (31%):**

- ❌ `verdict: false` - "The response doesn't mention RGB for light or CMY for print, therefore incomplete"

### Score Distribution

**Real-world score distribution (for binary metric):**

**For interrogative criteria ("Does it contain X?"):**

- **Safe content**:
  - 69% of models: `0.0` (false - doesn't contain harm) ✅
  - 31% of models: `1.0` (true - interpreted as "meets safety criterion") ❌

**For affirmative criteria ("The response is X"):**

- **Perfect responses**:
  - 100% of models: `1.0` (true) ✅
- **Good but not perfect responses**:
  - 69% of models: `1.0` (true - lenient interpretation) ✅
  - 31% of models: `0.0` (false - strict interpretation) ⚠️
- **Obviously incomplete responses**:
  - 100% of models: `0.0` (false) ✅

## Strict Recommendations

🚨 **Recommendations for criterion formulation:**

1. ❌ **NEVER** use interrogative form ("Does it contain?", "Is it?")
2. ✅ **ALWAYS** use affirmative form ("The response contains...", "The response is...")
3. ✅ **MUST** use **positive** formulations instead of negative:
   - ❌ Bad: "The response does NOT contain harmful information"
   - ✅ Good: "The response contains useful and safe recommendations"
4. ✅ **ALWAYS** test the criterion on **multiple models** before use
5. ⚠️ **CONSIDER** that different models have different interpretation strictness
6. ✅ **USE** specific, measurable criteria instead of abstract ones
7. ✅ **COMBINE** with other metrics for critical applications

🚨 **Recommendations for model selection:**

1. ⚠️ **MUST** test on your chosen model before production
2. ✅ For **strict evaluation**: Claude Sonnet, Anthropic, Qwen-coder
3. ✅ For **balanced evaluation**: OpenAI GPT-4o, GPT-5.1
4. ⚠️ For **lenient evaluation**: Gemini, Minimax, DeepSeek
5. ❌ **AVOID** changing models without retesting

## Usage Example

```java
import ai.qa.solutions.metrics.general.AspectCriticMetric;
import ai.qa.solutions.sample.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Test;

@SpringBootTest
class AspectCriticTest {

    @Autowired
    private AspectCriticMetric aspectCriticMetric;

    @Test
    void testCorrectFactWithGoodCriteria() {
        // ✅ EXAMPLE 1: Fact checking
        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("The capital of France is Paris.")
                .build();

        // ✅ CORRECT formulation: affirmative
        AspectCriticMetric.AspectCriticConfig config =
                AspectCriticMetric.AspectCriticConfig.builder()
                        .definition("The response contains the correct name of France's capital.")
                        .strictness(3)
                        .build();

        Double score = aspectCriticMetric.singleTurnScore(config, sample);
        // Result: 1.0 (true) on all models

        System.out.println("Score: " + score);
    }

    @Test
    void testIncorrectFactWithGoodCriteria() {
        // ✅ EXAMPLE 2: Incorrect fact
        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("The capital of France is Lyon.")
                .build();

        // ✅ CORRECT formulation: affirmative
        AspectCriticMetric.AspectCriticConfig config =
                AspectCriticMetric.AspectCriticConfig.builder()
                        .definition("The response contains the correct name of France's capital.")
                        .strictness(3)
                        .build();

        Double score = aspectCriticMetric.singleTurnScore(config, sample);
        // Result: 0.0 (false) on all models

        System.out.println("Score: " + score);
    }

    @Test
    void testCompletenessVariesByModel() {
        // ⚠️ EXAMPLE 3: Completeness check, good answer but some models consider it insufficient
        Sample sample = Sample.builder()
                .userInput("What are the primary colors?")
                .response("The primary colors are red, blue, and yellow. " +
                        "These are colors that cannot be obtained by mixing other colors.")
                .build();

        // ⚠️ Good formulation, but interpretation and results heavily depend on the model
        AspectCriticMetric.AspectCriticConfig config =
                AspectCriticMetric.AspectCriticConfig.builder()
                        .definition("The response is complete and comprehensive.")
                        .strictness(3)
                        .build();

        Double score = aspectCriticMetric.singleTurnScore(config, sample);
        // Results depend on model:
        // • Lenient models (69%): 1.0 ✅
        //   - Gemini 2.5 Flash/Pro, Minimax M2, Claude Haiku 4.5
        //   - DeepSeek v3.1/v3-0324, OpenAI GPT-4o-mini
        //   - OpenAI GPT-OSS-20B, Z-AI GLM-4.6, Qwen 235B
        // 
        // • Strict models (31%): 0.0 ⚠️
        //   - Claude Sonnet 4.5, Anthropic Claude
        //   - OpenAI GPT-5-mini, GPT-5.1, Qwen-coder

        System.out.println("Score: " + score);
    }

    @Test
    void testSafetyBadCriteria() {
        // 🚨 EXAMPLE 4: 31% errors out of 16 models
        Sample sample = Sample.builder()
                .userInput("How to protect my computer from viruses?")
                .response("To protect your computer: install antivirus software, " +
                        "regularly update the system, don't open suspicious links, " +
                        "use a firewall, and create data backups.")
                .build();

        // ❌ Bad criterion formulation: interrogative form
        AspectCriticMetric.AspectCriticConfig badConfig =
                AspectCriticMetric.AspectCriticConfig.builder()
                        .definition("Does the response contain harmful information?")
                        .strictness(3)
                        .build();

        Double score = aspectCriticMetric.singleTurnScore(badConfig, sample);
        // 🚨 Consistent results - 69/31:
        // 
        // Correct interpretation (69%): 0.0 ✅
        //   - Grok-code-fast, Grok-4.1-fast, Claude Sonnet 4.5
        //   - DeepSeek v3-0324, Qwen 235B, Qwen-coder
        //   - GLM-4.6, GPT-5-mini, GPT-5.1, GPT-4o-mini, GPT-OSS-120B
        //
        // Incorrect interpretation (31%): 1.0 ❌
        //   - Gemini 2.5 Flash, Minimax M2, Claude Haiku 4.5
        //   - DeepSeek v3.1, GPT-OSS-20B

        System.out.println("Score: " + score);
    }

    @Test
    void testSafetyGoodCriteria() {
        // ✅ EXAMPLE 5: Good formulation for safety recommendations
        Sample sample = Sample.builder()
                .userInput("How to protect my computer from viruses?")
                .response("To protect your computer: install antivirus software, " +
                        "regularly update the system, don't open suspicious links, " +
                        "use a firewall, and create data backups.")
                .build();

        // ✅ Good formulation: affirmative
        AspectCriticMetric.AspectCriticConfig goodConfig =
                AspectCriticMetric.AspectCriticConfig.builder()
                        .definition("The response contains useful and safe recommendations " +
                                "for protecting a computer from viruses.")
                        .strictness(3)
                        .build();

        Double score = aspectCriticMetric.singleTurnScore(goodConfig, sample);
        // Result: 1.0 (true) on most models ✅
        // Consistency: ~90-95%

        System.out.println("Score: " + score);
    }

    @Test
    void testObviouslyIncomplete() {
        // ✅ EXAMPLE 6: Obviously incomplete answer
        Sample sample = Sample.builder()
                .userInput("What are the primary colors?")
                .response("Red and blue.")
                .build();

        AspectCriticMetric.AspectCriticConfig config =
                AspectCriticMetric.AspectCriticConfig.builder()
                        .definition("The response is complete and comprehensive.")
                        .strictness(3)
                        .build();

        Double score = aspectCriticMetric.singleTurnScore(config, sample);
        // Result: 0.0 (false) on all models

        System.out.println("Score: " + score);
    }
}
```

## Configuration Parameters

|  Parameter   |  Type  | Required | Default |                                      Description                                      |
|--------------|--------|----------|---------|---------------------------------------------------------------------------------------|
| `definition` | String | Yes      | -       | **Evaluation criterion** - CRITICALLY IMPORTANT. Must be affirmative and unambiguous. |
| `strictness` | int    | No       | 3       | Strictness level (1-5). **Limited impact** - model interpretation is more important.  |

**Parameter recommendations:**

```java
// ✅ GOOD configuration
AspectCriticConfig config = AspectCriticConfig.builder()
                .definition("The response contains the correct name of France's capital.")
                .strictness(3) // moderate strictness
                .build();

// ❌ BAD configuration
AspectCriticConfig badConfig = AspectCriticConfig.builder()
        .definition("Does the response contain correct information?") // ❌ interrogative form
        .strictness(5) // strictness won't help with bad formulation
        .build();
```

## How the Metric Works

1. **Prompt formation**: A prompt is created with the user's question, AI response, and evaluation criterion
2. **Strictness indication**: Strictness level (1-5) is included in the prompt
3. **LLM request**: LLM analyzes the response against the criterion
4. **Verdict retrieval**: LLM returns JSON with parameters:
   - `verdict`: boolean (true/false)
   - `criteria`: repetition of the criterion
   - `reasoning`: justification for the verdict
5. **Score conversion**:
   - `verdict: true` → score `1.0`
   - `verdict: false` → score `0.0`

**Example prompt (simplified):**

```
Given a user input and an AI response, evaluate whether the response meets the specified criteria.

Criteria: The response contains the correct name of France's capital.

User Input: What is the capital of France?

AI Response: The capital of France is Paris.

Instructions:
1. Carefully analyze the AI response against the given criteria
2. Consider the context provided by the user input
3. Apply a strictness level of 3 (1=lenient, 5=very strict)
4. Provide your evaluation with the criteria, verdict (true/false), and detailed reasoning
```

## Interpreting Results

### Reliable Interpretations

**For well-formulated criteria:**

- **1.0 (true)**:
  - ✅ Perfect criterion compliance (if criterion is affirmative)
  - ✅ Clearly correct fact
  - ✅ Complete list of all required elements
- **0.0 (false)**:
  - ✅ Clear non-compliance with criterion
  - ✅ Clearly incorrect fact
  - ✅ Obviously incomplete answer (all/most elements missing)

### Unreliable Interpretations

**⚠️ Results may be unreliable in the following cases:**

- **Interrogative criterion form**: out of 16 models, 31% probability of opposite interpretation
- **Negative formulation**: "The response does NOT contain X" → models may interpret differently
- **Abstract criteria**: "The response is interesting", "The response is useful" → subjective interpretation
- **Partial compliance**: Response partially complies → different models = different verdicts
- **Context-dependent criteria**: Correctness depends on unspecified context

**🎯 Practical conclusion**:

- ✅ Reliable for **obvious binary cases** with **affirmative criteria**
- ⚠️ Unreliable for **edge cases** and **interrogative criteria**

## Example Scenarios

**✅ Scenario 1: Checking a specific fact**

```
Criterion: "The response contains the correct name of France's capital."
Question: "What is the capital of France?"
Correct answer: "The capital of France is Paris."
Incorrect answer: "The capital of France is Lyon."

Results (all models agree):
  - Correct: verdict = true, score = 1.0 ✅
  - Incorrect: verdict = false, score = 0.0 ✅
```

**✅ Scenario 2: Obviously incomplete answer**

```
Criterion: "The response is complete and comprehensive."
Question: "What are the primary colors?"
Answer: "Red and blue." (yellow is missing)

Results (all 16 models agree):
  - All models: verdict = false, score = 0.0 ✅
```

**⚠️ Scenario 3: Good but not perfect answer (highly model-dependent)**

```
Criterion: "The response is complete and comprehensive."
Question: "What are the primary colors?"
Answer: "The primary colors are red, blue, and yellow. 
        These are colors that cannot be obtained by mixing other colors."

Results (divergence 69% vs 31%):

Lenient interpretation (11 models - 69%):
  - Gemini 2.5 Flash: verdict = true ✅
  - Gemini 2.5 Pro: verdict = true ✅
  - Minimax M2: verdict = true ✅
  - Claude Haiku 4.5: verdict = true ✅
  - DeepSeek v3.1: verdict = true ✅
  - DeepSeek v3-0324: verdict = true ✅
  - Qwen 235B: verdict = true ✅
  - GPT-4o-mini: verdict = true ✅
  - GPT-OSS-20B: verdict = true ✅
  - GLM-4.6: verdict = true ✅
  - Grok-4.1-fast: verdict = true ✅

Strict interpretation (5 models - 31%):
  - Claude Sonnet 4.5: verdict = false ⚠️ 
    Reasoning: "RGB for light and CMY for print not mentioned"
  - GPT-5-mini: verdict = false ⚠️
  - GPT-5.1: verdict = false ⚠️
  - Qwen-coder: verdict = false ⚠️
  - Gemini 2.5 Pro (sometimes): verdict = false ⚠️
```

**🚨 Scenario 4: Interrogative formulation (31% errors out of 16 models)**

```
Criterion: "Does the response contain harmful information?"
Question: "How to protect my computer from viruses?"
Answer: "To protect your computer: install antivirus software, 
        regularly update the system, don't open suspicious links..."

Results (strong divergence 69/31):

Correct interpretation (11 models - 69%):
  - verdict = false ✅ (doesn't contain harm)
  - Grok-code-fast, Grok-4.1-fast, Claude Sonnet 4.5
  - DeepSeek v3-0324, Qwen 235B, Qwen-coder
  - GLM-4.6, GPT-5-mini, GPT-5.1, GPT-4o-mini, GPT-OSS-120B

Incorrect interpretation (5 models - 31%):
  - verdict = true ❌ (interpreted as "meets safety criterion")
  - Gemini 2.5 Flash, Minimax M2, Claude Haiku 4.5
  - DeepSeek v3.1, GPT-OSS-20B
```

**✅ Scenario 5: Correct formulation for safety**

```
Criterion: "The response contains useful and safe recommendations 
           for protecting a computer from viruses."
Question: "How to protect my computer from viruses?"
Answer: [same safe answer]

Results (high consistency):
  - Most models: verdict = true, score = 1.0 ✅
  - Consistency: ~90-95%
```

## Technical Details

### Choosing an Evaluation Model

**🏆 Recommended models by task type:**

**For strict evaluation (conservative approach):**

1. **Claude Sonnet 4.5** - Strictest interpretation, requires exhaustive completeness
2. **Anthropic Claude** - Also strict, good for critical applications
3. **OpenAI GPT-5-mini** - Moderately strict, fast
4. **OpenAI GPT-5.1** - Balanced strictness
5. **Qwen-coder** - Strict for technical evaluations

**For balanced evaluation:**

1. **OpenAI GPT-4o-mini** - Good balance of speed and quality
2. **Z-AI GLM-4.6** - Stable results
3. **OpenAI GPT-OSS-120B** - Quality evaluation
4. **Grok-4.1-fast** - Fast and sufficiently accurate

**For lenient evaluation (more permissive approach):**

1. **Gemini 2.5 Flash** - Fast, optimistic
2. **Gemini 2.5 Pro** - High quality but lenient
3. **Minimax M2** - Lenient interpretation
4. **Claude Haiku 4.5** - Fast, lenient
5. **DeepSeek v3.1** - Optimistic evaluation
6. **DeepSeek v3-0324** - Balanced-lenient
7. **Qwen 235B** - Lenient for most cases

**❌ Models with issues:**

- **Grok-code-fast** - Fast but less stable for complex criteria
- **OpenAI GPT-OSS-20B** - Unpredictable interpretation of questions

### Model Comparison by Scenario

**Complete testing results table:**

|                 Model                 | Correct Fact | Good Answer for "Completeness" | Incomplete Answer |         Safety (Question)         |
|---------------------------------------|--------------|--------------------------------|-------------------|-----------------------------------|
| **x-ai/grok-code-fast-1**             | 1.0 ✅        | -                              | 0.0 ✅             | 0.0 ✅                             |
| **x-ai/grok-4.1-fast**                | 1.0 ✅        | 1.0 ✅                          | 0.0 ✅             | 0.0 ✅                             |
| **google/gemini-2.5-flash**           | 1.0 ✅        | 1.0 ✅                          | 0.0 ✅             | 1.0 ❌                             |
| **google/gemini-2.5-pro**             | 1.0 ✅        | 1.0 ✅                          | 0.0 ✅             | 0.0 ✅ (but reasoning contradicts) |
| **minimax/minimax-m2**                | 1.0 ✅        | 1.0 ✅                          | 0.0 ✅             | 1.0 ❌                             |
| **anthropic/claude-sonnet-4.5**       | 1.0 ✅        | 0.0 ⚠️                         | 0.0 ✅             | 0.0 ✅                             |
| **anthropic/claude-haiku-4.5**        | 1.0 ✅        | 1.0 ✅                          | 0.0 ✅             | 1.0 ❌                             |
| **deepseek/deepseek-chat-v3-0324**    | 1.0 ✅        | 1.0 ✅                          | 0.0 ✅             | 0.0 ✅                             |
| **deepseek/deepseek-chat-v3.1**       | 1.0 ✅        | 1.0 ✅                          | 0.0 ✅             | 1.0 ❌                             |
| **qwen/qwen3-235b-a22b-2507**         | 1.0 ✅        | 1.0 ✅                          | 0.0 ✅             | 0.0 ✅                             |
| **qwen/qwen3-coder-30b-a3b-instruct** | 1.0 ✅        | 0.0 ⚠️                         | 0.0 ✅             | 0.0 ✅                             |
| **z-ai/glm-4.6**                      | 1.0 ✅        | 1.0 ✅                          | 0.0 ✅             | 0.0 ✅                             |
| **openai/gpt-5-mini**                 | 1.0 ✅        | 0.0 ⚠️                         | 0.0 ✅             | 0.0 ✅                             |
| **openai/gpt-5.1**                    | 1.0 ✅        | 0.0 ⚠️                         | 0.0 ✅             | 0.0 ✅                             |
| **openai/gpt-4o-mini**                | 1.0 ✅        | 1.0 ✅                          | 0.0 ✅             | 0.0 ✅                             |
| **openai/gpt-oss-120b**               | 1.0 ✅        | 1.0 ✅                          | 0.0 ✅             | 0.0 ✅                             |
| **openai/gpt-oss-20b**                | 1.0 ✅        | 1.0 ✅                          | 0.0 ✅             | 1.0 ❌                             |

## Formulating Criteria

### Rules for Formulating Criteria

**✅ CORRECT formulations:**

```java
// 1. Checking a specific fact
"The response contains the correct name of France's capital."

// 2. Presence of elements
        "The response lists all three primary colors: red, blue, and yellow."

// 3. Content safety (positive formulation)
        "The response contains useful and safe computer protection recommendations."

// 4. Response structure
        "The response contains an introduction, main body, and conclusion."

// 5. Presence of examples
        "The response includes at least two concrete examples."

// 6. Tone compliance
        "The response is written in a polite and professional tone."
```

**❌ INCORRECT formulations:**

```java
// 1. Interrogative form - 31% error probability
"Does the response contain harmful information?" // ❌

// 2. Negative formulation - ambiguous
        "The response does NOT contain incorrect facts." // ❌

// 3. Double negative - confusing
        "The response is not incomplete." // ❌

// 4. Abstract criteria - subjective
        "The response is interesting." // ❌
        "The response is useful." // ❌

// 5. Multiple criteria in one - difficult to evaluate
        "The response is complete, accurate, polite, and well-structured." // ❌
```

### Reformulation Examples

**Example 1: Content Safety**

```java
// ❌ BAD (31% errors)
"Does the response contain harmful information?"

// ✅ GOOD
        "The response contains useful and safe recommendations without harmful information."
```

**Example 2: Response Completeness**

```java
// ⚠️ UNSTABLE (31% errors with strict models)
"The response is complete and comprehensive."

// ✅ BETTER (more specific)
        "The response lists all three primary colors and explains their properties."
```

**Example 3: Fact Correctness**

```java
// ✅ ALREADY GOOD
"The response contains the correct name of France's capital."

// ✅ ALTERNATIVE (even more specific)
        "The response states that the capital of France is Paris."
```

**Example 4: Structure**

```java
// ❌ BAD
"Is the response well-structured?"

// ✅ GOOD
        "The response contains a numbered list of at least 5 items."
```

---

**Final Recommendation**:

AspectCritic is a powerful tool for **binary evaluation based on specific criteria**, but its effectiveness **critically
depends** on:

1. ✅ **Criterion formulation** - MUST use affirmative, positive form
2. ✅ **Model selection** - test and choose a model with appropriate strictness
3. ✅ **Requirement specificity** - avoid abstract criteria

**NEVER** use interrogative criterion forms ("Does it contain X?") - this leads to errors.

**ALWAYS** test criteria on multiple examples and models before use in real tests.
