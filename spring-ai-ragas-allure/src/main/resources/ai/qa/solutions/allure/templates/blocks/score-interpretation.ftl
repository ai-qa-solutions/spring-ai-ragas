<#ftl output_format="HTML">
<#-- Score Interpretation - Default view showing final score analysis -->

<#if data.hasScoreExplanation() && data.scoreExplanation.interpretation??>
<#assign interp = data.scoreExplanation.interpretation>

<div class="exp-interpretation">
    <div class="exp-detail-header">
        <span class="exp-step-title">${i18n["explanation.interpretation"]}</span>
    </div>

    <div class="exp-formula-section">
        <div class="exp-formula-label">${i18n["explanation.formula"]}</div>
        <div class="exp-formula">${interp.formula}</div>
    </div>

    <div class="exp-calculation-section">
        <div class="exp-calculation-label">${i18n["explanation.calculation"]}</div>
        <div class="exp-calculation">${interp.calculation}</div>
    </div>

    <div class="exp-result-section">
        <div class="exp-result-score <#if interp.isGood??>
            <#if interp.isGood>good<#else>bad</#if>
        </#if>">
            ${interp.scorePercent}
        </div>
        <div class="exp-result-meaning">${interp.meaning}</div>
    </div>

    <#-- Scale visualization -->
    <#if interp.scaleLevels?has_content>
    <div class="exp-scale-section">
        <div class="exp-section-title">${i18n["explanation.scale"]}</div>
        <div class="exp-scale-list">
            <#list interp.scaleLevels as level>
            <div class="exp-scale-item <#if level.current || level?index == (interp.currentLevelIndex!0)>current</#if>">
                <span class="exp-scale-marker">
                    <#if level.current || level?index == (interp.currentLevelIndex!0)>&#9658;</#if>
                </span>
                <span class="exp-scale-name">${level.name}</span>
                <span class="exp-scale-range">${level.range}</span>
                <span class="exp-scale-desc" title="${level.description}">${level.description}</span>
                <#if level.current || level?index == (interp.currentLevelIndex!0)>
                <span class="exp-scale-current">${i18n["explanation.yourResult"]}</span>
                </#if>
            </div>
            </#list>
        </div>
    </div>
    </#if>
</div>
</#if>
