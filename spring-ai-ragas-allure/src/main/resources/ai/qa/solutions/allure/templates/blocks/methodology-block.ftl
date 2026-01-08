<#ftl output_format="HTML">
<#-- Methodology Content (rendered from Markdown) -->
<div class="methodology-content">
    <#noautoesc>${methodologyHtml!'<p>Methodology documentation not available.</p>'}</#noautoesc>
</div>

<#-- Execution Steps Schema -->
<#if steps?has_content>
<div class="steps-schema">
    <h3>Execution Flow</h3>
    <div class="steps-diagram">
        <#list steps as step>
            <div class="step-box ${step.stepType?lower_case}">
                <span class="step-number">${step.stepNumber}.</span>
                <span class="step-name">${step.stepName}</span>
                <span class="step-type">[${step.stepType}]</span>
            </div>
            <#if step?has_next>
                <div class="step-arrow">&rarr;</div>
            </#if>
        </#list>
    </div>
</div>
</#if>

<#-- Score Interpretation is included in the methodology content above -->
