<#ftl output_format="HTML">
<#-- Score comparison chart -->
<#if chartData?? && chartData.hasScoreData()>
<div class="chart-container">
    <h3>${i18n["summary.modelScores"]}</h3>
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 600 ${(chartData.scoreEntries?size * 30) + 60}" class="bar-chart">
        <style>
            .row-label { font-size: 11px; fill: #333; text-anchor: end; font-family: sans-serif; }
            .scale-label { font-size: 10px; fill: #666; text-anchor: middle; font-family: sans-serif; }
            .score-label { font-size: 10px; fill: #333; font-family: sans-serif; }
            .grid-line { stroke: #e0e0e0; stroke-width: 1; }
            .bar-excellent { fill: #2e7d32; }
            .bar-good { fill: #4caf50; }
            .bar-moderate { fill: #ff9800; }
            .bar-poor { fill: #f44336; }
            .bar-excluded { fill: #9e9e9e; }
        </style>
        <#list 0..10 as i>
            <line x1="${150 + i * 40}" y1="20" x2="${150 + i * 40}" y2="${(chartData.scoreEntries?size * 30) + 30}" class="grid-line"/>
            <#if i % 2 == 0>
                <text x="${150 + i * 40}" y="${(chartData.scoreEntries?size * 30) + 50}" class="scale-label">${i / 10.0}</text>
            </#if>
        </#list>
        <#list chartData.scoreEntries as entry>
            <#assign y = 20 + entry?index * 30>
            <#assign barWidth = (entry.score * 400)?int>
            <#assign colorClass = entry.excluded?then('bar-excluded',
                data.invertedMetric?then(
                    (entry.score <= 0.1)?then('bar-excellent', (entry.score <= 0.3)?then('bar-good', (entry.score <= 0.5)?then('bar-moderate', 'bar-poor'))),
                    (entry.score >= 0.8)?then('bar-excellent', (entry.score >= 0.6)?then('bar-good', (entry.score >= 0.4)?then('bar-moderate', 'bar-poor')))
                ))>
            <text x="145" y="${y + 17}" class="row-label">${entry.modelId?truncate(20, '...')}</text>
            <rect x="150" y="${y}" width="${barWidth}" height="24" class="${colorClass}" rx="3"/>
            <text x="${155 + barWidth}" y="${y + 17}" class="score-label">${entry.score?string("0.0000")}</text>
        </#list>
    </svg>
</div>
</#if>

<#-- Timeline chart -->
<#if chartData?? && chartData.hasTimelineData()>
<div class="chart-container">
    <h3>${i18n["summary.executionTimeline"]}</h3>
    <#assign timelineHeight = (chartData.timelineEntries?size * 30) + 80>
    <#assign timeScale = (chartData.maxDurationMs > 0)?then(600.0 / chartData.maxDurationMs, 1.0)>
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 ${timelineHeight}" class="timeline-chart">
        <style>
            .row-label { font-size: 11px; fill: #333; text-anchor: end; font-family: sans-serif; }
            .time-label { font-size: 10px; fill: #666; text-anchor: middle; font-family: sans-serif; }
            .axis { stroke: #333; stroke-width: 1; }
            .grid-line { stroke: #e0e0e0; stroke-width: 1; stroke-dasharray: 2,2; }
            .bar-llm { fill: #2196f3; }
            .bar-embedding { fill: #9c27b0; }
            .bar-compute { fill: #ff9800; }
            .bar-error { fill: #f44336; }
            .legend-text { font-size: 10px; fill: #333; font-family: sans-serif; }
        </style>
        <line x1="150" y1="${timelineHeight - 30}" x2="770" y2="${timelineHeight - 30}" class="axis"/>
        <#list 0..5 as i>
            <#assign x = 150 + (i * 620 / 5)?int>
            <#assign time = (chartData.maxDurationMs * i / 5)?int>
            <text x="${x}" y="${timelineHeight - 10}" class="time-label">${time}ms</text>
            <line x1="${x}" y1="30" x2="${x}" y2="${timelineHeight - 35}" class="grid-line"/>
        </#list>
        <#list chartData.timelineEntries as entry>
            <#assign y = 30 + entry?index * 30>
            <#assign barX = 150 + (entry.startOffsetMs * timeScale)?int>
            <#assign barWidth = (entry.durationMs * timeScale)?int>
            <#if barWidth < 2><#assign barWidth = 2></#if>
            <#assign colorClass = (!entry.success)?then('bar-error', (entry.stepType == 'LLM')?then('bar-llm', (entry.stepType == 'EMBEDDING')?then('bar-embedding', 'bar-compute')))>
            <text x="145" y="${y + 17}" class="row-label">${entry.modelId?truncate(20, '...')}</text>
            <rect x="${barX}" y="${y}" width="${barWidth}" height="24" class="${colorClass}" rx="3">
                <title>${entry.stepName}: ${entry.durationMs?c}ms (${entry.stepType})</title>
            </rect>
        </#list>
        <g transform="translate(580, 5)">
            <rect x="0" y="0" width="12" height="12" class="bar-llm"/>
            <text x="16" y="10" class="legend-text">${i18n["chart.llm"]}</text>
            <rect x="60" y="0" width="12" height="12" class="bar-embedding"/>
            <text x="76" y="10" class="legend-text">${i18n["chart.embedding"]}</text>
            <rect x="150" y="0" width="12" height="12" class="bar-compute"/>
            <text x="166" y="10" class="legend-text">${i18n["chart.compute"]}</text>
        </g>
    </svg>
</div>
</#if>

<#-- Sample Overview -->
<div class="sample-section">
    <h3>${i18n["summary.inputSample"]}</h3>
    <dl>
        <#if userInput?has_content>
        <div class="sample-item">
            <dt>${i18n["summary.userInput"]}</dt>
            <dd><pre>${userInput}</pre></dd>
        </div>
        </#if>

        <#if response?has_content>
        <div class="sample-item">
            <dt>${i18n["summary.response"]}</dt>
            <dd><pre>${response}</pre></dd>
        </div>
        </#if>

        <#if reference?has_content>
        <div class="sample-item">
            <dt>${i18n["summary.reference"]}</dt>
            <dd><pre>${reference}</pre></dd>
        </div>
        </#if>

        <#if retrievedContexts?has_content>
        <div class="sample-item">
            <dt>${i18n["summary.retrievedContexts"]}</dt>
            <dd>
                <ol class="context-list">
                    <#list retrievedContexts as ctx>
                    <li><pre>${ctx}</pre></li>
                    </#list>
                </ol>
            </dd>
        </div>
        </#if>
    </dl>
</div>

<#-- Models Used -->
<div class="sample-section">
    <h3>${i18n["summary.models"]}</h3>
    <dl>
        <div class="sample-item">
            <dt>${i18n["summary.llmModels"]}</dt>
            <dd><code>${modelIds?join(", ")}</code></dd>
        </div>
        <#if embeddingModelIds?has_content>
        <div class="sample-item">
            <dt>${i18n["summary.embeddingModels"]}</dt>
            <dd><code>${embeddingModelIds?join(", ")}</code></dd>
        </div>
        </#if>
        <#if excludedModels?has_content>
        <div class="sample-item">
            <dt>${i18n["summary.excludedModels"]}</dt>
            <dd><code style="color: #f44336;">${excludedModels?join(", ")}</code></dd>
        </div>
        </#if>
    </dl>
</div>

<#-- Configuration -->
<#if configJson?has_content>
<div class="sample-section">
    <h3>${i18n["summary.configuration"]}</h3>
    <pre>${configJson}</pre>
</div>
</#if>
