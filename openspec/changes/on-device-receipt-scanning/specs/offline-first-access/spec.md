## REMOVED Requirements

### Requirement: Receipt AI requires an authenticated user
**Reason**: The requirement existed to stop receipt image data reaching Gemini and to protect the operator's API quota for unauthenticated users. Receipt scanning now runs entirely on the device, so there is no image egress to prevent and no per-scan cost to gate. Retaining the gate would make a fully local, offline-capable feature depend on a cloud account, contradicting the local-only access requirement in this same capability.

**Migration**: Receipt scanning is available to signed-out users. The receipt capture entry point no longer routes to sign-in guidance, and the transaction repository no longer rejects a scan without a session. Behavior for signed-in users is unchanged. Session policy remains injected and continues to govern synchronization decisions.

## ADDED Requirements

### Requirement: Receipt scanning is available without authentication
Cofinance SHALL allow receipt scanning and the resulting draft transaction flow while no Firebase session is active.

#### Scenario: Signed-out user selects receipt automation
- **WHEN** a signed-out user selects the receipt image or camera automation entry point
- **THEN** the app SHALL open the receipt capture flow
- **AND** it SHALL NOT present sign-in guidance or navigate to sign-in

#### Scenario: Signed-out scan reaches the repository
- **WHEN** a receipt scan request reaches the transaction repository without an authenticated session
- **THEN** the repository SHALL perform the scan

#### Scenario: Signed-out scan produces a local draft
- **WHEN** a signed-out user completes a receipt scan
- **THEN** a draft transaction SHALL be created in on-device storage and opened for review

#### Scenario: Signed-in user selects receipt automation
- **WHEN** an authenticated user selects the receipt image or camera automation entry point
- **THEN** the receipt capture and scan flow SHALL remain available
