---
name: "OPSX: Verify"
description: Independently audit an OpenSpec change before completion is claimed (Experimental)
category: Workflow
tags: [workflow, artifacts, verification, experimental]
---

Audit one OpenSpec change against its own artifacts and produce a completion verdict.

Use the **Skill tool** to invoke `openspec-verify-change`, passing the change name if one was given (e.g. `/opsx:verify add-auth`). If omitted, infer it from conversation context; if ambiguous, run `openspec list --json` and select via **AskUserQuestion**.

This action is read-only for implementation code: it verifies, it does not fix. Route gaps to `/opsx:apply` or the `openspec-continue` skill, and archive only after a COMPLETE verdict plus an explicit user request.
