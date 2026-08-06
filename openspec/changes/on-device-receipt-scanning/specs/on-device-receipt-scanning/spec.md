## ADDED Requirements

### Requirement: Receipt scanning runs entirely on the device
Cofinance SHALL extract transaction fields from a receipt image without transmitting the image, recognized text, or derived fields to any remote service.

#### Scenario: Scan performed with no network available
- **WHEN** a user scans a receipt while the device has no network connectivity
- **THEN** the scan SHALL complete and produce extracted fields
- **AND** the flow SHALL NOT report a network error

#### Scenario: Scan issues no network traffic
- **WHEN** a receipt scan is executed
- **THEN** the scan path SHALL NOT issue any outbound network request derived from the receipt image or its recognized text

#### Scenario: No remote AI credential is present in the build
- **WHEN** the generated build configuration is inspected for a release build of either platform
- **THEN** it SHALL NOT contain a Gemini API key or any other remote receipt-scanning credential

### Requirement: Platform text recognition is exposed through a common abstraction
Cofinance SHALL expose device text recognition to shared code as a platform-independent result containing recognized lines with normalized geometry, so that extraction logic is implemented once and is verifiable without a device.

#### Scenario: Android recognition produces a common result
- **WHEN** the Android implementation recognizes text in a sample receipt image
- **THEN** it SHALL return a result containing at least one recognized line with a bounding box expressed in normalized coordinates between 0 and 1

#### Scenario: iOS recognition produces a common result
- **WHEN** the iOS implementation recognizes text in the same sample receipt image
- **THEN** it SHALL return a result containing at least one recognized line with a bounding box expressed in normalized coordinates between 0 and 1

#### Scenario: Both platforms report geometry in one orientation convention
- **WHEN** a recognized token near the top of the image is inspected on either platform
- **THEN** its bounding box SHALL have a smaller top coordinate than a token near the bottom of the same image

#### Scenario: Extraction is testable without a device
- **WHEN** the shared extraction logic is exercised in common tests
- **THEN** it SHALL accept a recognition result constructed from a stored fixture without requiring a camera, emulator, or platform text recognition engine

### Requirement: Receipt parsing extracts the fields the transaction draft consumes
Cofinance SHALL derive total amount, transaction date, fee, and category from a recognition result, using formats and vocabulary of Indonesian receipts.

#### Scenario: Total amount is selected over other currency values
- **WHEN** a recognition result contains a subtotal, a tax line, a total line, and a cash-tendered line
- **THEN** the extracted amount SHALL be the value associated with the total line

#### Scenario: Indonesian currency formatting is normalized
- **WHEN** a recognized amount token uses an `Rp` prefix and `.` as a thousands separator
- **THEN** the extracted amount SHALL be the integer value with separators and currency prefix removed

#### Scenario: Indonesian date formats are recognized
- **WHEN** a recognition result contains a date written with an Indonesian month name in full or abbreviated form
- **THEN** the extracted date SHALL be an ISO 8601 value carrying a timezone

#### Scenario: Day-first ordering is assumed for ambiguous numeric dates
- **WHEN** a recognized date is numeric and both orderings are valid, such as `05/03/2026`
- **THEN** the extracted date SHALL interpret the first component as the day

#### Scenario: Receipt without a timezone is interpreted in the local market timezone
- **WHEN** a recognized date carries no timezone
- **THEN** the extracted date SHALL carry the Asia/Jakarta offset

#### Scenario: Fee is extracted from an administration line
- **WHEN** a recognition result contains a line matching an administration or transaction fee label with a currency value
- **THEN** the extracted fee SHALL be that value

#### Scenario: Receipt without a fee line reports no fee
- **WHEN** a recognition result contains no fee label
- **THEN** the extracted fee SHALL be zero

#### Scenario: Category is inferred from merchant or acquirer vocabulary
- **WHEN** a recognition result contains a merchant or payment acquirer name present in the category dictionary
- **THEN** the extracted category SHALL be the mapped category value

#### Scenario: Unknown merchant yields no category
- **WHEN** no merchant or acquirer in the recognition result matches the category dictionary
- **THEN** the extracted category SHALL be blank

### Requirement: Extraction reports per-field confidence
Cofinance SHALL report a confidence indication for each extracted field, and SHALL treat a date it cannot extract confidently as absent rather than guessed.

#### Scenario: Competing amount candidates lower amount confidence
- **WHEN** two currency values score comparably as the total
- **THEN** the reported amount confidence SHALL be lower than when a single candidate scores clearly highest

#### Scenario: No confident date is reported as blank
- **WHEN** no date candidate meets the minimum date confidence threshold
- **THEN** the extracted date SHALL be blank

#### Scenario: Blank date fails the scan as before
- **WHEN** extraction returns a blank transaction date
- **THEN** the scan flow SHALL report a receipt scan failure and SHALL NOT create a draft transaction

### Requirement: Low-confidence fields are flagged for user verification
Cofinance SHALL indicate on the draft review screen which extracted fields were low confidence, so the user is directed to check them before saving.

#### Scenario: Draft opens with a low-confidence amount
- **WHEN** a scan produces a low-confidence amount and a draft transaction is opened for review
- **THEN** the amount field SHALL be presented in a needs-verification state

#### Scenario: Draft opens with all fields confident
- **WHEN** a scan produces no low-confidence fields
- **THEN** no field SHALL be presented in a needs-verification state

#### Scenario: Extracted fields remain editable
- **WHEN** a draft transaction created from a scan is opened for review
- **THEN** amount, date, category, and fee SHALL be editable before the transaction is saved

### Requirement: Category inference improves from user corrections on the device
Cofinance SHALL record a merchant-to-category association when a user changes the category of a scanned draft, and SHALL apply it to later scans of the same merchant. This association SHALL remain on the device.

#### Scenario: Corrected merchant category is reused
- **WHEN** a user changes the category on a draft scanned from a given merchant, and later scans a receipt from the same merchant
- **THEN** the later scan SHALL extract the corrected category

#### Scenario: Learned associations are not synchronized
- **WHEN** merchant-to-category associations exist on the device and cloud synchronization runs
- **THEN** those associations SHALL NOT be uploaded

### Requirement: Extraction accuracy is measured against a stored corpus
Cofinance SHALL maintain stored recognition fixtures with expected extraction results spanning the common Indonesian receipt sources, and SHALL assert per-field accuracy thresholds so that a regression fails the build.

#### Scenario: Corpus spans the common receipt sources
- **WHEN** the fixture corpus is inspected
- **THEN** it SHALL include fixtures from bank transfer confirmations, QRIS payment slips, e-wallet receipts, and retail thermal receipts

#### Scenario: Accuracy regression fails the test suite
- **WHEN** extraction accuracy for amount or date falls below the recorded threshold across the corpus
- **THEN** the common test suite SHALL fail

#### Scenario: Fixtures contain no receipt images
- **WHEN** the fixture corpus is inspected
- **THEN** it SHALL contain recognized text and geometry only, and SHALL NOT contain receipt image data
