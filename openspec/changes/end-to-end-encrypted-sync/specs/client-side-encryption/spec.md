## ADDED Requirements

### Requirement: Finance records are encrypted on the device before synchronization
Cofinance SHALL encrypt account and transaction records with an authenticated symmetric cipher on the device before writing them to the cloud, and SHALL decrypt them on read. Synchronized documents SHALL NOT contain readable finance values.

#### Scenario: Transaction is synchronized
- **WHEN** a signed-in user's transaction is written to the cloud
- **THEN** the stored document SHALL NOT contain a readable amount, category, date, notes, fee, or account identifier

#### Scenario: Account is synchronized
- **WHEN** a signed-in user's account is written to the cloud
- **THEN** the stored document SHALL NOT contain a readable name, group, balance, or account type

#### Scenario: Draft transaction is synchronized
- **WHEN** a draft transaction is included in a synchronization upload
- **THEN** it SHALL be encrypted on the same terms as any other transaction

#### Scenario: Synchronized record is read back
- **WHEN** an encrypted record is read from the cloud by a device holding the data key
- **THEN** the decrypted record SHALL equal the record that was uploaded

#### Scenario: Ciphertext is tampered with
- **WHEN** a stored record's ciphertext or envelope is modified
- **THEN** decryption SHALL fail and the record SHALL NOT be imported

### Requirement: Each encryption operation uses a fresh nonce
Cofinance SHALL generate a new random nonce for every encryption operation and SHALL NOT derive a nonce from record content or a counter.

#### Scenario: Identical record is encrypted twice
- **WHEN** the same record content is encrypted on two separate occasions
- **THEN** the two resulting envelopes SHALL carry different nonces

#### Scenario: Full snapshot is re-uploaded
- **WHEN** synchronization re-encrypts and re-uploads the complete local snapshot
- **THEN** no nonce from a previous upload SHALL be reused

### Requirement: The data key is wrapped independently by a device key and a recovery phrase
Cofinance SHALL protect the data encryption key with independently usable wrapped copies: one bound to a non-extractable device key, and one derived from the user's recovery phrase. The data key SHALL NOT be transmitted or stored in unwrapped form.

#### Scenario: Key material is stored
- **WHEN** encryption setup completes
- **THEN** the stored key material SHALL contain a wrapped copy for the device key and a wrapped copy for the recovery phrase
- **AND** it SHALL NOT contain the unwrapped data key

#### Scenario: Device key is used for ordinary access
- **WHEN** the app opens on the device that completed setup
- **THEN** the data key SHALL be obtainable from the device-bound wrapped copy without the recovery phrase

#### Scenario: Device-bound material is not uploaded
- **WHEN** the cloud-stored key material is inspected
- **THEN** it SHALL contain only the recovery-phrase wrapped copy

#### Scenario: Key material accommodates future copies
- **WHEN** the stored key material structure is inspected
- **THEN** it SHALL represent wrapped copies as a collection so that an additional copy can be added without re-encrypting existing records

#### Scenario: Key material precedes encrypted records
- **WHEN** encryption setup runs
- **THEN** key material SHALL be stored before the first encrypted record is written

### Requirement: Device key material is held in non-extractable platform storage
Cofinance SHALL hold the device key in hardware-backed platform key storage such that the private key cannot be exported from the device.

#### Scenario: Android device key is inspected
- **WHEN** the Android device key is created
- **THEN** it SHALL reside in the platform keystore and SHALL NOT be exportable

#### Scenario: iOS device key is inspected
- **WHEN** the iOS device key is created
- **THEN** it SHALL reside in platform key storage restricted to this device and SHALL NOT be exportable

#### Scenario: Stronger hardware isolation is available
- **WHEN** the device reports stronger hardware key isolation than the baseline
- **THEN** the device key SHALL be created using it

### Requirement: A 12-word recovery phrase is generated and offered for keeping at setup
Cofinance SHALL generate a 12-word recovery phrase encoding at least 128 bits of entropy, SHALL offer to copy it to the clipboard or save it to a file, SHALL NOT require the user to re-enter any of its words, and SHALL state that losing it means losing access to synchronized data.

