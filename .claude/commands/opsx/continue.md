---
name: "OPSX: Continue"
description: Resume implementation of an active OpenSpec change from its current state (Experimental)
category: Workflow
tags: [workflow, artifacts, experimental]
---

Resume one existing OpenSpec change without rebuilding its plan or weakening its completion gates.

Use the **Skill tool** to invoke `openspec-continue`, passing the change name if one was given (e.g. `/opsx:continue add-auth`). If omitted, infer it from conversation context; if multiple active changes could govern the request, run `openspec list --json` and select via **AskUserQuestion**.

Use this instead of `/opsx:apply` when work was interrupted, when a change is partially implemented across turns, or when checked tasks and remaining verification need reconciling before further edits. Never create a new change merely because the current one is partially complete.

Audit the result with `/opsx:verify` before claiming completion, and archive only on explicit user request.
