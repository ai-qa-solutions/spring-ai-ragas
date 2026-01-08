<#ftl output_format="plainText">
# ${metricName} - ${i18n["report.title"]}

**${i18n["table.score"]}:** ${data.formattedScore}
**${i18n["report.duration"]}:** ${totalDurationMs?c}ms
<#if startTime??>**${i18n["md.timestamp"]}:** ${startTime?string}</#if>

---

## ${i18n["block.summary"]}

### ${i18n["summary.modelScores"]}

| ${i18n["table.model"]} | ${i18n["table.score"]} | ${i18n["table.status"]} |
|-------|-------|--------|
<#if chartData?? && chartData.scoreEntries?has_content>
<#list chartData.scoreEntries as entry>
| ${entry.modelId} | ${entry.score?string("0.0000")} | ${entry.excluded?then(i18n["status.excluded"], i18n["status.ok"])} |
</#list>
<#else>
| ${i18n["md.noScores"]} | - | - |
</#if>

### ${i18n["summary.inputSample"]}

**${i18n["summary.userInput"]}:**
```
${userInput!'N/A'}
```

<#if response?has_content>
**${i18n["summary.response"]}:**
```
${response}
```
</#if>

<#if reference?has_content>
**${i18n["summary.reference"]}:**
```
${reference}
```
</#if>

<#if retrievedContexts?has_content>
**${i18n["summary.retrievedContexts"]}:**
<#list retrievedContexts as ctx>
${ctx?index + 1}. ```
${ctx}
```
</#list>
</#if>

### ${i18n["summary.models"]}

**${i18n["summary.llmModels"]}:** ${modelIds?join(", ")}
<#if embeddingModelIds?has_content>
**${i18n["summary.embeddingModels"]}:** ${embeddingModelIds?join(", ")}
</#if>
<#if excludedModels?has_content>
**${i18n["summary.excludedModels"]}:** ${excludedModels?join(", ")}
</#if>

<#if configJson?has_content>
### ${i18n["summary.configuration"]}

```json
${configJson}
```
</#if>

---

<#if data.hasScoreExplanation()>
## ${i18n["block.explanation"]}

<#assign exp = data.scoreExplanation>
### ${i18n["explanation.whyScore"]}

${exp.simpleDescription}

<#list exp.steps as step>
#### ${i18n["explanation.step"]} ${step.stepNumber}: ${step.title}

${step.description}

<#if step.inputData??>
**${i18n["summary.response"]}:**
```
${step.inputData}
```
</#if>

<#if step.outputSummary??>
**${i18n["explanation.result"]}:** ${step.outputSummary}
</#if>

<#if step.items?has_content>
| # | Content | ${i18n["explanation.verdict"]} |
|---|---------|--------|
<#list step.items as item>
| ${item.index!item?index + 1} | ${item.content?replace("\n", " ")?truncate(80, "...")} | ${item.verdict!"-"} |
</#list>
</#if>

<#if step.modelResults?has_content>
**${i18n["summary.modelScores"]}:**
| ${i18n["table.model"]} | ${i18n["table.score"]} |
|-------|-------|
<#list step.modelResults as mr>
| ${mr.modelId} | ${mr.numericResult?string("0.00%")} |
</#list>
</#if>

<#if step.hasModelDisagreement!false>
> **${i18n["explanation.modelDisagreement"]}:** ${step.agreementPercent?string["0"]}% agreement
</#if>

</#list>

### ${i18n["explanation.interpretation"]}

**${i18n["explanation.formula"]}:** `${exp.interpretation.formula}`

**${i18n["explanation.calculation"]}:** `${exp.interpretation.calculation}`

**${i18n["explanation.result"]}:** ${exp.interpretation.scorePercent} - ${exp.interpretation.meaning}

<#if exp.interpretation.scaleLevels?has_content>
**${i18n["explanation.scale"]}:**
| Level | Range | Description |
|-------|-------|-------------|
<#list exp.interpretation.scaleLevels as level>
| ${level.current?then("**" + level.name + "**", level.name)} | ${level.range} | ${level.description} |
</#list>
</#if>

---

</#if>
## ${i18n["block.methodology"]}

${methodologyMarkdown!'Methodology documentation not available.'}

---

## ${i18n["block.execution"]}

<#list steps as step>
### ${i18n["meta.step"]} ${step.stepNumber}/${step.totalSteps}: ${step.stepName} [${step.stepType}]

**${i18n["meta.duration"]}:** ${step.durationMs?c}ms
**${i18n["trace.successful"]}:** ${step.successCount}
**${i18n["status.failed"]}:** ${step.failureCount}

<#if step.request?has_content && step.llmStep>
**${i18n["md.prompt"]}:**
```
${step.request}
```
</#if>

<#list step.modelResults as modelResult>
#### ${modelResult.modelId} - ${modelResult.success?then(i18n["status.ok"], i18n["status.failed"])} (${modelResult.durationMs?c}ms)

<#if modelResult.success>
**${i18n["trace.output"]}:**
```json
${modelResult.resultJson!'null'}
```
<#else>
**${i18n["trace.error"]}:** ${modelResult.errorMessage!'Unknown error'}

<#if modelResult.stackTrace?has_content>
<details>
<summary>${i18n["trace.stackTrace"]}</summary>

```
${modelResult.stackTrace}
```
</details>
</#if>
</#if>
</#list>

<#if step.hasEmbeddingResults()>
#### ${i18n["chart.embedding"]}

<#list step.embeddingResults as embResult>
- **${embResult.modelId}**: ${embResult.success?then(i18n["status.ok"], i18n["status.failed"])} (${embResult.durationMs?c}ms)
</#list>
</#if>

</#list>

<#if exclusions?has_content>
---

## ${i18n["excluded.title"]}

| ${i18n["table.model"]} | ${i18n["table.failedStep"]} | ${i18n["table.reason"]} |
|-------|-------------|--------|
<#list exclusions as excl>
| ${excl.modelId} | ${excl.failedStepName} | ${excl.errorMessage} |
</#list>

<#list exclusions as excl>
<#if excl.stackTrace?has_content>
<details>
<summary>${i18n["trace.stackTrace"]} - ${excl.modelId}</summary>

```
${excl.stackTrace}
```
</details>
</#if>
</#list>
</#if>