#### Scenario: Phrase is generated
- **WHEN** encryption setup begins
- **THEN** a 12-word phrase SHALL be generated from the standard wordlist

#### Scenario: Phrase is still on screen
- **WHEN** the user has been shown the phrase and has not acknowledged saving it
- **THEN** setup SHALL NOT complete
- **AND** synchronization SHALL NOT begin

#### Scenario: User keeps the phrase by copying or saving it
- **WHEN** the user chooses to copy the phrase or save it as a file
- **THEN** the 12 words SHALL be handed to the clipboard or written to a file the user can reach
- **AND** the outcome SHALL be reported, reporting failure when nothing was copied or written

#### Scenario: User acknowledges saving the phrase
- **WHEN** the user acknowledges that the phrase is saved
- **THEN** setup SHALL complete and the wrapped key material SHALL be stored

#### Scenario: Consequence of loss is stated
- **WHEN** the phrase is presented to the user
- **THEN** the app SHALL state that no recovery is possible without it

### Requirement: The recovery phrase can be re-displayed from security settings behind fresh PIN entry
Cofinance SHALL allow a user who has completed encryption setup to view their recovery phrase again from security settings, and SHALL require the PIN to be entered at that moment even when the app is already unlocked.

#### Scenario: User requests the phrase from settings
- **WHEN** a user with encryption set up chooses to view their recovery phrase in security settings
- **THEN** the app SHALL require the current PIN before displaying it

#### Scenario: PIN is not supplied
- **WHEN** the user dismisses or fails the PIN prompt raised by the re-display request
- **THEN** the phrase SHALL NOT be displayed

#### Scenario: App is already unlocked
- **WHEN** a re-display request is made while the app is unlocked and the data key is held in memory
- **THEN** the PIN SHALL still be required

### Requirement: Data is restored on a new device using the recovery phrase
Cofinance SHALL restore access to synchronized data on a device holding no key material when the user supplies a valid recovery phrase, and SHALL reject an invalid one.

#### Scenario: Valid phrase is supplied on a clean install
- **WHEN** a user signs in on a device with no key material and enters their correct recovery phrase
- **THEN** the data key SHALL be unwrapped, synchronized records SHALL be decrypted into local storage, and a device-bound wrapped copy SHALL be created on the new device

#### Scenario: Invalid phrase is supplied
- **WHEN** a user enters a phrase that does not unwrap the stored key material
- **THEN** restoration SHALL fail, no data SHALL be imported, and the user SHALL be able to retry

#### Scenario: No phrase is available
- **WHEN** a user has no key material on the device and does not supply a valid phrase
- **THEN** the app SHALL NOT expose synchronized data
- **AND** the app SHALL NOT offer an operator-assisted recovery path

### Requirement: Encryption setup is mandatory for synchronization and gated at sign-in
Cofinance SHALL require completed encryption setup before any finance data is synchronized, and SHALL require it at sign-in rather than at first launch.

#### Scenario: Local-only user launches the app
- **WHEN** a user who has never signed in launches the app
- **THEN** the app SHALL NOT require encryption setup or a recovery phrase

#### Scenario: User signs in for the first time
- **WHEN** a user completes sign-in with no existing key material
- **THEN** the app SHALL require encryption setup before synchronizing

#### Scenario: Synchronization is attempted without setup
- **WHEN** a synchronization is requested and encryption setup has not completed
- **THEN** no finance data SHALL be uploaded

### Requirement: Encrypted records carry an envelope version and key identifier
Cofinance SHALL store with each encrypted record the envelope version and the identifier of the key used, so that records can be identified as encrypted and interpreted by later versions.

#### Scenario: Encrypted record is inspected
- **WHEN** an encrypted record is read
- **THEN** it SHALL carry an envelope version and a key identifier

### Requirement: Cryptographic behavior is verifiable without a device
Cofinance SHALL implement envelope handling, key wrapping, and recovery phrase encoding in shared code so that they are exercisable in common tests.

#### Scenario: Common tests exercise the crypto path
- **WHEN** the common test suite runs
- **THEN** envelope round-trip, nonce uniqueness, wrap and unwrap for both wrapped-copy types, tampered-ciphertext rejection, and phrase generation and restoration SHALL be covered without a device, emulator, or cloud backend
