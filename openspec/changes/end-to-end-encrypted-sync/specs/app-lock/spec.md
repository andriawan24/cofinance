## ADDED Requirements

### Requirement: A PIN gates access to the decrypted data key
Cofinance SHALL allow the user to set a PIN that is required to obtain the decrypted data key, and SHALL derive key material from the PIN rather than comparing a stored PIN value.

#### Scenario: PIN is set
- **WHEN** a user sets a PIN in the security settings
- **THEN** a copy of the data key wrapped using the PIN SHALL be stored on the device

#### Scenario: Correct PIN is entered
- **WHEN** a user enters the correct PIN at the unlock screen
- **THEN** the data key SHALL be obtained and the app SHALL become usable

#### Scenario: Incorrect PIN is entered
- **WHEN** a user enters an incorrect PIN
- **THEN** the data key SHALL NOT be obtained and finance data SHALL NOT be displayed

#### Scenario: Unlock is derivation-based
- **WHEN** the unlock implementation is inspected
- **THEN** access to the data key SHALL depend on material derived from the PIN, and SHALL NOT depend on comparing the entered PIN against a stored value

### Requirement: PIN-derived key material is bound to the device and is not uploaded
Cofinance SHALL combine the PIN-derived material with a non-extractable device-held secret, and SHALL NOT upload the PIN-wrapped copy of the data key.

#### Scenario: PIN material requires the device
- **WHEN** the PIN-wrapped copy of the data key is taken to another device along with the correct PIN
- **THEN** it SHALL NOT yield the data key

#### Scenario: Cloud material is inspected
- **WHEN** the cloud-stored key material is inspected
- **THEN** it SHALL NOT contain a PIN-wrapped copy of the data key

#### Scenario: Memory-hard derivation is applied
- **WHEN** the PIN is converted to key material
- **THEN** a memory-hard derivation SHALL be applied before it is combined with the device-held secret

### Requirement: Repeated failed unlock attempts are throttled and ultimately destroy local key material
Cofinance SHALL count consecutive failed PIN attempts in secure storage, SHALL impose escalating delay from the fifth consecutive failure, SHALL destroy local key material at the tenth consecutive failure, SHALL NOT expose the threshold as a user setting, and SHALL NOT allow the counter to be reset by reinstalling the app.

#### Scenario: Consecutive failures impose delay
- **WHEN** a user submits a fifth or later consecutive incorrect PIN
- **THEN** that attempt SHALL be delayed, starting at 30 seconds and doubling with each further failure up to a cap of 5 minutes

#### Scenario: Threshold is reached
- **WHEN** consecutive failed attempts reach ten
- **THEN** local key material SHALL be destroyed and the app SHALL require recovery-phrase restoration

#### Scenario: Threshold is inspected in settings
- **WHEN** a user opens security settings
- **THEN** no control to change the failed-attempt threshold SHALL be offered

#### Scenario: Data remains recoverable after destruction
- **WHEN** local key material has been destroyed by failed attempts
- **THEN** the user SHALL be able to restore access with the correct recovery phrase

#### Scenario: Reinstall does not reset the counter
- **WHEN** the app is reinstalled after failed attempts have accumulated
- **THEN** the accumulated count SHALL NOT be reset

#### Scenario: Successful unlock clears the counter
- **WHEN** a correct PIN is entered before the threshold is reached
- **THEN** the failed attempt count SHALL reset

### Requirement: Biometric unlock is optional and requires a PIN
Cofinance SHALL offer biometric unlock only when a PIN is already set, SHALL treat it as a shortcut over the PIN, and SHALL fall back to the PIN when biometric authentication is unavailable or invalidated.

#### Scenario: Biometric is enabled without a PIN
- **WHEN** a user attempts to enable biometric unlock and no PIN is set
- **THEN** the app SHALL require the PIN to be set first

#### Scenario: Biometric succeeds
- **WHEN** a user with biometric enabled authenticates successfully at the unlock screen
- **THEN** the data key SHALL be obtained without entering the PIN

#### Scenario: Biometric fails or is cancelled
- **WHEN** biometric authentication fails or the user dismisses it
- **THEN** the app SHALL offer PIN entry

#### Scenario: Biometric enrollment changes
- **WHEN** the device's enrolled biometrics change after biometric unlock was enabled
- **THEN** the biometric path SHALL no longer grant access
- **AND** the user SHALL be able to regain access with the PIN

### Requirement: Lock settings are user-controlled from the profile page
Cofinance SHALL expose PIN, biometric, and auto-lock timeout controls in the profile experience, and SHALL require the current PIN to change or remove lock settings.

#### Scenario: User opens security settings
- **WHEN** a signed-in user with encryption set up opens the profile page
- **THEN** controls to set or change the PIN, toggle biometric unlock, choose an auto-lock timeout, and view the recovery phrase SHALL be available

#### Scenario: Auto-lock options are offered
- **WHEN** a user opens the auto-lock timeout control
- **THEN** the options SHALL be immediately, 1 minute, 5 minutes, and 15 minutes
- **AND** 1 minute SHALL be the value in effect for a user who has never changed it

#### Scenario: User disables biometric
- **WHEN** a user turns off biometric unlock
- **THEN** subsequent unlocks SHALL require the PIN

#### Scenario: Lock settings are changed
- **WHEN** a user changes or removes the PIN or changes the auto-lock timeout
- **THEN** the current PIN SHALL be required first

### Requirement: The data key is held in memory only and is cleared on auto-lock
Cofinance SHALL hold the decrypted data key in memory only, SHALL clear it when the app has been backgrounded beyond the auto-lock timeout, and SHALL require unlocking again before finance data is displayed.

#### Scenario: App is backgrounded past the timeout
- **WHEN** the app has been in the background longer than the configured auto-lock timeout
- **THEN** the in-memory data key SHALL be cleared

#### Scenario: App is resumed after auto-lock
- **WHEN** the user returns to the app after the key has been cleared
- **THEN** the unlock screen SHALL be presented before finance data is displayed

#### Scenario: App is resumed within the timeout
- **WHEN** the user returns to the app before the timeout elapses
- **THEN** the app SHALL resume without requiring unlock

#### Scenario: Key material is not persisted unwrapped
- **WHEN** application storage is inspected while the app is unlocked
- **THEN** the data key SHALL NOT be present in unwrapped form outside process memory

### Requirement: The lock does not gate local-only use before encryption setup
Cofinance SHALL NOT require an app lock from a user who has not completed encryption setup.

#### Scenario: Local-only user launches the app
- **WHEN** a user who has never signed in launches the app
- **THEN** no unlock screen SHALL be presented
