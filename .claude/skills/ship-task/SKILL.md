---
name: ship-task
description: Execute an approved plan file with the DeepSeek executor MCP, then pull the diff and review the changes against conventions.md.
disable-model-invocation: true
---

# Ship an approved plan: $ARGUMENTS

`$ARGUMENTS` is the path to an approved plan file (e.g. `plan_abc.md`). If it is empty, use the most recently modified `plan_*.md` at the repo root and tell me which one you picked before doing anything.

## Steps

1. Re-read the plan file from disk and give me a one-line summary of each step, so I can confirm the saved file is the version I approved.
2. Determine the test command for this repo: if a `pom.xml` exists use `mvn -q test`; if a `build.gradle` or `build.gradle.kts` exists use `./gradlew test`. Otherwise ask me.
3. Call `execute_plan_deepseek` with:
   - `repo_path`: the absolute path of the current repo root
   - `plan_path`: the plan file above
   - `conventions_path`: `conventions.md` at the repo root if it exists (otherwise omit it)
   - `test_command`: the command from step 2
4. When it finishes, call `get_review_diff_deepseek` for the repo and review the changes like a senior engineer: flag anything that violates `conventions.md`, any logic flaw, and anything the tests don't actually cover. Give me a clear pass/fail and what you'd change.
5. If the output is unusable, say so plainly and offer to call `revert_changes_deepseek` to reset the repo to HEAD — but do not revert without my go-ahead.

## Note

This runs the heavy work on the DeepSeek executor MCP, not on you. Do not implement the feature yourself; your job here is to drive the tool and review its output.
