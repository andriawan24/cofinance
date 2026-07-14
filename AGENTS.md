# Cofinance - AGENTS.md

## Mandatory OpenSpec Workflow

OpenSpec is the source of truth for every implementation change in this repository. This policy applies to the root agent, built-in workers, custom agents, subagents, automated continuations, and especially work started or resumed through a goal.

An **implementation change** is any edit that changes application behavior, build behavior, architecture, dependencies, data contracts, tests for changed behavior, deployment behavior, or user-facing requirements. Read-only investigation and purely editorial corrections may proceed without a change.

### Before implementation

1. Run `openspec list --json` and inspect `openspec/specs/`.
2. Associate the request or active goal with exactly one relevant OpenSpec change.
   - If a matching change exists, run `openspec status --change "<name>" --json` and read every artifact path returned for proposal, design, specs, and tasks.
   - If multiple changes could match, do not guess; ask the user to select one.
   - If no matching change exists, use the `openspec-propose` workflow to create the change and all artifacts required by its schema before editing implementation files.
3. Run `openspec instructions apply --change "<name>" --json`.
4. Do not edit implementation files until the change is apply-ready and its artifacts pass `openspec validate "<name>" --strict`.

### During implementation

- Use the `openspec-apply-change` workflow and implement only work represented in the selected change's artifacts and `tasks.md`.
- Keep the active change name visible in plans, delegated prompts, and progress updates.
- Give every implementation subagent the change name. Each subagent must read this file, the change status, apply instructions, and returned context files before editing.
- Mark a task complete only after its implementation and proportional verification succeed.
- If implementation reveals missing requirements, scope, or a design conflict, update the appropriate OpenSpec artifact first, validate it, and only then continue coding.
- Never create a parallel plan in `plans/`, ad-hoc design documents, or chat-only task lists that compete with the OpenSpec artifacts.

### Goal-driven work

When a goal is created, resumed, or automatically continued:

- Treat the full goal objective as the scope to map to OpenSpec; do not narrow it to existing tasks.
- Record the selected OpenSpec change name in the working plan.
- Do not mark the goal complete merely because code compiles or some tasks are checked.
- Goal completion requires all in-scope OpenSpec tasks complete, requirement-by-requirement verification, relevant tests/builds passing, strict change validation passing, and delta specs synchronized to main specs when applicable.
- Archive a completed change only when the user explicitly requests finalization or archival.

### Completion gate

Before claiming an implementation is complete:

1. Re-run the verification commands required by `tasks.md` and the affected specifications.
2. Compare the final diff and runtime/build evidence against every applicable requirement and scenario.
3. Run `openspec validate "<name>" --strict`.
4. Use `openspec-sync-specs` when delta specs must update `openspec/specs/`, then validate the main specs.
5. Report remaining unchecked tasks or unverified requirements as incomplete; do not silently defer them.

Repository-scoped Codex roles are defined in `.codex/agents/`:

- `openspec-planner`: explores and prepares apply-ready OpenSpec artifacts without implementing code.
- `openspec-implementer`: executes tasks from one named, apply-ready change.
- `openspec-verifier`: independently audits requirements, task completion, tests, and validation before completion is claimed.

## Project Overview

Cofinance is a Kotlin Multiplatform (KMP) personal finance app targeting Android, iOS, Desktop (JVM), and Web (JS/WASM). It uses Compose Multiplatform for the UI layer.

## Build & Run

```bash
# Build the project
./gradlew build

# Run Android app
./gradlew :androidApp:installDebug

# Run Desktop app
./gradlew :composeApp:run

# Run tests
./gradlew :composeApp:allTests
```

### Build Configuration

A `local.properties` file is required with:
- `supabase.project_url`
- `supabase.public_api_key`
- `gemini.api_key`
- `google_auth_client_id`

These are **secrets** injected at build time via BuildKonfig and must never be committed (including into forks), included in screenshots, or printed to CI logs. `local.properties` is already in `.gitignore` — do not remove it. To share keys across machines or CI, use environment variables or a secret manager rather than checking the file in.

## Architecture

**MVVM + Clean Architecture** with Koin dependency injection.

```text
Presentation (pages/, components/)  →  Domain (usecases/, model/)  →  Data (repository/, datasource/)
```

- **ViewModels**: Use `StateFlow<UiState>` + sealed `UiEvent` classes for unidirectional data flow
- **Navigation**: Type-safe routes via Kotlin serialization (`Destinations.kt`)
- **Async results**: Wrapped in `ResultState<T>` (Loading, Success, Error)
- **DI modules**: `networkModule`, `repositoryModule`, `useCaseModule`, `viewModelModule` — all registered in `App.kt`

## Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| ViewModel | `[Feature]ViewModel` | `ActivityViewModel` |
| Screen | `[Feature]Screen` | `LoginScreen` |
| UiState | `[Feature]UiState` | `ActivityUiState` |
| UiEvent | `[Feature]UiEvent` (sealed class) | `ActivityUiEvent` |
| Repository | `[Entity]Repository` / `[Entity]RepositoryImpl` | `AuthenticationRepository` |
| UseCase | `[Action][Entity]UseCase` | `FetchUserUseCase` |
| Component | Descriptive name | `BalanceCard`, `TransactionItem` |

## Module Structure

- **composeApp/** — Main KMP module with all shared and platform-specific code
- **androidApp/** — Thin Android wrapper
- **iosApp/** — iOS Xcode project wrapper

### Source Sets

All shared code lives under `composeApp/src/commonMain/kotlin/id/andriawan/cofinance/`:

- `pages/` — Screen composables (one package per feature)
- `components/` — Reusable UI components
- `navigations/` — Route definitions
- `domain/usecases/` — Business logic
- `data/repository/` — Repository implementations
- `data/datasource/` — Supabase and Gemini data sources
- `di/` — Koin modules
- `theme/` — Material Design 3 theming
- `localization/` — English and Indonesian strings
- `utils/` — Extensions, helpers, enums

Platform-specific code uses `expect`/`actual` for: permissions, Google auth, camera, gallery picker, locale management.

## Key Libraries

- **Compose Multiplatform** 1.10.0 — UI framework
- **Supabase Kotlin** 3.3.0 — Auth, PostgREST, Realtime, Storage
- **Ktor** 3.4.0 — HTTP client
- **Koin** 4.1.1 — Dependency injection
- **Coil** 3.3.0 — Image loading
- **Google Generative AI** 0.9.0-1.1.0 — Gemini for receipt scanning
- **CameraK** 0.2.0 — Multiplatform camera
- **Kotlin Datetime** 0.7.1 — Date/time handling

## Code Style

- Kotlin official code style (`kotlin.code.style=official`)
- No automated linting tools configured (no ktlint/detekt)
- Package: `id.andriawan.cofinance`
- Android: minSdk 24, targetSdk 36, JVM 17
