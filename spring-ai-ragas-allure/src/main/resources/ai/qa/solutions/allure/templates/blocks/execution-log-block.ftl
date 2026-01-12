<#ftl output_format="HTML">
<#-- Langfuse-style trace viewer -->
<div class="trace-viewer">
    <#-- Left panel: Tree view -->
    <div class="trace-tree">
        <div class="tree-header">${i18n["trace.header"]}</div>
        <div class="tree-content">
            <#list steps as step>
            <div class="tree-node step-node" data-step="${step.stepIndex}">
                <div class="tree-item" onclick="selectStep(${step.stepIndex})">
                    <span class="tree-expand <#if step.modelResults?has_content || step.hasEmbeddingResults()>has-children</#if>"
                          onclick="event.stopPropagation(); toggleTreeNode(this)">
                        <#if step.modelResults?has_content || step.hasEmbeddingResults()>▶<#else>•</#if>
                    </span>
                    <span class="tree-type-badge ${step.stepType?lower_case}">${step.stepType}</span>
                    <span class="tree-name">${step.stepName}</span>
                    <span class="tree-duration">${step.durationMs?c}ms</span>
                </div>
                <#-- Children: Model results -->
                <div class="tree-children" style="display: none;">
                    <#list step.modelResults as modelResult>
                    <div class="tree-item model-item ${modelResult.success?then('', 'error')}"
                         onclick="selectModel(${step.stepIndex}, ${modelResult?index}, 'llm')">
                        <span class="tree-expand">•</span>
                        <span class="tree-type-badge model">LLM</span>
                        <span class="tree-name" title="${modelResult.modelId}">${modelResult.modelId}</span>
                        <span class="tree-duration">${modelResult.durationMs?c}ms</span>
                        <#if !modelResult.success><span class="tree-error-badge">!</span></#if>
                    </div>
                    </#list>
                    <#if step.hasEmbeddingResults()>
                    <#list step.embeddingResults as embResult>
                    <div class="tree-item model-item ${embResult.success?then('', 'error')}"
                         onclick="selectModel(${step.stepIndex}, ${embResult?index}, 'emb')">
                        <span class="tree-expand">•</span>
                        <span class="tree-type-badge embedding">EMB</span>
                        <span class="tree-name" title="${embResult.modelId}">${embResult.modelId}</span>
                        <span class="tree-duration">${embResult.durationMs?c}ms</span>
                        <#if !embResult.success><span class="tree-error-badge">!</span></#if>
                    </div>
                    </#list>
                    </#if>
                </div>
            </div>
            </#list>
        </div>
    </div>

    <#-- Right panel: Details view -->
    <div class="trace-details">
        <div class="details-header">
            <span class="details-title">${i18n["trace.selectItem"]}</span>
        </div>
        <div class="details-content" id="details-content">
            <div class="details-placeholder">
                ${i18n["trace.clickToView"]}
            </div>
        </div>
    </div>
</div>

<#-- Localized strings for JavaScript -->
<script id="trace-i18n" type="application/json">
<#noautoesc>
{
    "input": "${i18n["trace.input"]?json_string}",
    "output": "${i18n["trace.output"]?json_string}",
    "error": "${i18n["trace.error"]?json_string}",
    "stackTrace": "${i18n["trace.stackTrace"]?json_string}",
    "scoreCalculation": "${i18n["trace.scoreCalculation"]?json_string}",
    "computeInfo": "${i18n["trace.computeInfo"]?json_string}",
    "modelsEvaluated": "${i18n["trace.modelsEvaluated"]?json_string}",
    "successful": "${i18n["trace.successful"]?json_string}",
    "modelOutputs": "${i18n["trace.modelOutputs"]?json_string}",
    "duration": "${i18n["meta.duration"]?json_string}",
    "step": "${i18n["meta.step"]?json_string}",
    "success": "${i18n["status.success"]?json_string}",
    "errorStatus": "${i18n["status.error"]?json_string}",
    "ok": "${i18n["status.ok"]?json_string}",
    "failed": "${i18n["status.failed"]?json_string}"
}
</#noautoesc>
</script>

