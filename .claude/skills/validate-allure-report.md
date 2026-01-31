# Validate Allure Report Skill

**Trigger**: `/validate-allure-report`

## Purpose

Validate Allure report generation by running tests and checking the generated reports:
1. Execute a metric integration test
2. Find generated Allure attachments
3. Validate report structure follows 4-questions pattern
4. Check CSS colors, conversation blocks, step explanations

## Usage

```
/validate-allure-report <test-pattern>
/validate-allure-report EnAgentGoalAccuracyIntegrationIT
/validate-allure-report *FaithfulnessIntegrationIT#testHighScore
/validate-allure-report ru/RuTopicAdherenceIntegrationIT
```

## Instructions for Claude

When this skill is invoked:

---

## Phase 1: Run Integration Test

Execute the specified test and capture Allure results:

```bash
# Run single test class
mvn verify -pl spring-ai-ragas-spring-boot -P integration-tests \
    -Dtest=<TestClass> -DfailIfNoTests=false 2>&1 | tee /tmp/test-output.log

# Check test result
grep -E "(Tests run|BUILD|FAIL)" /tmp/test-output.log
```

**Capture test execution details:**
- Test class name
- Test method name (if specified)
- Metric type being tested
- Models configured in test
- Sample data used

---

## Phase 2: Find Generated Reports

After test execution, locate generated Allure attachments:

```bash
# Find newest HTML/MD attachments
ls -lt spring-ai-ragas-spring-boot/target/allure-results/*-attachment.html | head -5
ls -lt spring-ai-ragas-spring-boot/target/allure-results/*-attachment.md | head -5
```

**Match report to test:**
1. Find newest files (created during test run)
2. Extract metric type from JSON in HTML
3. Verify it matches the test that was run

---

## Phase 3: Extract Report Data

### 3.1 From HTML: Extract JSON

```bash
# Get explanation data JSON
grep -A 500 '<script id="explanation-data"' <file> | head -600
```

### 3.2 Parse Key Fields

Extract from JSON:
- `metricType` - Which metric generated this
- `summary` - Sample data used
- `steps[]` - Step-by-step explanation
- `interpretation` - Score calculation
- `executionSteps` - Raw execution log

---

## Phase 4: Validate 4 Questions Per Step

### Core Principle: Each step must answer 4 questions

For each step in Score Explanation, validate:

| # |                Question                |            JSON Field             |          What User Should See           |
|---|----------------------------------------|-----------------------------------|-----------------------------------------|
| 1 | **What is being checked?** (with data) | `step.inputData`                  | Actual text/conversation being analyzed |
| 2 | **Why / what criteria?**               | `step.title` + `step.description` | Purpose of this step                    |
| 3 | **By which models?** (for LLM steps)   | `step.modelResults[].modelId`     | List of models that ran                 |
| 4 | **What did they answer?**              | `step.items[]` with `source`      | Each model's response                   |

### Q1: What is being checked? (inputData)

|       Check       |                Valid If                 |
|-------------------|-----------------------------------------|
| inputData present | Non-empty string showing actual content |
| Shows source text | Contains the text that's being analyzed |

**Examples by metric:**
| Metric | Step | inputData should contain |
|--------|------|--------------------------|
| faithfulness | ExtractStatements | Response text |
| faithfulness | VerifyStatements | Contexts |
| agent-goal-accuracy | InferGoal | Conversation messages |
| agent-goal-accuracy | EvaluateOutcome | Inferred goal text |
| context-recall | ClassifyStatements | Reference answer |

### Q2: Why / what criteria? (title + description)

|    Check    |           Valid If            |
|-------------|-------------------------------|
| Title       | Describes what this step does |
| Description | Explains the purpose/criteria |

### Q3: By which models? (modelResults)

|        Check        |             Valid If             |
|---------------------|----------------------------------|
| For LLM steps       | All configured LLM models listed |
| For EMBEDDING steps | All embedding models listed      |
| For COMPUTE steps   | N/A (pure computation)           |

### Q4: What did they answer? (items[] with source)

|      Check      |              Valid If               |
|-----------------|-------------------------------------|
| items[] array   | Not empty for evaluation steps      |
| items[].source  | Contains model ID                   |
| items[].content | Model's generated text              |
| items[].verdict | Pass/fail judgment where applicable |
| items[].reason  | Reasoning for the verdict           |

---

## Phase 5: Validate Conversation Block

### For Single-Turn Metrics:

Check summary contains:
- `userInput` - User's question
- `response` - AI's response
- `reference` - Ground truth (if applicable)
- `retrievedContexts` - Retrieved docs (for RAG metrics)

### For Multi-Turn Metrics (Agent):

Check conversation display:

|    Element     |     HTML Class      |         Required         |
|----------------|---------------------|--------------------------|
| Message list   | `conversation-list` | Yes                      |
| Human messages | `message-human`     | At least 1               |
| AI messages    | `message-ai`        | At least 1               |
| Tool messages  | `message-tool`      | If ToolMessage in sample |
| Tool calls     | `tool-calls`        | If ToolCall in AIMessage |

### Tool Call Format:

```html
<div class="tool-calls">
    <span class="tool-calls-label">Tool Calls:</span>
    <code class="tool-call">tool_name({"arg": "value"})</code>
</div>
```

---

## Phase 6: Validate CSS Colors

### Score Color Classes:

| Score Range |  Expected Class   |     Color      |
|-------------|-------------------|----------------|
| >= 0.8      | `score-excellent` | Green #09a232  |
| >= 0.6      | `score-good`      | Green #09a232  |
| >= 0.4      | `score-moderate`  | Orange #eb9b46 |
| < 0.4       | `score-poor`      | Red #eb5146    |

