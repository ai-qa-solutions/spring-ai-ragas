<#ftl output_format="HTML">
<#-- Score Explanation Block - Explains why the metric got a specific score -->

<#if data.hasScoreExplanation()>
<#assign exp = data.scoreExplanation>

<div class="explanation-viewer">
    <#-- Header with simple description -->
    <div class="explanation-intro">
        <div class="explanation-icon">
            <#if exp.interpretation?? && exp.interpretation.isGood??>
                <#if exp.interpretation.isGood>
                    <span class="icon-good">&#10003;</span>
                <#else>
                    <span class="icon-bad">&#10007;</span>
                </#if>
            <#else>
                <span class="icon-info">i</span>
            </#if>
        </div>
        <div class="explanation-summary">
            <div class="explanation-title">${i18n["explanation.whyScore"]}</div>
            <div class="explanation-simple">${exp.simpleDescription}</div>
        </div>
    </div>

    <#-- Main content: Tree + Details panel -->
    <div class="explanation-main">
        <#-- Left panel: Steps tree -->
        <div class="explanation-tree">
            <div class="tree-header">${i18n["explanation.steps"]}</div>
            <div class="tree-content">
                <#list exp.steps as step>
                <div class="exp-tree-node" data-step="${step?index}">
                    <div class="exp-tree-item" onclick="selectExplanationStep(${step?index})">
                        <span class="exp-tree-number">${step.stepNumber}</span>
                        <span class="exp-tree-name">${step.title}</span>
                        <#if step.agreementPercent?? && step.agreementPercent < 100>
                        <span class="exp-tree-delta" title="${i18n["explanation.modelDisagreement"]}">
                            &#916; ${step.agreementPercent?string["0"]}%
                        </span>
                        </#if>
                    </div>
                </div>
                </#list>

                <#-- Final score interpretation -->
                <div class="exp-tree-node interpretation-node" data-step="interpretation">
                    <div class="exp-tree-item selected" onclick="selectExplanationStep('interpretation')">
                        <span class="exp-tree-number">&#10148;</span>
                        <span class="exp-tree-name">${i18n["explanation.interpretation"]}</span>
                    </div>
                </div>
            </div>
        </div>

        <#-- Right panel: Step details -->
        <div class="explanation-details">
            <div id="exp-details-content">
                <#-- Default: show interpretation -->
                <#include "score-interpretation.ftl">
            </div>
        </div>
    </div>
</div>

