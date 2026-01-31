<#ftl output_format="HTML">
<#-- Score comparison chart -->
<#if chartData?? && chartData.hasScoreData()>
<#-- Determine if this is a rubrics metric and get scale bounds -->
<#assign isRubrics = data.rubricsMetric!false>
<#assign minLevel = data.rubricsMinLevel!1>
<#assign maxLevel = data.rubricsMaxLevel!5>
<#assign levelRange = maxLevel - minLevel>
<div class="chart-container">
    <h3>${i18n["summary.modelScores"]}</h3>
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 600 ${(chartData.scoreEntries?size * 30) + 60}" class="bar-chart">
        <style>
            .row-label { font-size: 11px; fill: #1b2129; text-anchor: end; font-family: sans-serif; }
            .scale-label { font-size: 10px; fill: #677281; text-anchor: middle; font-family: sans-serif; }
            .score-label { font-size: 10px; fill: #1b2129; font-family: sans-serif; }
            .grid-line { stroke: #e8edf1; stroke-width: 1; }
            .bar-excellent { fill: #09a232; }
            .bar-good { fill: #09a232; }
            .bar-moderate { fill: #eb9b46; }
            .bar-poor { fill: #eb5146; }
            .bar-excluded { fill: #7281a1; }
        </style>
        <#if isRubrics>
            <#-- Rubrics scale: show level numbers (e.g., 1, 2, 3, 4, 5) -->
            <#assign scaleSteps = maxLevel - minLevel>
            <#assign stepWidth = (scaleSteps > 0)?then(400 / scaleSteps, 400)>
            <#list minLevel..maxLevel as lvl>
                <#assign xPos = 150 + (lvl - minLevel) * stepWidth>
                <line x1="${xPos?int}" y1="20" x2="${xPos?int}" y2="${(chartData.scoreEntries?size * 30) + 30}" class="grid-line"/>
                <text x="${xPos?int}" y="${(chartData.scoreEntries?size * 30) + 50}" class="scale-label">${lvl}</text>
            </#list>
        <#else>
            <#-- Standard scale: 0.0 to 1.0 -->
            <#list 0..10 as i>
                <line x1="${150 + i * 40}" y1="20" x2="${150 + i * 40}" y2="${(chartData.scoreEntries?size * 30) + 30}" class="grid-line"/>
                <#if i % 2 == 0>
                    <text x="${150 + i * 40}" y="${(chartData.scoreEntries?size * 30) + 50}" class="scale-label">${i / 10.0}</text>
                </#if>
            </#list>
        </#if>
        <#list chartData.scoreEntries as entry>
            <#assign y = 20 + entry?index * 30>
            <#if isRubrics>
                <#-- Rubrics: normalize score to 0-1 for bar width, then scale to 400px -->
                <#assign normalizedScore = (levelRange > 0)?then((entry.score - minLevel) / levelRange, 0)>
                <#assign barWidth = (normalizedScore * 400)?int>
                <#-- Position-based color: first 33% = poor, middle 33% = moderate, top 33% = good -->
                <#assign colorClass = entry.excluded?then('bar-excluded',
                    (normalizedScore <= 0.33)?then('bar-poor', (normalizedScore <= 0.66)?then('bar-moderate', 'bar-good')))>
                <#assign scoreDisplay = entry.score?string("0.0") + " / " + maxLevel>
            <#else>
                <#-- Standard metrics -->
                <#assign barWidth = (entry.score * 400)?int>
                <#assign colorClass = entry.excluded?then('bar-excluded',
                    data.invertedMetric?then(
                        (entry.score <= 0.1)?then('bar-excellent', (entry.score <= 0.3)?then('bar-good', (entry.score <= 0.5)?then('bar-moderate', 'bar-poor'))),
                        (entry.score >= 0.8)?then('bar-excellent', (entry.score >= 0.6)?then('bar-good', (entry.score >= 0.4)?then('bar-moderate', 'bar-poor')))
                    ))>
                <#assign scoreDisplay = entry.score?string("0.0000")>
            </#if>
            <text x="145" y="${y + 17}" class="row-label">${entry.modelId}</text>
            <rect x="150" y="${y}" width="${barWidth}" height="24" class="${colorClass}" rx="3"/>
            <text x="${155 + barWidth}" y="${y + 17}" class="score-label">${scoreDisplay}</text>
        </#list>
    </svg>
</div>
</#if>

<#-- Timeline chart -->
<#if chartData?? && chartData.hasTimelineData()>
<div class="chart-container">
    <h3>${i18n["summary.executionTimeline"]}</h3>
    <#assign timelineHeight = (chartData.timelineEntries?size * 30) + 80>
    <#assign labelWidth = 280>
    <#assign chartWidth = 600>
    <#assign totalWidth = labelWidth + chartWidth + 20>
    <#assign timeScale = (chartData.maxDurationMs > 0)?then(chartWidth / chartData.maxDurationMs, 1.0)>
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${totalWidth} ${timelineHeight}" class="timeline-chart">
        <style>
            .row-label { font-size: 11px; fill: #1b2129; text-anchor: end; font-family: sans-serif; }
            .time-label { font-size: 10px; fill: #677281; text-anchor: middle; font-family: sans-serif; }
            .axis { stroke: #1b2129; stroke-width: 1; }
            .grid-line { stroke: #e8edf1; stroke-width: 1; stroke-dasharray: 2,2; }
            .bar-llm { fill: #2c67e8; }
            .bar-embedding { fill: #7a2ce8; }
            .bar-compute { fill: #eb9b46; }
            .bar-error { fill: #eb5146; }
            .legend-text { font-size: 10px; fill: #1b2129; font-family: sans-serif; }
        </style>
        <line x1="${labelWidth}" y1="${timelineHeight - 30}" x2="${labelWidth + chartWidth}" y2="${timelineHeight - 30}" class="axis"/>
        <#list 0..5 as i>
            <#assign x = labelWidth + (i * chartWidth / 5)?int>
            <#assign time = (chartData.maxDurationMs * i / 5)?int>
            <text x="${x}" y="${timelineHeight - 10}" class="time-label">${time}ms</text>
            <line x1="${x}" y1="30" x2="${x}" y2="${timelineHeight - 35}" class="grid-line"/>
        </#list>
        <#list chartData.timelineEntries as entry>
            <#assign y = 30 + entry?index * 30>
            <#assign barX = labelWidth + (entry.startOffsetMs * timeScale)?int>
            <#assign barWidth = (entry.durationMs * timeScale)?int>
            <#if barWidth < 2><#assign barWidth = 2></#if>
            <#assign colorClass = (!entry.success)?then('bar-error', (entry.stepType == 'LLM')?then('bar-llm', (entry.stepType == 'EMBEDDING')?then('bar-embedding', 'bar-compute')))>
            <text x="${labelWidth - 5}" y="${y + 17}" class="row-label">${entry.modelId}</text>
            <rect x="${barX}" y="${y}" width="${barWidth}" height="24" class="${colorClass}" rx="3">
                <title>${entry.modelId} - ${entry.stepName}: ${entry.durationMs?c}ms (${entry.stepType})</title>
            </rect>
        </#list>
        <g transform="translate(${labelWidth + chartWidth - 220}, 5)">
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
        <#-- Conversation messages for agent metrics (multi-turn) -->
        <#if conversationMessages?has_content>
        <div class="sample-item">
            <dt>${i18n["summary.conversation"]!("Conversation")}</dt>
            <dd>
                <div class="conversation-list">
                    <#list conversationMessages as msg>
                    <div class="conversation-message message-${msg.type}">
                        <span class="message-role">
                            <#if msg.type == "human">${i18n["message.type.human"]!("User")}<#elseif msg.type == "ai">${i18n["message.type.ai"]!("Assistant")}<#else>${i18n["message.type.tool"]!("Tool")}</#if>:
                        </span>
                        <pre class="message-content">${msg.content}</pre>
                        <#if msg.toolCalls?has_content>
                        <div class="tool-calls">
                            <span class="tool-calls-label">${i18n["message.toolCalls"]!("Tool Calls")}:</span>
                            <#list msg.toolCalls as tc>
                            <code class="tool-call">${tc.name}(${tc.arguments})</code>
                            </#list>
                        </div>
                        </#if>
                    </div>
                    </#list>
                </div>
            </dd>
        </div>
        <#else>
        <#-- Single-turn metrics: userInput and response -->
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
            <dd><code style="color: #eb5146;">${excludedModels?join(", ")}</code></dd>
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