### Pass/Fail Item Classes:

|    State     |  Expected Class   |
|--------------|-------------------|
| passed=true  | `.passed` (green) |
| passed=false | `.failed` (red)   |
| passed=null  | `.neutral` (gray) |

---

## Phase 7: Metric-Specific Validation

### agent-goal-accuracy

**Mode Detection:**
- WITH_REFERENCE: 1 step (CompareOutcome)
- WITHOUT_REFERENCE: 2 steps (InferGoal + EvaluateOutcome)

**WITHOUT_REFERENCE checks:**
| Step | Q4 should contain |
|------|-------------------|
| InferGoal | items[].content = inferred goal text |
| EvaluateOutcome | items[].verdict = ACHIEVED/NOT ACHIEVED |

**WITH_REFERENCE checks:**
| Step | Q4 should contain |
|------|-------------------|
| CompareOutcome | items[].verdict = ACHIEVED/NOT ACHIEVED |

### faithfulness

|       Step        |          Q4 items contain          |
|-------------------|------------------------------------|
| ExtractStatements | Extracted statements (passed=null) |
| VerifyStatements  | FAITHFUL/UNFAITHFUL verdicts       |

### tool-call-accuracy

|         Check          |         Expected         |
|------------------------|--------------------------|
| Precision/Recall shown | In interpretation        |
| Matched calls          | Listed with match scores |

### topic-adherence

|       Step        |     Q4 items contain      |
|-------------------|---------------------------|
| ExtractTopics     | Extracted topics          |
| EvaluateAdherence | ADHERES/DEVIATES verdicts |

---

## Phase 8: Output Report

### Success Format:

```
═══════════════════════════════════════════════════════════════
                    ALLURE REPORT VALIDATION
═══════════════════════════════════════════════════════════════

Test Executed: EnAgentGoalAccuracyIntegrationIT#testGoalAchieved
Metric: agent-goal-accuracy
Mode: WITHOUT_REFERENCE
Models: [grok-4.1-fast, gemini-2.5-flash, deepseek-v3.2]

───────────────────────────────────────────────────────────────
GENERATED REPORTS
───────────────────────────────────────────────────────────────
HTML: abc123-attachment.html (18.5 KB)
MD: abc123-attachment.md (5.2 KB)

───────────────────────────────────────────────────────────────
STEP EXPLANATION (4 Questions)
───────────────────────────────────────────────────────────────

Step 1: InferGoal
├── Q1. What is checked? ✓ inputData shows conversation
├── Q2. Why? ✓ title="Inferring goal from conversation"
├── Q3. By which models? ✓ 3 models in modelResults
└── Q4. What did they answer? ✓ 3 items with source badges

Step 2: EvaluateOutcome
├── Q1. What is checked? ✓ inputData shows inferred goal
├── Q2. Why? ✓ title="Evaluating outcome"
├── Q3. By which models? ✓ 3 models
└── Q4. What did they answer? ✓ All show ACHIEVED

───────────────────────────────────────────────────────────────
CONVERSATION BLOCK
───────────────────────────────────────────────────────────────
✓ Multi-turn format used
✓ Human messages: 3
✓ AI messages: 3
✓ Tool calls displayed: 2
✓ Tool results: 2

───────────────────────────────────────────────────────────────
CSS VALIDATION
───────────────────────────────────────────────────────────────
✓ Score 1.0 → score-excellent (green)
✓ Items with passed=true → .passed styling

═══════════════════════════════════════════════════════════════
SUMMARY: 16/16 checks passed
STATUS: ✓ PASS
═══════════════════════════════════════════════════════════════
```

### Failure Format:

```
═══════════════════════════════════════════════════════════════
                    ALLURE REPORT VALIDATION
═══════════════════════════════════════════════════════════════

Test Executed: EnToolCallAccuracyIntegrationIT#testTypedMessages
Metric: tool-call-accuracy

───────────────────────────────────────────────────────────────
ISSUES FOUND
───────────────────────────────────────────────────────────────
✗ Q4 items missing source field
  → items[0].source = null (expected model ID)
  → items[1].source = null (expected model ID)

✗ Test returned null score
  → Check multiTurnScoreAsync() implementation

═══════════════════════════════════════════════════════════════
SUMMARY: 14/16 checks passed
STATUS: ✗ FAIL (2 issues)
═══════════════════════════════════════════════════════════════
```

---

## Common Issues

|        Issue        |          Symptom          |               Root Cause                |
|---------------------|---------------------------|-----------------------------------------|
| No report generated | 0 files in allure-results | Test returned null/threw exception      |
| Missing items       | items[] is empty          | Extractor not populating                |
| source=null         | "0" instead of model name | source field not set on ExplanationItem |
| Empty conversation  | No messages shown         | conversationMessages not set            |
| Test returns null   | No score calculated       | Validation failed early                 |

---

## Validation Checklist

```
[ ] Phase 1: Test executed successfully
[ ] Phase 2: Report files generated
[ ] Phase 3: JSON extracted from HTML
[ ] Phase 4: 4 Questions per step
    [ ] Q1: inputData shows actual content
    [ ] Q2: title + description present
    [ ] Q3: modelResults list all models
    [ ] Q4: items[] have source and content
[ ] Phase 5: Conversation block correct
[ ] Phase 6: CSS colors match scores
[ ] Phase 7: Metric-specific checks pass
```

