<#ftl output_format="HTML">
<!DOCTYPE html>
<html lang="${language!'en'}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${metricName} - ${i18n["report.title"]}</title>
    <style>
        :root {
            --color-excellent: #2e7d32;
            --color-good: #4caf50;
            --color-moderate: #ff9800;
            --color-poor: #f44336;
            --color-unknown: #9e9e9e;
            --color-bg: #ffffff;
            --color-border: #e0e0e0;
            --color-text: #333333;
            --color-text-secondary: #666666;
            --color-code-bg: #f5f5f5;
        }

        * { box-sizing: border-box; }

        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
            line-height: 1.6;
            color: var(--color-text);
            background: var(--color-bg);
            margin: 0;
            padding: 20px;
        }

        .report-container {
            max-width: 1200px;
            margin: 0 auto;
        }

        .report-header {
            background: linear-gradient(135deg, #1a237e 0%, #283593 100%);
            color: white;
            padding: 24px;
            border-radius: 8px;
            margin-bottom: 24px;
        }

        .report-header h1 {
            margin: 0 0 16px 0;
            font-size: 24px;
            font-weight: 600;
        }

        .metadata {
            display: flex;
            flex-wrap: wrap;
            gap: 16px;
            align-items: center;
        }

        .score-badge {
            font-size: 20px;
            font-weight: 700;
            padding: 8px 16px;
            border-radius: 4px;
            background: rgba(255,255,255,0.2);
        }

        .score-badge.excellent { background: var(--color-excellent); }
        .score-badge.good { background: var(--color-good); }
        .score-badge.moderate { background: var(--color-moderate); }
        .score-badge.poor { background: var(--color-poor); }
        .score-badge.unknown { background: var(--color-unknown); }

        .duration, .timestamp {
            font-size: 14px;
            opacity: 0.9;
        }

        .report-block {
            background: white;
            border: 1px solid var(--color-border);
            border-radius: 8px;
            margin-bottom: 24px;
            overflow: hidden;
        }

        .block-header {
            background: #f5f5f5;
            padding: 16px 24px;
            border-bottom: 1px solid var(--color-border);
        }

        .block-header h2 {
            margin: 0;
            font-size: 18px;
            font-weight: 600;
        }

        .block-content {
            padding: 24px;
        }

        .chart-container {
            margin-bottom: 24px;
        }

        .chart-container h3 {
            font-size: 16px;
            margin: 0 0 12px 0;
            color: var(--color-text-secondary);
        }

        .chart-container svg {
            max-width: 100%;
            height: auto;
        }

        .sample-section {
            margin-top: 24px;
        }

        .sample-section h3 {
            font-size: 16px;
            margin: 0 0 16px 0;
        }

        .sample-item {
            margin-bottom: 16px;
        }

        .sample-item dt {
            font-weight: 600;
            font-size: 14px;
            color: var(--color-text-secondary);
            margin-bottom: 4px;
        }

        .sample-item dd {
            margin: 0;
        }

        pre {
            background: var(--color-code-bg);
            padding: 16px;
            border-radius: 4px;
            overflow-x: auto;
            font-size: 13px;
            line-height: 1.5;
            margin: 0;
            white-space: pre-wrap;
            word-wrap: break-word;
        }

        .step-execution {
            border: 1px solid var(--color-border);
            border-radius: 8px;
            margin-bottom: 16px;
            overflow: hidden;
        }

        .step-header {
            background: #f5f5f5;
            padding: 12px 16px;
            display: flex;
            align-items: center;
            gap: 12px;
            flex-wrap: wrap;
        }

        .step-name {
            font-weight: 600;
            font-size: 15px;
        }

        .step-type-badge {
            font-size: 11px;
            padding: 2px 8px;
            border-radius: 4px;
            text-transform: uppercase;
            font-weight: 600;
        }

        .step-type-badge.llm { background: #2196f3; color: white; }
        .step-type-badge.embedding { background: #9c27b0; color: white; }
        .step-type-badge.compute { background: #ff9800; color: white; }

        .step-duration {
            font-size: 13px;
            color: var(--color-text-secondary);
        }

        .step-stats {
            font-size: 13px;
        }

        .step-stats .success { color: var(--color-good); }
        .step-stats .failure { color: var(--color-poor); }

        .step-content {
            padding: 16px;
        }

        .prompt-section, .model-results {
            margin-bottom: 16px;
        }

        .prompt-section h4, .model-results h4 {
            font-size: 14px;
            margin: 0 0 8px 0;
            color: var(--color-text-secondary);
        }

        .model-result {
            border: 1px solid var(--color-border);
            border-radius: 4px;
            margin-bottom: 8px;
            overflow: hidden;
        }

        .model-result.success { border-left: 4px solid var(--color-good); }
        .model-result.failure { border-left: 4px solid var(--color-poor); }

        .model-header {
            background: #fafafa;
            padding: 8px 12px;
            display: flex;
            align-items: center;
            gap: 12px;
            font-size: 13px;
        }

        .model-id {
            font-weight: 600;
            font-family: monospace;
        }

        .model-status {
            padding: 2px 6px;
            border-radius: 3px;
            font-size: 11px;
            font-weight: 600;
        }

        .model-result.success .model-status { background: #e8f5e9; color: var(--color-good); }
        .model-result.failure .model-status { background: #ffebee; color: var(--color-poor); }

        .model-duration {
            color: var(--color-text-secondary);
        }

        .model-response, .model-error {
            padding: 12px;
        }

        .model-response h5, .model-error h5 {
            font-size: 12px;
            margin: 0 0 8px 0;
            color: var(--color-text-secondary);
        }

        .exclusions-section {
            margin-top: 24px;
        }

        .exclusions-table {
            width: 100%;
            border-collapse: collapse;
            font-size: 14px;
        }

        .exclusions-table th, .exclusions-table td {
            padding: 12px;
            text-align: left;
            border-bottom: 1px solid var(--color-border);
        }

        .exclusions-table th {
            background: #f5f5f5;
            font-weight: 600;
        }

        .methodology-content {
            line-height: 1.8;
        }

        .methodology-content h1 { font-size: 20px; margin: 0 0 16px 0; }
        .methodology-content h2 { font-size: 18px; margin: 24px 0 12px 0; }
        .methodology-content h3 { font-size: 16px; margin: 20px 0 10px 0; }

        .methodology-content table {
            width: 100%;
            border-collapse: collapse;
            margin: 16px 0;
        }

        .methodology-content th, .methodology-content td {
            padding: 10px 12px;
            border: 1px solid var(--color-border);
            text-align: left;
        }

        .methodology-content th {
            background: #f5f5f5;
        }

        .methodology-content code {
            background: var(--color-code-bg);
            padding: 2px 6px;
            border-radius: 3px;
            font-size: 13px;
        }

        .steps-schema {
            margin: 24px 0;
        }

        .steps-diagram {
            display: flex;
            align-items: center;
            flex-wrap: wrap;
            gap: 8px;
        }

        .step-box {
            background: #e3f2fd;
            border: 1px solid #90caf9;
            border-radius: 4px;
            padding: 8px 12px;
            font-size: 13px;
        }

        .step-box.llm { background: #e3f2fd; border-color: #2196f3; }
        .step-box.embedding { background: #f3e5f5; border-color: #9c27b0; }
        .step-box.compute { background: #fff3e0; border-color: #ff9800; }

        .step-number {
            font-weight: 700;
            margin-right: 4px;
        }

        .step-arrow {
            font-size: 18px;
            color: var(--color-text-secondary);
        }

        details {
            margin-top: 8px;
        }

        details summary {
            cursor: pointer;
            font-size: 13px;
            color: var(--color-text-secondary);
        }

        .context-list {
            margin: 0;
            padding-left: 20px;
        }

        .context-list li {
            margin-bottom: 12px;
        }

        /* Collapsible block styles */
        .block-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            cursor: pointer;
            user-select: none;
        }

        .block-header:hover {
            background: #eeeeee;
        }

        .collapse-btn {
            width: 28px;
            height: 28px;
            border: none;
            background: #e0e0e0;
            border-radius: 4px;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 18px;
            font-weight: bold;
            color: #666;
            transition: all 0.2s ease;
            flex-shrink: 0;
        }

        .collapse-btn:hover {
            background: #d0d0d0;
            color: #333;
        }

        .block-content {
            transition: max-height 0.3s ease-out, padding 0.3s ease-out, opacity 0.2s ease-out;
            overflow: hidden;
        }

        .report-block.collapsed .block-content {
            max-height: 0;
            padding-top: 0;
            padding-bottom: 0;
            opacity: 0;
        }

        .report-block.collapsed .block-header {
            border-bottom: none;
        }

        /* Trace Viewer - Langfuse style */
        .trace-viewer {
            display: flex;
            border: 1px solid var(--color-border);
            border-radius: 6px;
            overflow: hidden;
            min-height: 500px;
            max-height: 700px;
        }

        .trace-tree {
            width: 320px;
            min-width: 280px;
            border-right: 1px solid var(--color-border);
            display: flex;
            flex-direction: column;
            background: #fafafa;
        }

        .tree-header {
            padding: 12px 16px;
            font-weight: 600;
            font-size: 13px;
            color: var(--color-text-secondary);
            border-bottom: 1px solid var(--color-border);
            background: #f5f5f5;
        }

        .tree-content {
            overflow-y: auto;
            flex: 1;
            padding: 8px 0;
        }

        .tree-node {
            user-select: none;
        }

        .tree-item {
            display: flex;
            align-items: center;
            padding: 6px 12px;
            cursor: pointer;
            font-size: 13px;
            gap: 8px;
            border-left: 3px solid transparent;
        }

        .tree-item:hover {
            background: #f0f0f0;
        }

        .tree-item.selected {
            background: #e3f2fd;
            border-left-color: #2196f3;
        }

        .tree-item.error {
            color: var(--color-poor);
        }

        .model-item {
            padding-left: 32px;
        }

        .tree-expand {
            width: 16px;
            text-align: center;
            color: var(--color-text-secondary);
            font-size: 10px;
            flex-shrink: 0;
        }

        .tree-expand.has-children {
            cursor: pointer;
        }

        .tree-type-badge {
            font-size: 10px;
            padding: 2px 6px;
            border-radius: 3px;
            font-weight: 600;
            text-transform: uppercase;
            flex-shrink: 0;
        }

        .tree-type-badge.llm { background: #e3f2fd; color: #1565c0; }
        .tree-type-badge.embedding { background: #f3e5f5; color: #7b1fa2; }
        .tree-type-badge.compute { background: #fff3e0; color: #e65100; }
        .tree-type-badge.model { background: #e8f5e9; color: #2e7d32; }

        .tree-name {
            flex: 1;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .tree-duration {
            font-size: 11px;
            color: var(--color-text-secondary);
            flex-shrink: 0;
        }

        .tree-error-badge {
            background: var(--color-poor);
            color: white;
            width: 16px;
            height: 16px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 10px;
            font-weight: bold;
            flex-shrink: 0;
        }

        .tree-children {
            border-left: 1px dashed #ccc;
            margin-left: 20px;
        }

        /* Right panel: Details */
        .trace-details {
            flex: 1;
            display: flex;
            flex-direction: column;
            overflow: hidden;
        }

        .details-header {
            padding: 12px 16px;
            font-weight: 600;
            font-size: 14px;
            border-bottom: 1px solid var(--color-border);
            background: #f5f5f5;
        }

        .details-content {
            flex: 1;
            overflow-y: auto;
            padding: 16px;
        }

        .details-placeholder {
            color: var(--color-text-secondary);
            text-align: center;
            padding: 40px;
        }

        .detail-section {
            margin-bottom: 20px;
        }

        .detail-header {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 12px;
        }

        .detail-type-badge {
            font-size: 11px;
            padding: 3px 8px;
            border-radius: 4px;
            font-weight: 600;
            text-transform: uppercase;
        }

        .detail-type-badge.llm { background: #e3f2fd; color: #1565c0; }
        .detail-type-badge.embedding { background: #f3e5f5; color: #7b1fa2; }
        .detail-type-badge.compute { background: #fff3e0; color: #e65100; }
        .detail-type-badge.model { background: #e8f5e9; color: #2e7d32; }

        .detail-name {
            font-weight: 600;
            font-size: 16px;
        }

        .detail-status {
            font-size: 12px;
            padding: 2px 8px;
            border-radius: 3px;
            font-weight: 500;
        }

        .detail-status.success { background: #e8f5e9; color: var(--color-good); }
        .detail-status.error { background: #ffebee; color: var(--color-poor); }

        .detail-meta {
            display: flex;
            gap: 20px;
            flex-wrap: wrap;
            margin-bottom: 8px;
        }

        .meta-item {
            font-size: 13px;
        }

        .meta-label {
            color: var(--color-text-secondary);
        }

        .detail-section-title {
            font-weight: 600;
            font-size: 13px;
            color: var(--color-text-secondary);
            margin-bottom: 8px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .detail-code {
            background: var(--color-code-bg);
            border: 1px solid var(--color-border);
            border-radius: 4px;
            padding: 12px;
            font-size: 12px;
            line-height: 1.5;
            overflow-x: auto;
            white-space: pre-wrap;
            word-wrap: break-word;
            max-height: 300px;
            overflow-y: auto;
        }

        .detail-code.error {
            background: #ffebee;
            border-color: #ffcdd2;
            color: var(--color-poor);
        }

        .detail-code.stack-trace {
            font-size: 11px;
            max-height: 200px;
            color: var(--color-text-secondary);
        }

        .compute-info {
            background: #fff3e0;
            border: 1px solid #ffe0b2;
            border-radius: 4px;
            padding: 12px;
        }

        .compute-info p {
            margin: 0 0 8px 0;
            font-size: 13px;
        }

        .outputs-summary {
            display: flex;
            flex-direction: column;
            gap: 6px;
        }

        .output-item {
            display: flex;
            align-items: center;
            gap: 12px;
            padding: 8px 12px;
            background: #fafafa;
            border: 1px solid var(--color-border);
            border-radius: 4px;
            cursor: pointer;
            font-size: 13px;
        }

        .output-item:hover {
            background: #f0f0f0;
        }

        .output-item.error {
            border-left: 3px solid var(--color-poor);
        }

        .output-item.success {
            border-left: 3px solid var(--color-good);
        }

        .output-model {
            flex: 1;
            font-family: monospace;
            font-size: 12px;
        }

        .output-duration {
            color: var(--color-text-secondary);
            font-size: 12px;
        }

        .output-status {
            font-size: 11px;
            padding: 2px 6px;
            border-radius: 3px;
            font-weight: 500;
        }

        .output-status.success { background: #e8f5e9; color: var(--color-good); }
        .output-status.error { background: #ffebee; color: var(--color-poor); }

        /* Score Explanation Styles */
        .explanation-viewer {
            border: 1px solid var(--color-border);
            border-radius: 6px;
            overflow: hidden;
        }

        .explanation-intro {
            display: flex;
            align-items: flex-start;
            gap: 16px;
            padding: 16px;
            background: linear-gradient(to right, #f8f9fa, #fff);
            border-bottom: 1px solid var(--color-border);
        }

        .explanation-icon {
            width: 40px;
            height: 40px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 20px;
            font-weight: bold;
            flex-shrink: 0;
            background: #e0e0e0;
            color: #666;
        }

        .explanation-icon .icon-good {
            color: var(--color-good);
        }

        .explanation-icon .icon-bad {
            color: var(--color-poor);
        }

        .explanation-icon .icon-info {
            color: #2196f3;
        }

        .explanation-summary {
            flex: 1;
        }

        .explanation-title {
            font-weight: 600;
            font-size: 16px;
            margin-bottom: 4px;
        }

        .explanation-simple {
            font-size: 14px;
            color: var(--color-text-secondary);
            line-height: 1.5;
        }

        .explanation-main {
            display: flex;
            min-height: 400px;
            max-height: 600px;
        }

        .explanation-tree {
            width: 280px;
            min-width: 240px;
            border-right: 1px solid var(--color-border);
            display: flex;
            flex-direction: column;
            background: #fafafa;
        }

        .explanation-tree .tree-header {
            padding: 12px 16px;
            font-weight: 600;
            font-size: 13px;
            color: var(--color-text-secondary);
            border-bottom: 1px solid var(--color-border);
            background: #f5f5f5;
        }

        .explanation-tree .tree-content {
            overflow-y: auto;
            flex: 1;
            padding: 8px 0;
        }

        .exp-tree-node {
            user-select: none;
        }

        .exp-tree-item {
            display: flex;
            align-items: center;
            padding: 10px 16px;
            cursor: pointer;
            font-size: 13px;
            gap: 10px;
            border-left: 3px solid transparent;
            transition: all 0.15s ease;
        }

        .exp-tree-item:hover {
            background: #f0f0f0;
        }

        .exp-tree-item.selected {
            background: #e3f2fd;
            border-left-color: #2196f3;
        }

        .exp-tree-number {
            width: 24px;
            height: 24px;
            background: #e0e0e0;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 600;
            font-size: 12px;
            color: #666;
            flex-shrink: 0;
        }

        .exp-tree-item.selected .exp-tree-number {
            background: #2196f3;
            color: white;
        }

        .interpretation-node .exp-tree-number {
            background: #ff9800;
            color: white;
        }

        .exp-tree-name {
            flex: 1;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .exp-tree-delta {
            font-size: 10px;
            padding: 2px 6px;
            background: #fff3e0;
            color: #e65100;
            border-radius: 3px;
            font-weight: 600;
        }

        .explanation-details {
            flex: 1;
            overflow-y: auto;
            padding: 20px;
        }

        /* Explanation detail styles */
        .exp-detail-header {
            display: flex;
            align-items: center;
            gap: 12px;
            margin-bottom: 16px;
        }

        .exp-step-badge {
            font-size: 11px;
            padding: 4px 10px;
            background: #e3f2fd;
            color: #1565c0;
            border-radius: 4px;
            font-weight: 600;
            text-transform: uppercase;
        }

        .exp-step-title {
            font-weight: 600;
            font-size: 16px;
        }

        .exp-detail-description {
            font-size: 14px;
            color: var(--color-text-secondary);
            margin-bottom: 16px;
            line-height: 1.5;
        }

        .exp-input-section {
            margin-bottom: 16px;
            padding: 12px;
            background: #e3f2fd;
            border: 1px solid #90caf9;
            border-radius: 4px;
        }

        .exp-input-label {
            font-size: 12px;
            font-weight: 600;
            color: #1565c0;
            margin-bottom: 6px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .exp-input-data {
            font-size: 14px;
            color: var(--color-text);
            line-height: 1.5;
        }

        .exp-output-summary {
            background: #e8f5e9;
            border: 1px solid #c8e6c9;
            border-radius: 4px;
            padding: 12px 16px;
            font-size: 14px;
            font-weight: 500;
            margin-bottom: 20px;
        }

        .exp-section-title {
            font-weight: 600;
            font-size: 13px;
            color: var(--color-text-secondary);
            margin-bottom: 12px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .exp-items-section {
            margin-bottom: 20px;
        }

        .exp-items-list {
            display: flex;
            flex-direction: column;
            gap: 8px;
        }

        .exp-item {
            border: 1px solid var(--color-border);
            border-radius: 4px;
            padding: 12px;
            background: #fafafa;
        }

        .exp-item.passed {
            border-left: 4px solid var(--color-good);
            background: #f1f8e9;
        }

        .exp-item.failed {
            border-left: 4px solid var(--color-poor);
            background: #ffebee;
        }

        .exp-item-main {
            display: flex;
            align-items: flex-start;
            gap: 8px;
        }

        .exp-item-index {
            font-weight: 600;
            color: var(--color-text-secondary);
            min-width: 24px;
        }

        .exp-item-content {
            flex: 1;
            font-size: 14px;
            line-height: 1.4;
        }

        .exp-item-verdict {
            font-size: 11px;
            padding: 2px 8px;
            border-radius: 3px;
            font-weight: 600;
            flex-shrink: 0;
        }

        .exp-item-verdict.passed {
            background: #e8f5e9;
            color: var(--color-good);
        }

        .exp-item-verdict.failed {
            background: #ffebee;
            color: var(--color-poor);
        }

        .exp-item-reason {
            margin-top: 8px;
            font-size: 13px;
            color: var(--color-text-secondary);
            padding-left: 32px;
            font-style: italic;
        }

        .exp-item-source {
            margin-top: 4px;
            font-size: 12px;
            color: #9e9e9e;
            padding-left: 32px;
        }

        /* Model disagreement section */
        .exp-disagreement-section {
            margin-top: 20px;
            padding: 16px;
            background: #fff3e0;
            border: 1px solid #ffe0b2;
            border-radius: 4px;
        }

        .exp-disagreement-grid {
            display: flex;
            gap: 16px;
        }

        .exp-disagreement-col {
            flex: 1;
            min-width: 0;
        }

        .exp-col-header {
            font-weight: 600;
            font-size: 12px;
            margin-bottom: 8px;
            padding-bottom: 8px;
            border-bottom: 1px solid #ffe0b2;
        }

        .exp-disagreement-col.agree .exp-col-header {
            color: var(--color-good);
        }

        .exp-disagreement-col.disagree .exp-col-header {
            color: var(--color-poor);
        }

        .exp-model-item {
            font-size: 12px;
            font-family: monospace;
            padding: 4px 0;
        }

        /* Model numeric results section */
        .exp-model-results-section {
            margin-top: 20px;
            padding: 16px;
            background: #f8f9fa;
            border: 1px solid var(--color-border);
            border-radius: 4px;
        }

        .exp-model-results-table {
            margin-top: 12px;
        }

        .exp-table {
            width: 100%;
            border-collapse: collapse;
            font-size: 13px;
        }

        .exp-table th,
        .exp-table td {
            padding: 8px 12px;
            text-align: left;
            border-bottom: 1px solid var(--color-border);
        }

        .exp-table th {
            font-weight: 600;
            color: var(--color-text-secondary);
            font-size: 11px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            background: #f5f5f5;
        }

        .exp-table td:first-child {
            font-family: monospace;
            font-size: 12px;
        }

        .exp-table td:last-child {
            font-weight: 500;
            color: var(--color-text);
        }

        .exp-table tbody tr:hover {
            background: #f5f5f5;
        }

        /* Per-model breakdown styles */
        .exp-models-breakdown {
            margin-top: 16px;
        }

        .exp-model-card {
            border: 1px solid var(--color-border);
            border-radius: 6px;
            margin-bottom: 8px;
            background: #fff;
            overflow: hidden;
        }

        .exp-model-card.error {
            border-color: var(--color-poor);
            background: #fff5f5;
        }

        .exp-model-header {
            display: flex;
            align-items: center;
            gap: 12px;
            padding: 10px 14px;
            background: #f8f9fa;
            cursor: pointer;
            user-select: none;
        }

        .exp-model-header:hover {
            background: #eef0f2;
        }

        .exp-model-expand {
            font-size: 10px;
            color: #666;
            width: 14px;
        }

        .exp-model-name {
            flex: 1;
            font-family: monospace;
            font-size: 13px;
            font-weight: 500;
        }

        .exp-model-calc {
            font-family: monospace;
            font-size: 12px;
            color: var(--color-text-secondary);
            background: #e9ecef;
            padding: 2px 8px;
            border-radius: 4px;
        }

        .exp-model-score {
            font-weight: 600;
            font-size: 13px;
            min-width: 60px;
            text-align: right;
        }

        .exp-model-status.error {
            color: var(--color-poor);
            font-weight: 600;
        }

        .exp-model-details {
            padding: 12px 14px;
            border-top: 1px solid var(--color-border);
            background: #fafafa;
        }

        .exp-model-items-list {
            display: flex;
            flex-direction: column;
            gap: 6px;
        }

        .exp-model-item {
            display: flex;
            align-items: flex-start;
            gap: 8px;
            padding: 8px 10px;
            border-radius: 4px;
            font-size: 13px;
            line-height: 1.4;
        }

        .exp-model-item.passed {
            background: #e8f5e9;
            border-left: 3px solid var(--color-good);
        }

        .exp-model-item.failed {
            background: #ffebee;
            border-left: 3px solid var(--color-poor);
        }

        .exp-model-item.neutral {
            background: #f5f5f5;
            border-left: 3px solid #9e9e9e;
        }

        .exp-item-marker {
            font-weight: 600;
            width: 18px;
            text-align: center;
            flex-shrink: 0;
        }

        .exp-item-marker.passed {
            color: var(--color-good);
        }

        .exp-item-marker.failed {
            color: var(--color-poor);
        }

        .exp-item-marker.neutral {
            color: #757575;
        }

        .exp-item-text {
            flex: 1;
            word-break: break-word;
        }

        .exp-item-badge {
            font-size: 10px;
            font-weight: 600;
            padding: 2px 6px;
            border-radius: 3px;
            text-transform: uppercase;
            flex-shrink: 0;
        }

        .exp-item-badge.passed {
            background: var(--color-good);
            color: white;
        }

        .exp-item-badge.failed {
            background: var(--color-poor);
            color: white;
        }

        .exp-item-badge.neutral {
            background: #9e9e9e;
            color: white;
        }

        .exp-no-items {
            color: var(--color-text-secondary);
            font-style: italic;
            padding: 8px;
        }

        /* Score breakdown styles */
        .exp-scores-breakdown {
            margin-top: 16px;
        }

        .exp-scores-table {
            margin: 12px 0;
        }

        .exp-scores-table .model-name-cell {
            font-family: monospace;
            font-size: 12px;
        }

        .exp-scores-table .score-cell {
            font-weight: 600;
            text-align: right;
            min-width: 80px;
        }

        .score-excellent { color: var(--color-excellent); }
        .score-good { color: var(--color-good); }
        .score-moderate { color: var(--color-moderate); }
        .score-poor { color: var(--color-poor); }

        .exp-aggregation {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 12px 16px;
            background: #f0f4f8;
            border-radius: 6px;
            border: 2px solid var(--color-border);
            margin-top: 12px;
        }

        .exp-aggregation-label {
            font-size: 13px;
            color: var(--color-text-secondary);
            font-weight: 500;
        }

        .exp-aggregation-value {
            font-size: 18px;
            font-weight: 700;
        }

        /* Interpretation styles */
        .exp-interpretation {
            max-width: 600px;
        }

        .exp-formula-section,
        .exp-calculation-section {
            margin-bottom: 16px;
        }

        .exp-formula-label,
        .exp-calculation-label {
            font-size: 12px;
            color: var(--color-text-secondary);
            margin-bottom: 4px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .exp-formula,
        .exp-calculation {
            font-family: monospace;
            font-size: 14px;
            background: var(--color-code-bg);
            padding: 10px 14px;
            border-radius: 4px;
            border: 1px solid var(--color-border);
        }

        .exp-result-section {
            margin: 24px 0;
            text-align: center;
            padding: 24px;
            background: #f5f5f5;
            border-radius: 8px;
        }

        .exp-result-score {
            font-size: 36px;
            font-weight: 700;
            margin-bottom: 8px;
        }

        .exp-result-score.good {
            color: var(--color-good);
        }

        .exp-result-score.bad {
            color: var(--color-poor);
        }

        .exp-result-meaning {
            font-size: 14px;
            color: var(--color-text-secondary);
        }

        .exp-scale-section {
            margin-top: 24px;
        }

        .exp-scale-list {
            display: flex;
            flex-direction: column;
            gap: 4px;
        }

        .exp-scale-item {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 10px 12px;
            border-radius: 4px;
            font-size: 13px;
            background: #fafafa;
            border: 1px solid transparent;
        }

        .exp-scale-item.current {
            background: #e3f2fd;
            border-color: #2196f3;
        }

        .exp-scale-marker {
            width: 20px;
            color: #2196f3;
            font-weight: bold;
        }

        .exp-scale-name {
            font-weight: 600;
            min-width: 80px;
        }

        .exp-scale-range {
            font-family: monospace;
            font-size: 12px;
            color: var(--color-text-secondary);
            min-width: 60px;
        }

        .exp-scale-desc {
            flex: 1;
            color: var(--color-text-secondary);
        }

        .exp-scale-current {
            font-size: 10px;
            padding: 2px 8px;
            background: #2196f3;
            color: white;
            border-radius: 3px;
            font-weight: 600;
        }

        .no-explanation {
            padding: 40px;
            text-align: center;
            color: var(--color-text-secondary);
            font-style: italic;
        }
    </style>
</head>
<body>
    <div class="report-container">
        <header class="report-header">
            <h1>${metricName} - ${i18n["report.title"]}</h1>
            <div class="metadata">
                <span class="score-badge ${data.scoreClass}">${data.formattedScore}</span>
                <span class="duration">${i18n["report.duration"]}: ${totalDurationMs?c}ms</span>
                <#if startTime??>
                <span class="timestamp">${startTime?string}</span>
                </#if>
            </div>
        </header>

        <#-- Block 1: Summary (expanded by default) -->
        <section class="report-block" id="block-summary">
            <div class="block-header" onclick="toggleBlock('block-summary')">
                <h2>${i18n["block.summary"]}</h2>
                <button class="collapse-btn" aria-label="Toggle section">−</button>
            </div>
            <div class="block-content">
                <#include "blocks/summary-block.ftl">
            </div>
        </section>

        <#-- Block 2: Score Explanation (expanded by default if available) -->
        <#if data.hasScoreExplanation()>
        <section class="report-block" id="block-explanation">
            <div class="block-header" onclick="toggleBlock('block-explanation')">
                <h2>${i18n["block.explanation"]}</h2>
                <button class="collapse-btn" aria-label="Toggle section">−</button>
            </div>
            <div class="block-content">
                <#include "blocks/score-explanation-block.ftl">
            </div>
        </section>
        </#if>

        <#-- Block 3: Methodology (collapsed by default) -->
        <section class="report-block collapsed" id="block-methodology">
            <div class="block-header" onclick="toggleBlock('block-methodology')">
                <h2>${i18n["block.methodology"]}</h2>
                <button class="collapse-btn" aria-label="Toggle section">+</button>
            </div>
            <div class="block-content">
                <#include "blocks/methodology-block.ftl">
            </div>
        </section>

        <#-- Block 3: Execution Log (collapsed by default) -->
        <section class="report-block collapsed" id="block-execution">
            <div class="block-header" onclick="toggleBlock('block-execution')">
                <h2>${i18n["block.execution"]}</h2>
                <button class="collapse-btn" aria-label="Toggle section">+</button>
            </div>
            <div class="block-content">
                <#include "blocks/execution-log-block.ftl">
            </div>
        </section>
    </div>

    <script>
        function toggleBlock(blockId) {
            const block = document.getElementById(blockId);
            const btn = block.querySelector('.collapse-btn');
            block.classList.toggle('collapsed');
            btn.textContent = block.classList.contains('collapsed') ? '+' : '−';
        }
    </script>
</body>
</html>