<#-- Hidden data for JavaScript -->
<script id="explanation-data" type="application/json">
<#noautoesc>
{
    "metricType": "${exp.metricType}",
    "hasFixedInterpretation": ${exp.hasFixedInterpretation()?c},
    "executionSteps": [
        <#list steps as step>
        {
            "stepName": "${step.stepName?json_string}",
            "stepType": "${step.stepType}",
            "modelResults": [
                <#list step.modelResults as mr>
                {
                    "modelId": "${mr.modelId?json_string}",
                    "success": ${mr.success?c},
                    "durationMs": ${mr.durationMs?c},
                    "resultJson": <#if mr.resultJson?has_content>"${mr.resultJson?json_string}"<#else>null</#if>
                }<#if mr?has_next>,</#if>
                </#list>
            ]
        }<#if step?has_next>,</#if>
        </#list>
    ],
    "modelScores": {
        <#list data.modelScores as modelId, score>
        "${modelId?json_string}": ${score}<#if modelId?has_next>,</#if>
        </#list>
    },
    "aggregatedScore": <#if data.aggregatedScore??>${data.aggregatedScore}<#else>null</#if>,
    "steps": [
        <#list exp.steps as step>
        {
            "stepNumber": ${step.stepNumber},
            "stepName": "${step.stepName?json_string}",
            "title": "${step.title?json_string}",
            "description": "${step.description?json_string}",
            "inputData": <#if step.inputData??>"${step.inputData?json_string}"<#else>null</#if>,
            "outputSummary": <#if step.outputSummary??>"${step.outputSummary?json_string}"<#else>null</#if>,
            "agreementPercent": ${step.agreementPercent!100},
            "hasModelDisagreement": ${(step.hasModelDisagreement!false)?c},
            "items": [
                <#list step.items![] as item>
                {
                    "index": ${item.index!item?index + 1},
                    "content": "${item.content?json_string}",
                    "passed": <#if item.passed??>${item.passed?c}<#else>null</#if>,
                    "verdict": <#if item.verdict??>"${item.verdict?json_string}"<#else>null</#if>,
                    "reason": <#if item.reason??>"${item.reason?json_string}"<#else>null</#if>,
                    "source": <#if item.source??>"${item.source?json_string}"<#else>null</#if>,
                    "numericValue": <#if item.numericValue??>${item.numericValue}<#else>null</#if>
                }<#if item?has_next>,</#if>
                </#list>
            ],
            "modelResults": [
                <#list step.modelResults![] as mr>
                {
                    "modelId": "${mr.modelId?json_string}",
                    "success": ${mr.success?c},
                    "verdict": <#if mr.verdict??>${mr.verdict?c}<#else>null</#if>,
                    "numericResult": <#if mr.numericResult??>${mr.numericResult}<#else>null</#if>,
                    "numerator": <#if mr.numerator??>${mr.numerator}<#else>null</#if>,
                    "denominator": <#if mr.denominator??>${mr.denominator}<#else>null</#if>,
                    "reasoning": <#if mr.reasoning??>"${mr.reasoning?json_string}"<#else>null</#if>,
                    "errorMessage": <#if mr.errorMessage??>"${mr.errorMessage?json_string}"<#else>null</#if>
                }<#if mr?has_next>,</#if>
                </#list>
            ],
            "metadata": {
                <#if step.metadata??>
                <#list step.metadata as key, value>
                "${key?json_string}": <#if value??>"${value?string?json_string}"<#else>null</#if><#if key?has_next>,</#if>
                </#list>
                </#if>
            }
        }<#if step?has_next>,</#if>
        </#list>
    ],
    "interpretation": {
        "formula": "${exp.interpretation.formula?json_string}",
        "calculation": "${exp.interpretation.calculation?json_string}",
        "score": <#if exp.interpretation.score??>${exp.interpretation.score}<#else>null</#if>,
        "scorePercent": "${exp.interpretation.scorePercent?json_string}",
        "level": "${exp.interpretation.level?json_string}",
        "isGood": <#if exp.interpretation.isGood??>${exp.interpretation.isGood?c}<#else>null</#if>,
        "meaning": "${exp.interpretation.meaning?json_string}",
        "numerator": <#if exp.interpretation.numerator??>${exp.interpretation.numerator}<#else>null</#if>,
        "denominator": <#if exp.interpretation.denominator??>${exp.interpretation.denominator}<#else>null</#if>,
        "currentLevelIndex": ${exp.interpretation.currentLevelIndex!0},
        "minLevel": <#if exp.interpretation.minLevel??>${exp.interpretation.minLevel}<#else>1</#if>,
        "maxLevel": <#if exp.interpretation.maxLevel??>${exp.interpretation.maxLevel}<#else>5</#if>,
        "scaleLevels": [
            <#list exp.interpretation.scaleLevels![] as level>
            {
                "name": "${level.name?json_string}",
                "range": "${level.range?json_string}",
                "description": "${level.description?json_string}",
                "current": ${level.current?c}
            }<#if level?has_next>,</#if>
            </#list>
        ]
    }
}
</#noautoesc>
</script>

<#-- i18n strings for JavaScript -->
<script id="explanation-i18n" type="application/json">
<#noautoesc>
{
    "step": "${i18n["explanation.step"]?json_string}",
    "formula": "${i18n["explanation.formula"]?json_string}",
    "calculation": "${i18n["explanation.calculation"]?json_string}",
    "result": "${i18n["explanation.result"]?json_string}",
    "interpretation": "${i18n["explanation.interpretation"]?json_string}",
    "scale": "${i18n["explanation.scale"]?json_string}",
    "currentLevel": "${i18n["explanation.currentLevel"]?json_string}",
    "items": "${i18n["explanation.items"]?json_string}",
    "verdict": "${i18n["explanation.verdict"]?json_string}",
    "reason": "${i18n["explanation.reason"]?json_string}",
    "source": "${i18n["explanation.source"]?json_string}",
    "modelDisagreement": "${i18n["explanation.modelDisagreement"]?json_string}",
    "modelsAgree": "${i18n["explanation.modelsAgree"]?json_string}",
    "modelsDisagree": "${i18n["explanation.modelsDisagree"]?json_string}",
    "passed": "${i18n["explanation.passed"]?json_string}",
    "failed": "${i18n["explanation.failed"]?json_string}",
    "yourResult": "${i18n["explanation.yourResult"]?json_string}",
    "modelResults": "${(i18n["summary.modelScores"]!'Model Scores')?json_string}",
    "model": "${(i18n["table.model"]!'Model')?json_string}",
    "score": "${(i18n["table.score"]!'Score')?json_string}",
    "level": "${(i18n["table.level"]!'Level')?json_string}",
    "averageModels": "${(i18n["aggregation.average"]!'Average (%d models):')?json_string}",
    "nItems": "${(i18n["common.nItems"]!'%d items')?json_string}",
    "inputData": "${(i18n["summary.response"]!'Response')?json_string}",
    "inputLabelReference": "${(i18n["summary.reference"]!'Reference')?json_string}",
    "inputLabelResponse": "${(i18n["summary.response"]!'Response')?json_string}",
    "inputLabelUserInput": "${(i18n["summary.userInput"]!'User Input')?json_string}",
    "inputLabelContexts": "${(i18n["summary.retrievedContexts"]!'Retrieved Contexts')?json_string}",
    "inputLabelQuestionAnswer": "${(i18n["input.questionAnswer"]!'Question + Reference')?json_string}"
}
</#noautoesc>
</script>

