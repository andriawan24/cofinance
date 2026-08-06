---
name: openspec-verify-change
description: Independently audit an OpenSpec change before completion is claimed - verifying every requirement, scenario, task checkbox, test/build evidence, strict validation, and delta-spec sync state. Use when the user asks to verify, audit, double-check, or confirm a change is really done, and before archiving.
license: MIT
compatibility: Requires openspec CLI.
metadata:
  author: openspec
  version: "1.0"
---

Audit one OpenSpec change against its own artifacts and produce a completion verdict. This skill is **read-only for implementation code**: it verifies, it does not fix. Report findings and let the user decide whether to route fixes to `openspec-continue` or `openspec-apply-change`.

**Store selection:** If the user names a store (a store is a standalone OpenSpec repo registered on this machine) or the work lives in one, run `openspec store list --json` to discover registered store ids, then pass `--store <id>` on the commands that read specs and changes (`status`, `instructions`, `list`, `show`, `validate`, `doctor`, `context`). Without a store, commands act on the nearest local `openspec/` root.

**Input**: Optionally a change name. If omitted, infer from conversation context; if ambiguous, run `openspec list --json` and select via **AskUserQuestion**. Never auto-pick among multiple candidates.

**Steps**

1. **Resolve the change**

   Announce: "Verifying change: <name>" and how to override.

2. **Load ground truth**

   ```bash
   openspec status --change "<name>" --json
   openspec instructions apply --change "<name>" --json
   ```

   Use `planningHome`, `changeRoot`, `artifactPaths`, `actionContext`, and `contextFiles` from the JSON instead of assuming paths. Read every context file completely — proposal, design, delta specs, tasks. A verdict built on skimmed artifacts is worthless.

3. **Run strict validation**

   ```bash
   openspec validate "<name>" --strict
   ```

   Record the exact output. A failure here is a blocking finding regardless of task state.

4. **Audit requirement-by-requirement (the core pass)**

   Build a table with one row per requirement/scenario from the delta specs. For each row, find concrete evidence in the diff (`git status --short`, `git diff`) and in test/build output:

   - **Verified** — code implements the observable behavior AND executed evidence confirms it.
   - **Unverified** — code appears present but no runtime/test evidence exists.
   - **Missing** — no implementation found.
   - **Contradicted** — implementation diverges from the requirement's stated behavior.

   Requirements are the authority; task checkboxes are only claims. Verify claims, do not inherit them.

5. **Audit task checkboxes against evidence**

   For every `- [x]` task, confirm implementation and verification both actually happened. Flag any checked task whose evidence is absent as a **false-complete** — this is the highest-value finding this skill produces. List remaining `- [ ]` tasks with their blockers.

6. **Re-run required verification**

   Run the verification commands `tasks.md` and the affected specifications require. For this repo that typically means:

   ```bash
   ./gradlew :composeApp:allTests
   ```

   plus any platform build or run required by the change (Android/iOS). Never infer runtime success from compilation alone. If evidence needs a device, simulator, credential, external service, or user action you cannot perform, say so explicitly and name the exact command or action still required — do not mark it verified.

7. **Check delta-spec sync state**

   Use `artifactPaths.specs.existingOutputPaths` to locate delta specs and compare each with `openspec/specs/<capability>/spec.md`. Report whether main specs are in sync, and what `openspec-sync-specs` would apply if not. Do not sync here.

8. **Check scope discipline**

   Compare the diff against the proposal's scope and non-goals. Flag work outside the change's scope, and requested scope that was silently narrowed or dropped.

9. **Issue the verdict**

   Exactly one of:
   - **COMPLETE** — every requirement verified, every task genuinely complete, required tests/builds pass, strict validation passes, delta specs synced or sync explicitly deferred by the user.
   - **INCOMPLETE** — anything above fails. Enumerate every gap.

   Never soften an INCOMPLETE verdict. A change with unverifiable manual gates is INCOMPLETE with a named blocker, not "complete pending verification".

**Output format**

```
## Verification: <change-name> (schema: <schema-name>)

**Verdict:** COMPLETE | INCOMPLETE

### Requirements
| Requirement / Scenario | Status | Evidence |
|---|---|---|
| ... | Verified / Unverified / Missing / Contradicted | file:line, test name, or command output |

### Tasks
- Progress: N/M checked
- False-completes: <checked tasks without evidence, or "none">
- Remaining: <unchecked tasks + blockers>

### Verification run
- `<command>` -> pass/fail (key output)
- Unavailable evidence: <what, and the exact command/action/platform needed>

### Strict validation
`openspec validate "<name>" --strict` -> pass/fail (output)

### Spec sync
<in sync | delta specs pending sync: list | no delta specs>

### Scope
<out-of-scope work, dropped scope, or "matches proposal">

### Blocking gaps
1. ...
```

**Guardrails**
- Read-only for implementation code. Do not edit source, do not check or uncheck task boxes, do not sync specs, do not archive.
- Evidence or it did not happen — no verdict line rests on "looks correct".
- Compilation is not runtime evidence. Passing unrelated tests is not evidence for a requirement.
- Report the CLI's exact failure output, not a paraphrase.
- If artifacts are missing or the change is not apply-ready, say so and stop — there is nothing meaningful to verify yet.
- Suggest `openspec-continue` for gaps and `openspec-archive-change` only after a COMPLETE verdict and an explicit user request.
