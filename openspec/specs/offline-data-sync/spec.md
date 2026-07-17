# Finance Data Access Specification

## Purpose

Define the shared account and transaction persistence boundary after removal of app-managed local synchronization.

## Requirements

### Requirement: Repositories depend on a shared database contract
Account and transaction repositories SHALL use `CofinanceDatabase` rather than selecting a platform storage implementation directly.

#### Scenario: Firebase database is injected
- **WHEN** the application composition root starts
- **THEN** it SHALL provide the shared Firestore-backed database implementation through Koin
