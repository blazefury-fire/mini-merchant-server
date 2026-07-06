---
name: plan-task
description: Draft a detailed step-by-step implementation plan for a feature, following conventions.md, then stop for my review. Does not execute or change code.
disable-model-invocation: true
---

# Plan a feature: $ARGUMENTS

Write an implementation plan for the feature above. The plan feeds a full-file executor (DeepSeek, via `/ship`), so the plan must contain the **complete final content of every file that changes** — never partial snippets, diffs, or "insert here / keep the rest" instructions. Anything you leave out, the executor will invent, and it will regress unrelated code.

## Rules

1. If `conventions.md` exists at the repo root, read it and follow it exactly (package layout, naming, migrations, error handling, test style).
2. For every file the feature modifies, **read its current full content first** (skip only for brand-new files). You cannot reproduce a file in full unless you have read it in full.
3. Decide the complete set of files to create or modify. Cover every layer the feature actually needs (entity, repository, service, controller, migration, config, tests) — but only the layers it needs.
4. Write the plan to `plan_<slug>.md` at the repo root, where `<slug>` is a 1–3 word slug of the feature (e.g. `plan_abc.md`, `plan_redis_cache.md`).

## Plan file format — full content only

The plan body is nothing but a list of files. For each file, emit exactly:

- a header line: `### CREATE: <path>` or `### MODIFY: <path>`
- immediately followed by **one** fenced code block containing the **entire final content of that file** — every import, every existing line, plus your change — exactly as it should appear on disk.

No prose, no rationale, no step numbering, no snippets, no ellipses (`// ...`), no "unchanged"/"rest of file" placeholders. A `MODIFY` block must be the whole file with the edit already applied, not just the changed region. If you cannot produce the whole file, go read it first.

## Hard stop

After writing the plan file, print the list of file paths it contains and the exact plan filename, then STOP. Do NOT call `execute_plan_deepseek`, and do NOT change any code. I will review the plan and run `/ship` myself when it's approved.