<#-- Hidden data for JavaScript -->
<script id="trace-data" type="application/json">
<#noautoesc>
{
    "steps": [
        <#list steps as step>
        {
            "stepIndex": ${step.stepIndex},
            "stepName": "${step.stepName?json_string}",
            "stepType": "${step.stepType}",
            "durationMs": ${step.durationMs?c},
            "request": <#if step.request?has_content>"${step.request?json_string}"<#else>null</#if>,
            "isLlmStep": ${step.llmStep?c},
            "successCount": ${step.successCount},
            "failureCount": ${step.failureCount},
            "modelResults": [
                <#list step.modelResults as mr>
                {
                    "modelId": "${mr.modelId?json_string}",
                    "success": ${mr.success?c},
                    "durationMs": ${mr.durationMs?c},
                    "resultJson": <#if mr.resultJson?has_content>"${mr.resultJson?json_string}"<#else>null</#if>,
                    "errorMessage": <#if mr.errorMessage?has_content>"${mr.errorMessage?json_string}"<#else>null</#if>,
                    "stackTrace": <#if mr.stackTrace?has_content>"${mr.stackTrace?json_string}"<#else>null</#if>
                }<#if mr?has_next>,</#if>
                </#list>
            ],
            "embeddingResults": [
                <#list step.embeddingResults as er>
                {
                    "modelId": "${er.modelId?json_string}",
                    "success": ${er.success?c},
                    "durationMs": ${er.durationMs?c}
                }<#if er?has_next>,</#if>
                </#list>
            ]
        }<#if step?has_next>,</#if>
        </#list>
    ]
}
</#noautoesc>
</script>

