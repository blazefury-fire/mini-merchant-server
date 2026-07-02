---
name: plan-task
description: Draft a detailed step-by-step implementation plan for a feature, following conventions.md, then stop for my review. Does not execute or change code.
disable-model-invocation: true
---

# Plan a feature: $ARGUMENTS

Write a detailed, step-by-step implementation plan for the feature described above.

## Steps

1. If `conventions.md` exists at the repo root, read it and follow it exactly (package layout, naming, migrations, error handling, test style).
2. Break the feature into concrete, ordered steps. Cover every layer the feature actually touches — for a typical backend feature that means entity/model, repository, service, controller/handler, DB migration, and tests, but include only the layers this feature needs.
3. Make each step specific: exact file paths, class/method names, key logic, edge cases, and what each test asserts. A cheap executor model will implement this literally, so ambiguity here becomes wasted correction cycles later.
4. Save the plan to `plan_<slug>.md` at the repo root, where `<slug>` is a 1–3 word slug of the feature (e.g. `plan_abc.md`, `plan_redis_cache.md`).
5. Print the full plan in the chat, and tell me the exact filename you saved.

## Hard stop

STOP after saving and printing. Do NOT call `execute_plan_deepseek`, and do NOT change any code. I will review the plan and run `/ship` myself when it's approved.