<script>
<#noparse>
(function() {
    const expData = JSON.parse(document.getElementById('explanation-data').textContent);
    const i18n = JSON.parse(document.getElementById('explanation-i18n').textContent);
    let selectedElement = null;

    window.selectExplanationStep = function(stepIndex) {
        // Clear previous selection
        document.querySelectorAll('.exp-tree-item.selected').forEach(el => el.classList.remove('selected'));

        // Select new item
        let targetNode;
        if (stepIndex === 'interpretation') {
            targetNode = document.querySelector('.interpretation-node .exp-tree-item');
        } else {
            targetNode = document.querySelector(`.exp-tree-node[data-step="${stepIndex}"] .exp-tree-item`);
        }
        if (targetNode) {
            targetNode.classList.add('selected');
        }

        // Render content
        const detailsContent = document.getElementById('exp-details-content');

        if (stepIndex === 'interpretation') {
            detailsContent.innerHTML = renderInterpretation();
        } else {
            const step = expData.steps[stepIndex];
            detailsContent.innerHTML = renderStep(step);
        }
    };

    function renderStep(step) {
        let html = `
            <div class="exp-detail-header">
                <span class="exp-step-badge">${i18n.step} ${step.stepNumber}</span>
                <span class="exp-step-title">${escapeHtml(step.title)}</span>
            </div>
            <div class="exp-detail-description">${escapeHtml(step.description)}</div>
        `;

        // Always show inputData if available - this is the source text being analyzed
        if (step.inputData) {
            const inputLabel = getInputLabel(step.stepName);
            html += `
                <div class="exp-input-section">
                    <div class="exp-input-label">${inputLabel}:</div>
                    <div class="exp-input-data">${escapeHtml(step.inputData)}</div>
                </div>
            `;
        }

        // Show contexts from metadata if available (for ContextRecall)
        if (step.metadata && step.metadata.contexts) {
            html += `
                <div class="exp-input-section">
                    <div class="exp-input-label">${i18n.inputLabelContexts}:</div>
                    <div class="exp-input-data exp-contexts-data">${escapeHtml(step.metadata.contexts)}</div>
                </div>
            `;
        }

        // Find matching execution step to get model results
        const execStep = findExecutionStep(step.stepName);

        // Always show outputSummary if available
        if (step.outputSummary) {
            html += `<div class="exp-output-summary">${escapeHtml(step.outputSummary)}</div>`;
        }

        // Always show static items if available (e.g., entity comparison results)
        if (step.items && step.items.length > 0) {
            html += renderStepItems(step);
        }

        // Special handling for ComputeScore step - show per-model score breakdown
        if (step.stepName === 'ComputeScore' && expData.modelScores && Object.keys(expData.modelScores).length > 0) {
            html += renderModelScoresBreakdown();
        }
        // If we have execution data with model results, show per-model breakdown
        else if (execStep && execStep.modelResults && execStep.modelResults.length > 0) {
            html += renderModelDetailsFromExecution(execStep, step);
        }

        // Model results - show numeric or agreement/disagreement
        if (step.modelResults && step.modelResults.length > 0) {
            const hasNumericResults = step.modelResults.some(m => m.numericResult !== null);
            if (hasNumericResults) {
                html += renderModelNumericResults(step);
            } else if (step.hasModelDisagreement) {
                html += renderModelDisagreement(step);
            }
        }

        return html;
    }

    function findExecutionStep(stepName) {
        if (!expData.executionSteps) return null;
        // Map explanation step names to execution step names
        const nameMap = {
            // Faithfulness
            'ExtractStatements': 'GenerateStatements',
            'VerifyStatements': 'EvaluateFaithfulness',
            // ContextRecall
            'ClassifyStatements': 'ClassifyStatements',
            // ContextEntityRecall - metric now reports 3 separate steps
            'ExtractReferenceEntities': 'ExtractReferenceEntities',
            'ExtractContextEntities': 'ExtractContextEntities',
            'CompareEntities': 'ComputeEntityRecall',
            // ContextPrecision
            'EvaluateContexts': 'EvaluateAllContexts',
            // ResponseRelevancy
            'GenerateQuestions': 'GenerateQuestions',
            'EmbedAndCompare': 'ComputeCosineSimilarity',
            // NoiseSensitivity
            'ExtractReferenceStatements': 'DecomposeReference',
            'ExtractResponseStatements': 'DecomposeResponse',
            'AnalyzeMatches': 'EvaluateGroundTruthToAnswer',
            // AspectCritic, SimpleCriteria, RubricsScore
            'EvaluateAspect': 'Evaluate',
            'EvaluateCriteria': 'Evaluate',
            'EvaluateRubrics': 'Evaluate',
            'SelectLevel': 'Evaluate'
        };
        const execName = nameMap[stepName] || stepName;
        return expData.executionSteps.find(s =>
            s.stepName === execName ||
            s.stepName === stepName ||
            s.stepName.includes(stepName) ||
            stepName.includes(s.stepName)
        );
    }

    function renderModelDetailsFromExecution(execStep, explStep) {
        if (!execStep.modelResults || execStep.modelResults.length === 0) {
            return renderStepItems(explStep);
        }

        const isRubrics = expData.metricType === 'rubrics-score';
        const minLevel = expData.interpretation.minLevel || 1;
        const maxLevel = expData.interpretation.maxLevel || 5;

        let html = `<div class="exp-models-breakdown">
            <div class="exp-section-title">${i18n.modelResults}</div>`;

        execStep.modelResults.forEach(mr => {
            if (!mr.success) {
                html += `
                    <div class="exp-model-card error">
                        <div class="exp-model-header">
                            <span class="exp-model-name">${escapeHtml(mr.modelId)}</span>
                            <span class="exp-model-status error">ERROR</span>
                        </div>
                    </div>
                `;
                return;
            }

            // Parse resultJson and extract items
            const items = parseModelResult(mr.resultJson, expData.metricType, explStep.stepName);

            // For rubrics: show level instead of pass/fail count
            if (isRubrics && items.length > 0 && items[0].isRubricLevel) {
                const level = items[0].rubricScore;
                const colorClass = getRubricScoreColorClass(level, minLevel, maxLevel);
                html += `
                    <div class="exp-model-card">
                        <div class="exp-model-header" onclick="toggleModelDetails(this)">
                            <span class="exp-model-expand">▶</span>
                            <span class="exp-model-name">${escapeHtml(mr.modelId)}</span>
                            <span class="exp-model-calc ${colorClass}">${level} / ${maxLevel}</span>
                        </div>
                        <div class="exp-model-details" style="display: none;">
                            ${renderModelItems(items)}
                        </div>
                    </div>
                `;
                return;
            }

            // Count only items with actual pass/fail (not neutral)
            const evaluatedItems = items.filter(i => i.passed !== null);
            const passedCount = evaluatedItems.filter(i => i.passed === true).length;
            const totalCount = evaluatedItems.length;
            const isNeutral = totalCount === 0; // All items are extraction (neutral)
            const score = totalCount > 0 ? (passedCount / totalCount * 100).toFixed(1) : '-';

            html += `
                <div class="exp-model-card">
                    <div class="exp-model-header" onclick="toggleModelDetails(this)">
                        <span class="exp-model-expand">▶</span>
                        <span class="exp-model-name">${escapeHtml(mr.modelId)}</span>
                        ${isNeutral
                            ? `<span class="exp-model-calc neutral">${i18n.nItems.replace('%d', items.length)}</span>`
                            : `<span class="exp-model-calc">${passedCount} / ${totalCount}</span>
                               <span class="exp-model-score">${score}%</span>`
                        }
                    </div>
                    <div class="exp-model-details" style="display: none;">
                        ${renderModelItems(items)}
                    </div>
                </div>
            `;
        });

        html += '</div>';
        return html;
    }

    function parseModelResult(resultJson, metricType, stepName) {
        if (!resultJson) return [];
        try {
            const data = JSON.parse(resultJson);

            // RubricsScore: check metricType FIRST before any other checks
            // Returns level as neutral item (not pass/fail)
            if (metricType === 'rubrics-score' && typeof data.score === 'number') {
                return [{
                    content: data.reasoning || data.reason || 'Rubric evaluation',
                    passed: null, // null = neutral, this is a level not pass/fail
                    verdict: 'SCORE: ' + data.score,
                    reason: data.reasoning || data.reason || '',
                    isRubricLevel: true,
                    rubricScore: data.score
                }];
            }

            // Faithfulness & NoiseSensitivity verdicts
            if (data.verdicts && Array.isArray(data.verdicts)) {
                const isNoise = metricType === 'noise-sensitivity';
                return data.verdicts.map(v => ({
                    content: v.statement || '',
                    passed: v.verdict === 1 || v.verdict === true,
                    verdict: (v.verdict === 1 || v.verdict === true)
                        ? (isNoise ? 'OK' : 'FAITHFUL')
                        : (isNoise ? 'ERROR' : 'UNFAITHFUL'),
                    reason: v.reason || ''
                }));
            }

            // Context recall classifications
            if (data.classifications && Array.isArray(data.classifications)) {
                return data.classifications.map(c => ({
                    content: c.statement || '',
                    passed: c.attributed === 1 || c.attributed === true,
                    verdict: (c.attributed === 1 || c.attributed === true) ? 'FOUND' : 'MISSING',
                    reason: c.reason || ''
                }));
            }

            // Statement extraction (Faithfulness, NoiseSensitivity) - neutral, no pass/fail
            if (data.statements && Array.isArray(data.statements)) {
                return data.statements.map(s => ({
                    content: typeof s === 'string' ? s : (s.statement || JSON.stringify(s)),
                    passed: null, // null means neutral - no pass/fail for extraction
                    verdict: '',
                    reason: ''
                }));
            }

            // Generated questions (ResponseRelevancy)
            if (data.questions && Array.isArray(data.questions)) {
                return data.questions.map(q => {
                    const text = typeof q === 'string' ? q : (q.question || '');
                    const isNoncommittal = q.noncommittal === 1 || q.noncommittal === true;
                    // Only mark as failed if noncommittal flag is explicitly set
                    const hasNoncommittalFlag = typeof q === 'object' && typeof q.noncommittal !== 'undefined';
                    return {
                        content: text,
                        passed: hasNoncommittalFlag ? !isNoncommittal : null, // null if no flag
                        verdict: isNoncommittal ? 'NONCOMMITTAL' : '',
                        reason: ''
                    };
                });
            }

            // Entities (ContextEntityRecall) - neutral, no pass/fail for extraction
            if (data.entities && Array.isArray(data.entities)) {
                return data.entities.map(e => ({
                    content: typeof e === 'string' ? e : JSON.stringify(e),
                    passed: null, // null means neutral
                    verdict: '',
                    reason: ''
                }));
            }

            // ContextPrecision: { relevant: Boolean, reasoning }
            if (typeof data.relevant !== 'undefined') {
                return [{
                    content: data.reasoning || 'Context relevance',
                    passed: data.relevant === true,
                    verdict: data.relevant ? 'RELEVANT' : 'NOT RELEVANT',
                    reason: data.reasoning || ''
                }];
            }

            // AspectCritic: { verdict: Boolean, reasoning }
            if (typeof data.verdict !== 'undefined' && typeof data.reasoning !== 'undefined') {
                return [{
                    content: data.reasoning || 'Evaluation result',
                    passed: data.verdict === 1 || data.verdict === true,
                    verdict: (data.verdict === 1 || data.verdict === true) ? 'PASS' : 'FAIL',
                    reason: data.reasoning || ''
                }];
            }

            // SimpleCriteria: { score: Double, reasoning }
            if (typeof data.score === 'number' && typeof data.reasoning !== 'undefined') {
                return [{
                    content: data.reasoning || 'Score evaluation',
                    passed: true,
                    verdict: 'Score: ' + data.score,
                    reason: data.reasoning || ''
                }];
            }

            // Single verdict fallback
            if (typeof data.verdict !== 'undefined') {
                return [{
                    content: data.reason || data.reasoning || 'Evaluation result',
                    passed: data.verdict === 1 || data.verdict === true,
                    verdict: (data.verdict === 1 || data.verdict === true) ? 'PASS' : 'FAIL',
                    reason: data.reason || data.reasoning || ''
                }];
            }

            // Numeric array (cosine similarity scores)
            if (Array.isArray(data)) {
                return data.slice(0, 5).map((score, idx) => ({
                    content: `Similarity ${idx + 1}: ${(score * 100).toFixed(2)}%`,
                    passed: score >= 0.5,
                    verdict: (score * 100).toFixed(2) + '%',
                    reason: ''
                }));
            }

            // Single numeric result
            if (typeof data === 'number') {
                return [{
                    content: `Score: ${(data * 100).toFixed(2)}%`,
                    passed: data >= 0.5,
                    verdict: (data * 100).toFixed(2) + '%',
                    reason: ''
                }];
            }

        } catch (e) {
            console.debug('Failed to parse model result:', e);
        }
        return [];
    }

    function renderModelItems(items) {
        if (items.length === 0) return '<div class="exp-no-items">No items</div>';

        // For embedding/numeric results, show abbreviated view
        if (items.length === 1 && items[0].content.startsWith('Score:')) {
            return `<div class="exp-model-item">${escapeHtml(items[0].content)}</div>`;
        }

        let html = '<div class="exp-model-items-list">';
        items.forEach((item, idx) => {
            // Handle neutral items (passed === null) - no coloring
            const isNeutral = item.passed === null;
            const statusClass = isNeutral ? 'neutral' : (item.passed ? 'passed' : 'failed');
            const marker = isNeutral ? '•' : (item.passed ? '✓' : '✗');
            // For rubrics, show full reasoning text (no truncation)
            const displayText = item.isRubricLevel
                ? escapeHtml(item.content)
                : escapeHtml(truncate(item.content, 150));
            html += `
                <div class="exp-model-item ${statusClass}">
                    <span class="exp-item-marker ${statusClass}">${marker}</span>
                    <span class="exp-item-text">${displayText}</span>
                    ${item.verdict ? `<span class="exp-item-badge ${statusClass}">${escapeHtml(item.verdict)}</span>` : ''}
                </div>
            `;
        });
        html += '</div>';
        return html;
    }

    function truncate(str, maxLen) {
        if (!str || str.length <= maxLen) return str;
        return str.substring(0, maxLen) + '...';
    }

    function renderStepItems(step) {
        if (!step.items || step.items.length === 0) return '';

        const isRubrics = expData.metricType === 'rubrics-score';
        const minLevel = expData.interpretation.minLevel || 1;
        const maxLevel = expData.interpretation.maxLevel || 5;

        let html = `<div class="exp-items-section">
            <div class="exp-section-title">${i18n.items} (${step.items.length})</div>
            <div class="exp-items-list">`;

        step.items.forEach(item => {
            let statusClass = '';
            if (isRubrics && step.stepName === 'DefineRubric') {
                // For rubrics: unselected = neutral, selected = color by position
                if (item.passed === true) {
                    // Selected level - use position-based color
                    statusClass = getRubricScoreColorClass(item.index, minLevel, maxLevel);
                } else {
                    // Unselected levels - neutral gray
                    statusClass = 'neutral';
                }
            } else {
                // Standard logic for other metrics
                statusClass = item.passed === true ? 'passed' : (item.passed === false ? 'failed' : '');
            }

            html += `
                <div class="exp-item ${statusClass}">
                    <div class="exp-item-main">
                        <span class="exp-item-index">${item.index}.</span>
                        <span class="exp-item-content">${escapeHtml(item.content)}</span>
                        ${item.verdict ? `<span class="exp-item-verdict ${statusClass}">${escapeHtml(item.verdict)}</span>` : ''}
                    </div>
                    ${item.reason ? `<div class="exp-item-reason">${escapeHtml(item.reason)}</div>` : ''}
                    ${item.source ? `<div class="exp-item-source">${i18n.source}: ${escapeHtml(item.source)}</div>` : ''}
                </div>
            `;
        });

        html += '</div></div>';
        return html;
    }

    window.toggleModelDetails = function(header) {
        const details = header.nextElementSibling;
        const expand = header.querySelector('.exp-model-expand');
        if (details) {
            const isHidden = details.style.display === 'none';
            details.style.display = isHidden ? 'block' : 'none';
            expand.textContent = isHidden ? '▼' : '▶';
        }
    };

    function renderModelScoresBreakdown() {
        const scores = expData.modelScores;
        const modelIds = Object.keys(scores);
        if (modelIds.length === 0) return '';

        const isRubrics = expData.metricType === 'rubrics-score';
        const isInverted = expData.metricType === 'noise-sensitivity';
        const minLevel = expData.interpretation.minLevel || 1;
        const maxLevel = expData.interpretation.maxLevel || 5;
        const aggregated = expData.aggregatedScore;

        let html = `
            <div class="exp-scores-breakdown">
                <div class="exp-section-title">${i18n.modelResults}</div>
                <div class="exp-scores-table">
                    <table class="exp-table">
                        <thead>
                            <tr>
                                <th>${i18n.model}</th>
                                <th>${isRubrics ? i18n.level : i18n.score}</th>
                                ${isRubrics ? '<th>' + i18n.score + '</th>' : ''}
                            </tr>
                        </thead>
                        <tbody>
        `;

        modelIds.forEach(modelId => {
            const rawScore = scores[modelId];
            if (isRubrics) {
                // For rubrics: display level "2 / 5" and normalized percent
                const level = Math.round(rawScore);
                const displayLevel = level + ' / ' + maxLevel;
                const normalizedScore = (maxLevel > minLevel)
                    ? (level - minLevel) / (maxLevel - minLevel)
                    : 0;
                const normalizedPercent = (normalizedScore * 100).toFixed(1) + '%';
                const colorClass = getRubricScoreColorClass(level, minLevel, maxLevel);
                html += `
                    <tr>
                        <td class="model-name-cell">${escapeHtml(modelId)}</td>
                        <td class="score-cell ${colorClass}">${displayLevel}</td>
                        <td class="score-cell">${normalizedPercent}</td>
                    </tr>
                `;
            } else {
                // Standard percentage display
                const percent = (rawScore * 100).toFixed(2) + '%';
                const colorClass = getScoreColorClass(rawScore, isInverted);
                html += `
                    <tr>
                        <td class="model-name-cell">${escapeHtml(modelId)}</td>
                        <td class="score-cell ${colorClass}">${percent}</td>
                    </tr>
                `;
            }
        });

        html += `
                        </tbody>
                    </table>
                </div>
        `;

        // Show aggregation
        if (aggregated !== null && modelIds.length > 1) {
            if (isRubrics) {
                const avgLevel = aggregated;
                const normalizedScore = (maxLevel > minLevel)
                    ? (avgLevel - minLevel) / (maxLevel - minLevel)
                    : 0;
                const normalizedPercent = (normalizedScore * 100).toFixed(1) + '%';
                const colorClass = getRubricScoreColorClass(avgLevel, minLevel, maxLevel);
                html += `
                    <div class="exp-aggregation">
                        <div class="exp-aggregation-label">${i18n.averageModels.replace('%d', modelIds.length)}</div>
                        <div class="exp-aggregation-value ${colorClass}">
                            ${avgLevel.toFixed(1)} → ${normalizedPercent}
                        </div>
                    </div>
                `;
            } else {
                const avgPercent = (aggregated * 100).toFixed(2) + '%';
                const avgColorClass = getScoreColorClass(aggregated, isInverted);
                html += `
                    <div class="exp-aggregation">
                        <div class="exp-aggregation-label">${i18n.averageModels.replace('%d', modelIds.length)}</div>
                        <div class="exp-aggregation-value ${avgColorClass}">${avgPercent}</div>
                    </div>
                `;
            }
        }

        html += '</div>';
        return html;
    }

    /**
     * Determines color class for rubric level based on position in scale.
     * @param level - current level (e.g., 2)
     * @param minLevel - minimum level in scale (e.g., 1)
     * @param maxLevel - maximum level in scale (e.g., 5 or 9)
     * @returns CSS class: 'score-poor', 'score-moderate', or 'score-good'
     */
    function getRubricScoreColorClass(level, minLevel, maxLevel) {
        if (maxLevel === minLevel) return 'score-moderate';

        // Position of level in scale (0.0 = minimum, 1.0 = maximum)
        const position = (level - minLevel) / (maxLevel - minLevel);

        // First 33% - red (poor), middle 33% - yellow (moderate), top 33% - green (good)
        if (position <= 0.33) return 'score-poor';
        if (position <= 0.66) return 'score-moderate';
        return 'score-good';
    }

    function getScoreColorClass(score, isInverted) {
        if (isInverted) {
            // Lower is better (NoiseSensitivity)
            if (score <= 0.1) return 'score-excellent';
            if (score <= 0.3) return 'score-good';
            if (score <= 0.5) return 'score-moderate';
            return 'score-poor';
        } else {
            // Higher is better
            if (score >= 0.8) return 'score-excellent';
            if (score >= 0.6) return 'score-good';
            if (score >= 0.4) return 'score-moderate';
            return 'score-poor';
        }
    }

    function renderModelNumericResults(step) {
        const hasRatios = step.modelResults.some(m => m.numerator !== null && m.denominator !== null);

        let html = `
            <div class="exp-model-results-section">
                <div class="exp-section-title">${i18n.modelResults || 'Model Results'}</div>
                <div class="exp-model-results-table">
                    <table class="exp-table">
                        <thead>
                            <tr>
                                <th>${i18n.model || 'Model'}</th>
                                ${hasRatios ? '<th>Calculation</th>' : ''}
                                <th>${i18n.score || 'Score'}</th>
                            </tr>
                        </thead>
                        <tbody>
        `;

        step.modelResults.forEach(m => {
            const score = m.numericResult !== null ? (m.numericResult * 100).toFixed(2) + '%' : '-';
            const calculation = (m.numerator !== null && m.denominator !== null)
                ? `${m.numerator} / ${m.denominator}`
                : '';
            const rowClass = m.success ? '' : 'error-row';
            html += `
                <tr class="${rowClass}">
                    <td>${escapeHtml(m.modelId)}</td>
                    ${hasRatios ? `<td class="calculation-cell">${calculation}</td>` : ''}
                    <td class="score-cell">${score}</td>
                </tr>
            `;
        });

        html += `
                        </tbody>
                    </table>
                </div>
            </div>
        `;
        return html;
    }

    function renderModelDisagreement(step) {
        const agreed = step.modelResults.filter(m => m.verdict === true);
        const disagreed = step.modelResults.filter(m => m.verdict === false);

        let html = `
            <div class="exp-disagreement-section">
                <div class="exp-section-title">${i18n.modelDisagreement}</div>
                <div class="exp-disagreement-grid">
        `;

        if (agreed.length > 0) {
            html += `
                <div class="exp-disagreement-col agree">
                    <div class="exp-col-header">${i18n.modelsAgree} (${agreed.length})</div>
                    ${agreed.map(m => `<div class="exp-model-item">${escapeHtml(m.modelId)}</div>`).join('')}
                </div>
            `;
        }

        if (disagreed.length > 0) {
            html += `
                <div class="exp-disagreement-col disagree">
                    <div class="exp-col-header">${i18n.modelsDisagree} (${disagreed.length})</div>
                    ${disagreed.map(m => `<div class="exp-model-item">${escapeHtml(m.modelId)}</div>`).join('')}
                </div>
            `;
        }

        html += '</div></div>';
        return html;
    }

    function renderInterpretation() {
        const interp = expData.interpretation;
        const isRubrics = expData.metricType === 'rubrics-score';
        const minLevel = interp.minLevel || 1;
        const maxLevel = interp.maxLevel || 5;

        let html = `
            <div class="exp-interpretation">
                <div class="exp-detail-header">
                    <span class="exp-step-title">${i18n.interpretation}</span>
                </div>

                <div class="exp-formula-section">
                    <div class="exp-formula-label">${i18n.formula}</div>
                    <div class="exp-formula">${escapeHtml(interp.formula)}</div>
                </div>

                <div class="exp-calculation-section">
                    <div class="exp-calculation-label">${i18n.calculation}</div>
                    <div class="exp-calculation">${escapeHtml(interp.calculation)}</div>
                </div>

                <div class="exp-result-section">
                    <div class="exp-result-score ${interp.isGood === true ? 'good' : (interp.isGood === false ? 'bad' : '')}">
                        ${interp.scorePercent}
                    </div>
                    <div class="exp-result-meaning">${escapeHtml(interp.meaning)}</div>
                </div>
        `;

        // Scale visualization
        if (interp.scaleLevels && interp.scaleLevels.length > 0) {
            html += `
                <div class="exp-scale-section">
                    <div class="exp-section-title">${i18n.scale}</div>
                    <div class="exp-scale-list">
            `;

            interp.scaleLevels.forEach((level, idx) => {
                const isCurrent = level.current || idx === interp.currentLevelIndex;
                const levelNum = parseInt(level.range) || (interp.scaleLevels.length - idx);

                // For rubrics: inactive levels are neutral (gray),
                // current level gets dynamic color based on position
                let colorClass = 'neutral';
                if (isCurrent && isRubrics) {
                    colorClass = getRubricScoreColorClass(levelNum, minLevel, maxLevel);
                }

                html += `
                    <div class="exp-scale-item ${isCurrent ? 'current' : ''} ${colorClass}">
                        <span class="exp-scale-marker">${isCurrent ? '&#9658;' : ''}</span>
                        <span class="exp-scale-name">${escapeHtml(level.name)}</span>
                        <span class="exp-scale-range">${escapeHtml(level.range)}</span>
                        <span class="exp-scale-desc" title="${escapeHtml(level.description)}">${escapeHtml(level.description)}</span>
                        ${isCurrent ? `<span class="exp-scale-current">${i18n.yourResult}</span>` : ''}
                    </div>
                `;
            });

            html += '</div></div>';
        }

        html += '</div>';
        return html;
    }

    function getInputLabel(stepName) {
        // Map step names to appropriate input labels
        const labelMap = {
            // NoiseSensitivity
            'ExtractReferenceStatements': i18n.inputLabelReference,
            'ExtractResponseStatements': i18n.inputLabelResponse,
            // Faithfulness
            'ExtractStatements': i18n.inputLabelResponse,
            'VerifyStatements': i18n.inputLabelContexts,
            // ContextRecall
            'ClassifyStatements': i18n.inputLabelReference,
            // ContextEntityRecall
            'ExtractReferenceEntities': i18n.inputLabelReference,
            'ExtractContextEntities': i18n.inputLabelContexts,
            // ResponseRelevancy
            'GenerateQuestions': i18n.inputLabelResponse,
            'EmbedAndCompare': i18n.inputLabelUserInput,
            // ContextPrecision
            'EvaluateContexts': i18n.inputLabelQuestionAnswer,
            // General metrics
            'EvaluateAspect': i18n.inputLabelResponse,
            'EvaluateCriteria': i18n.inputLabelResponse,
            'EvaluateRubrics': i18n.inputLabelResponse,
            'SelectLevel': i18n.inputLabelResponse
        };
        return labelMap[stepName] || i18n.inputData || 'Input Data';
    }

    function escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
})();
</script>
</#noparse>

<#else>
<#-- No score explanation available -->
<div class="no-explanation">
    ${i18n["explanation.notAvailable"]}
</div>
</#if>
