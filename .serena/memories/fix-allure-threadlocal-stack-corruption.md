# Fix: Allure ThreadLocal Stack Corruption (Issue #1)

## Problem

`AllureMetricExecutionListener.beforeMetricEvaluation()` calls `lifecycle.startStep()` on the main thread (pushes to ThreadLocal), but `afterMetricEvaluation()` calls `lifecycle.stopStep()` on an async thread (pops from wrong ThreadLocal). Main thread's stack retains stale step UUID, causing subsequent metric steps to be created under a stopped parent and lost in reports.

## Solution (Option A)

- `beforeMetricEvaluation()`: Add immediate `lifecycle.stopStep(metricStepUuid)` after `lifecycle.startStep(...)` — cleans ThreadLocal on main thread.
- `afterMetricEvaluation()`: Replace `lifecycle.stopStep()` with `lifecycle.updateStep()` — storage-only, no ThreadLocal interaction.

## Key Insight

- `lifecycle.updateStep(uuid, consumer)` works on global Allure storage by UUID, independent of ThreadLocal.
- `AllureAttachmentWriter.addAttachmentToStep()` already uses this pattern successfully.
- NLP metrics unaffected (same thread for both callbacks).

## Files Changed

- `spring-ai-ragas-allure/.../AllureMetricExecutionListener.java` — bugfix
- `spring-ai-ragas-allure/.../AllureMetricExecutionListenerTest.java` — updated tests

## Plan

See `specs/fix-allure-threadlocal-stack-corruption.md`