<script>
<#noparse>
(function() {
    const traceData = JSON.parse(document.getElementById('trace-data').textContent);
    const i18n = JSON.parse(document.getElementById('trace-i18n').textContent);
    let selectedElement = null;

    window.toggleTreeNode = function(el) {
        const node = el.closest('.step-node');
        const children = node.querySelector('.tree-children');
        if (children) {
            const isHidden = children.style.display === 'none';
            children.style.display = isHidden ? 'block' : 'none';
            el.textContent = isHidden ? '▼' : '▶';
        }
    };

    window.selectStep = function(stepIndex) {
        const step = traceData.steps[stepIndex];
        clearSelection();

        const stepNode = document.querySelector(`.step-node[data-step="${stepIndex}"] > .tree-item`);
        if (stepNode) {
            stepNode.classList.add('selected');
            selectedElement = stepNode;
        }

        let html = `
            <div class="detail-section">
                <div class="detail-header">
                    <span class="detail-type-badge ${step.stepType.toLowerCase()}">${step.stepType}</span>
                    <span class="detail-name">${escapeHtml(step.stepName)}</span>
                </div>
                <div class="detail-meta">
                    <div class="meta-item"><span class="meta-label">${i18n.duration}:</span> ${step.durationMs}ms</div>
                    <div class="meta-item"><span class="meta-label">${i18n.success}:</span> ${step.successCount}</div>
                    <div class="meta-item"><span class="meta-label">${i18n.failed}:</span> ${step.failureCount}</div>
                </div>
            </div>
        `;

        if (step.isLlmStep && step.request) {
            html += `
                <div class="detail-section">
                    <div class="detail-section-title">${i18n.input}</div>
                    <pre class="detail-code">${escapeHtml(step.request)}</pre>
                </div>
            `;
        }

        if (step.stepType === 'COMPUTE') {
            html += `
                <div class="detail-section">
                    <div class="detail-section-title">${i18n.scoreCalculation}</div>
                    <div class="compute-info">
                        <p>${i18n.computeInfo}</p>
                        <div class="meta-item"><span class="meta-label">${i18n.modelsEvaluated}:</span> ${step.successCount + step.failureCount}</div>
                        <div class="meta-item"><span class="meta-label">${i18n.successful}:</span> ${step.successCount}</div>
                    </div>
                </div>
            `;
        }

        if (step.modelResults.length > 0) {
            html += `
                <div class="detail-section">
                    <div class="detail-section-title">${i18n.modelOutputs} (${step.modelResults.length})</div>
                    <div class="outputs-summary">
            `;
            step.modelResults.forEach((mr, idx) => {
                const status = mr.success ? 'success' : 'error';
                html += `
                    <div class="output-item ${status}" onclick="selectModel(${stepIndex}, ${idx}, 'llm')">
                        <span class="output-model">${escapeHtml(mr.modelId)}</span>
                        <span class="output-duration">${mr.durationMs}ms</span>
                        <span class="output-status ${status}">${mr.success ? i18n.ok : i18n.failed}</span>
                    </div>
                `;
            });
            html += '</div></div>';
        }

        document.getElementById('details-content').innerHTML = html;
        document.querySelector('.details-title').textContent = step.stepName;
    };

    window.selectModel = function(stepIndex, modelIndex, type) {
        const step = traceData.steps[stepIndex];
        const model = type === 'llm' ? step.modelResults[modelIndex] : step.embeddingResults[modelIndex];
        clearSelection();

        const items = document.querySelectorAll(`.step-node[data-step="${stepIndex}"] .model-item`);
        const targetIndex = type === 'llm' ? modelIndex : step.modelResults.length + modelIndex;
        if (items[targetIndex]) {
            items[targetIndex].classList.add('selected');
            selectedElement = items[targetIndex];
        }

        // Expand parent if collapsed
        const stepNode = document.querySelector(`.step-node[data-step="${stepIndex}"]`);
        const children = stepNode.querySelector('.tree-children');
        const expand = stepNode.querySelector('.tree-expand');
        if (children && children.style.display === 'none') {
            children.style.display = 'block';
            expand.textContent = '▼';
        }

        let html = `
            <div class="detail-section">
                <div class="detail-header">
                    <span class="detail-type-badge model">${type === 'llm' ? 'LLM' : 'EMBEDDING'}</span>
                    <span class="detail-name">${escapeHtml(model.modelId)}</span>
                    <span class="detail-status ${model.success ? 'success' : 'error'}">${model.success ? i18n.success : i18n.errorStatus}</span>
                </div>
                <div class="detail-meta">
                    <div class="meta-item"><span class="meta-label">${i18n.duration}:</span> ${model.durationMs}ms</div>
                    <div class="meta-item"><span class="meta-label">${i18n.step}:</span> ${step.stepName}</div>
                </div>
            </div>
        `;

        if (type === 'llm') {
            if (step.request) {
                html += `
                    <div class="detail-section">
                        <div class="detail-section-title">${i18n.input}</div>
                        <pre class="detail-code">${escapeHtml(step.request)}</pre>
                    </div>
                `;
            }

            if (model.success && model.resultJson) {
                let formattedJson = model.resultJson;
                try {
                    formattedJson = JSON.stringify(JSON.parse(model.resultJson), null, 2);
                } catch(e) {}
                html += `
                    <div class="detail-section">
                        <div class="detail-section-title">${i18n.output}</div>
                        <pre class="detail-code">${escapeHtml(formattedJson)}</pre>
                    </div>
                `;
            }

            if (!model.success) {
                html += `
                    <div class="detail-section error-section">
                        <div class="detail-section-title">${i18n.error}</div>
                        <pre class="detail-code error">${escapeHtml(model.errorMessage || i18n.error)}</pre>
                    </div>
                `;
                if (model.stackTrace) {
                    html += `
                        <div class="detail-section">
                            <details>
                                <summary class="detail-section-title" style="cursor: pointer;">${i18n.stackTrace}</summary>
                                <pre class="detail-code stack-trace">${escapeHtml(model.stackTrace)}</pre>
                            </details>
                        </div>
                    `;
                }
            }
        }

        document.getElementById('details-content').innerHTML = html;
        document.querySelector('.details-title').textContent = model.modelId;
    };

    function clearSelection() {
        if (selectedElement) {
            selectedElement.classList.remove('selected');
        }
        document.querySelectorAll('.tree-item.selected').forEach(el => el.classList.remove('selected'));
    }

    function escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    // Auto-select first step on load
    if (traceData.steps.length > 0) {
        selectStep(0);
    }
})();
</#noparse>
</script>

<#-- Excluded Models Summary -->
<#if exclusions?has_content>
<div class="exclusions-section" style="margin-top: 24px;">
    <h3>${i18n["excluded.title"]}</h3>
    <table class="exclusions-table">
        <thead>
            <tr>
                <th>${i18n["table.model"]}</th>
                <th>${i18n["table.failedStep"]}</th>
                <th>${i18n["table.reason"]}</th>
            </tr>
        </thead>
        <tbody>
            <#list exclusions as excl>
            <tr>
                <td><code>${excl.modelId}</code></td>
                <td>${excl.failedStepName} (${i18n["meta.step"]} ${excl.failedStepIndex + 1})</td>
                <td>${excl.errorMessage}</td>
            </tr>
            </#list>
        </tbody>
    </table>
</div>
</#if>
