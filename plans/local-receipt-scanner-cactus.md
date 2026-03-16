# feat: On-Device Receipt Scanner with Cactus AI Engine

> **Status**: Plan reviewed. Decisions made. Starting with spike.
>
> **Reviewer feedback applied**:
> - Spike first to validate Cactus SDK works with receipts (all 3 reviewers agreed)
> - Keep moderate complexity (simplified interface but retain ModelStatus for UX)
> - Collapse 6 phases → 3 (Spike → Implement → Polish)
> - Desktop: disable scan feature (not Gemini fallback)
> - Delete GeminiDataSource/GeminiHelper after Cactus is proven
> - Decouple ReceiptScanResponse from GeminiHelper constants
> - Fix existing bugs: `var` → `val` in PreviewUiState, null-file loading stuck

## Overview

Replace the current cloud-based Gemini API receipt scanning with fully on-device AI processing using the **Cactus AI inference engine (v1.7)**. The primary goal is **privacy**: receipt images must never leave the user's device. All inference happens locally using a vision-language model (VLM) with structured output via function calling.

**Current state**: `GeminiDataSource.scanReceipt()` sends receipt `ByteArray` to Google's cloud API and receives structured JSON.
**Target state**: `CactusReceiptScanner.scanReceipt()` runs inference entirely on-device using LFM2.5-VL-1.6B (INT4) with zero network calls.

---

## Problem Statement / Motivation

1. **Privacy**: Users' financial receipt images are sent to Google's Gemini API. This is a privacy risk for a personal finance app. Users should have the option to process receipts without any data leaving their device.
2. **Offline capability**: The current scanner requires internet connectivity. With PowerSync already enabling offline-first data, the scanner is the last remaining online-only feature.
3. **Cost**: Gemini API calls incur per-request costs that scale with user count.

---

## Model Recommendation

### Primary: **LFM2.5-VL-1.6B (INT4 quantization)**

| Attribute | Value |
|-----------|-------|
| Parameters | 1.6B |
| Quantization | INT4 |
| Download size | ~800MB-1GB |
| RAM footprint | ~1-2GB |
| Vision support | Yes (image + text input) |
| Function calling | Yes |
| Decode speed (iPhone 17 Pro) | ~48 tok/s |
| Decode speed (Galaxy S25 Ultra) | ~37 tok/s |
| Apple NPU optimized | Yes |

**Why this model:**
- Best balance of accuracy and mobile performance for structured data extraction
- Supports both vision (image input) and function calling (structured JSON output)
- Fits within typical mobile RAM budgets (3-6GB available)
- INT4 quantization minimizes battery drain and storage

### Fallback: **LFM2-VL-450M (INT4)**
- Lighter (450M params, ~300MB), faster, but may struggle with complex receipts
- Good for low-end devices with <4GB RAM
- Can be offered as a "lite" option in settings

---

## Technical Approach

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        commonMain                            │
│                                                              │
│  ReceiptScannerService (interface)                           │
│    ├── scanReceipt(image: ByteArray): ReceiptScanResponse    │
│    ├── isModelDownloaded(): Boolean                          │
│    ├── downloadModel(onProgress: (Float) -> Unit)            │
│    └── getModelStatus(): ModelStatus                         │
│                                                              │
│  ModelStatus (sealed class)                                  │
│    ├── NotDownloaded                                         │
│    ├── Downloading(progress: Float)                          │
│    ├── Ready                                                 │
│    ├── Loading (into RAM)                                    │
│    └── Error(message: String)                                │
├──────────────────────────┬───────────────────────────────────┤
│       mobileMain         │          nonMobileMain            │
│   (Android + iOS)        │       (Desktop + Web)             │
│                          │                                   │
│  CactusReceiptScanner    │  GeminiReceiptScanner (existing)  │
│  (implements interface)  │  OR UnsupportedReceiptScanner     │
│  Uses Cactus SDK         │  (throws/returns error)           │
│  Fully on-device         │                                   │
└──────────────────────────┴───────────────────────────────────┘
```

### Source Set Hierarchy Change

Current:
```
commonMain
├── nonWebMain (Android + iOS + Desktop) → PowerSync
│   ├── androidMain
│   ├── iosMain
│   └── desktopMain
└── webMain (JS + WasmJS)
    ├── jsMain
    └── wasmJsMain
```

New (add `mobileMain` intermediate):
```
commonMain
├── nonWebMain (Android + iOS + Desktop) → PowerSync
│   ├── mobileMain (Android + iOS only) → Cactus SDK
│   │   ├── androidMain
│   │   └── iosMain
│   └── desktopMain
└── webMain (JS + WasmJS)
    ├── jsMain
    └── wasmJsMain
```

---

## Implementation Phases

### Phase 1: Foundation - Cactus SDK Integration & Source Set Setup

**Goal**: Get Cactus SDK compiling in the project with the new source set hierarchy.

#### Tasks

- [ ] **Add `mobileMain` intermediate source set** in `composeApp/build.gradle.kts`
  ```kotlin
  // In sourceSets block, after nonWebMain setup:
  val mobileMain by creating {
      dependsOn(nonWebMain)
  }
  val androidMain by getting { dependsOn(mobileMain) }
  val iosMain by getting { dependsOn(mobileMain) }
  ```

- [ ] **Add Cactus Kotlin SDK dependency** in `gradle/libs.versions.toml`
  ```toml
  [versions]
  cactus = "1.4.1-beta"

  [libraries]
  cactus-sdk = { module = "com.cactuscompute:cactus", version.ref = "cactus" }
  ```
  Then in `build.gradle.kts`:
  ```kotlin
  mobileMain.dependencies {
      implementation(libs.cactus.sdk)
  }
  ```

- [ ] **Create `ReceiptScannerService` interface** in `commonMain`
  - File: `composeApp/src/commonMain/kotlin/id/andriawan/cofinance/data/datasource/ReceiptScannerService.kt`
  ```kotlin
  interface ReceiptScannerService {
      suspend fun scanReceipt(image: ByteArray): ReceiptScanResponse
      suspend fun isModelReady(): Boolean
      fun getModelStatus(): StateFlow<ModelStatus>
      suspend fun downloadModel(onProgress: (Float) -> Unit = {})
      suspend fun deleteModel()
  }

  sealed class ModelStatus {
      data object NotDownloaded : ModelStatus()
      data class Downloading(val progress: Float) : ModelStatus()
      data object Ready : ModelStatus()
      data object LoadingModel : ModelStatus()
      data object Inferring : ModelStatus()
      data class Error(val message: String) : ModelStatus()
  }
  ```

- [ ] **Create `CactusReceiptScanner`** in `mobileMain`
  - File: `composeApp/src/mobileMain/kotlin/id/andriawan/cofinance/data/datasource/CactusReceiptScanner.kt`
  - Implements `ReceiptScannerService`
  - Uses `CactusLM` from the Cactus SDK

- [ ] **Create fallback implementation** for Desktop/Web
  - File: `composeApp/src/desktopMain/kotlin/id/andriawan/cofinance/data/datasource/ReceiptScannerFactory.kt` (wraps existing Gemini or returns `UnsupportedOperationException`)
  - File: `composeApp/src/webMain/kotlin/id/andriawan/cofinance/data/datasource/ReceiptScannerFactory.kt` (no-op)

- [ ] **Wire `expect/actual` factory function**
  - `commonMain`: `expect fun createReceiptScanner(): ReceiptScannerService`
  - `mobileMain`: `actual fun createReceiptScanner() = CactusReceiptScanner()`
  - `desktopMain`: `actual fun createReceiptScanner() = GeminiReceiptScanner(...)` or `UnsupportedReceiptScanner()`
  - `webMain`: `actual fun createReceiptScanner() = UnsupportedReceiptScanner()`

**Success criteria**: Project compiles on all targets. Cactus SDK is available in `mobileMain`.

---

### Phase 2: Model Management - Download, Storage & Lifecycle

**Goal**: Implement model download-on-demand with progress tracking and persistent caching.

#### Tasks

- [ ] **Implement model download in `CactusReceiptScanner`**
  - Use Cactus SDK's built-in `CactusLM.downloadModel(slug)` for model download
  - Model slug: `"lfm2.5-vl-1.6b"` (or equivalent from `lm.getModels().filter { it.supports_vision }`)
  - The SDK's `CactusModelManager` handles platform-specific storage paths internally
  - Track progress via SDK callbacks

- [ ] **Implement model status tracking**
  - `MutableStateFlow<ModelStatus>` exposed as `StateFlow`
  - States: `NotDownloaded → Downloading(progress) → Ready`
  - On app launch: check `CactusModelManager.isModelDownloaded(slug)` to set initial state

- [ ] **Add model integrity check**
  - Verify model file exists and is loadable before marking as `Ready`
  - If corrupted (load fails), delete and re-download

- [ ] **Implement model lifecycle management**
  - Load model on first `scanReceipt()` call, keep warm for 60 seconds after last use
  - Unload after 60s idle to free RAM
  - Use a `Job` with `delay(60_000)` that resets on each scan

- [ ] **Add storage pre-check before download**
  - Check available disk space >= model size + 100MB buffer
  - Surface `ModelStatus.Error("insufficient_storage")` if not enough space

- [ ] **Add model deletion** via `CactusModelManager.deleteModel(slug)` for settings screen

**Key files**:
- `CactusReceiptScanner.kt` (mobileMain)
- `CactusModelManager` (from SDK - handles platform-specific storage)

**Success criteria**: Model downloads with progress tracking, persists across app restarts, and loads successfully for inference.

---

### Phase 3: Core Inference - Vision + Structured Output

**Goal**: Implement receipt scanning using Cactus vision model with function calling for structured JSON extraction.

#### Tasks

- [ ] **Define the receipt extraction tool (function calling schema)**
  ```kotlin
  // In CactusReceiptScanner
  private val receiptExtractionTool = createTool(
      name = "extract_receipt_data",
      description = "Extract structured transaction data from a receipt image",
      parameters = mapOf(
          "total_price" to ToolParameter(
              type = "number",
              description = "Total transaction amount in the smallest currency unit (e.g., cents/rupiah). Exclude fees.",
              required = true
          ),
          "transaction_date" to ToolParameter(
              type = "string",
              description = "Transaction date in ISO 8601 format with timezone (e.g., 2026-03-15T14:30:00+07:00)",
              required = true
          ),
          "bank_name" to ToolParameter(
              type = "string",
              description = "Name of the bank or financial institution",
              required = true
          ),
          "fee" to ToolParameter(
              type = "number",
              description = "Transaction fee amount, null if no fee",
              required = false
          ),
          "transaction_type" to ToolParameter(
              type = "string",
              description = "Transaction type: QRIS, Transfer, or null",
              required = false
          ),
          "category" to ToolParameter(
              type = "string",
              description = "Transaction category based on receiver/description. Default: Others",
              required = true
          ),
          "sender_name" to ToolParameter(
              type = "string",
              description = "Sender account holder name",
              required = false
          ),
          "sender_account_number" to ToolParameter(
              type = "string",
              description = "Sender account number",
              required = false
          ),
          "receiver_name" to ToolParameter(
              type = "string",
              description = "Receiver account holder name",
              required = false
          ),
          "receiver_account_number" to ToolParameter(
              type = "string",
              description = "Receiver account number",
              required = false
          )
      )
  )
  ```

- [ ] **Implement `scanReceipt()` in `CactusReceiptScanner`**
  ```kotlin
  override suspend fun scanReceipt(image: ByteArray): ReceiptScanResponse {
      _modelStatus.value = ModelStatus.Inferring

      // Save image to temp file (Cactus needs file paths for vision)
      val tempImagePath = saveTempImage(image)

      try {
          val result = lm.generateCompletion(
              messages = listOf(
                  ChatMessage(
                      content = SYSTEM_INSTRUCTION,
                      role = "system"
                  ),
                  ChatMessage(
                      content = "Extract all transaction data from this receipt image.",
                      role = "user",
                      images = listOf(tempImagePath)
                  )
              ),
              params = CactusCompletionParams(
                  tools = listOf(receiptExtractionTool),
                  forceTools = true, // Force tool calling for structured output
                  maxTokens = 500,
                  temperature = 0.1f // Low temperature for deterministic extraction
              )
          )

          return parseToolCallToResponse(result)
      } finally {
          deleteTempFile(tempImagePath)
          _modelStatus.value = ModelStatus.Ready
      }
  }
  ```

- [ ] **Define system instruction** (ported from GeminiHelper)
  ```kotlin
  private const val SYSTEM_INSTRUCTION = """
  You are an expert financial data extractor. When given a receipt image,
  use the extract_receipt_data tool to return the structured transaction data.
  For dates, use ISO 8601 format with timezone.
  For prices, use the raw number without currency symbols.
  If a field is not visible in the receipt, omit it (do not guess).
  """
  ```

- [ ] **Implement `parseToolCallToResponse()`** - maps Cactus `ToolCall` to `ReceiptScanResponse`
  ```kotlin
  private fun parseToolCallToResponse(result: CactusCompletionResult?): ReceiptScanResponse {
      val toolCall = result?.toolCalls?.firstOrNull()
          ?: throw IllegalStateException("No tool call returned from model")

      val args = toolCall.arguments
      return ReceiptScanResponse(
          totalPrice = (args["total_price"] as? Number)?.toLong(),
          transactionDate = args["transaction_date"] as? String,
          bankName = args["bank_name"] as? String,
          fee = (args["fee"] as? Number)?.toLong(),
          transactionType = args["transaction_type"] as? String,
          category = (args["category"] as? String) ?: "Others",
          sender = if (args["sender_name"] != null) AccountResponse(
              name = args["sender_name"] as? String,
              accountNumber = args["sender_account_number"] as? String
          ) else null,
          receiver = if (args["receiver_name"] != null) AccountResponse(
              name = args["receiver_name"] as? String,
              accountNumber = args["receiver_account_number"] as? String
          ) else null
      )
  }
  ```

- [ ] **Add image preprocessing before inference**
  - Resize to max 1024px on longest side (reduce inference time)
  - Compress as JPEG quality 85 (balance quality vs. processing speed)
  - Use existing `compressImage()` expect/actual function

- [ ] **Add inference timeout** - 30 second maximum via `withTimeout(30_000)`

- [ ] **Add confidence check** - if `result.confidence < 0.3f`, treat as failed scan

**Success criteria**: Receipt images are processed on-device and return correct `ReceiptScanResponse` matching the existing data structure.

---

### Phase 4: UI Integration - Download UX & Processing States

**Goal**: Update PreviewScreen and ViewModel to handle model download flow and enhanced processing states.

#### Tasks

- [ ] **Update `PreviewUiState`** to support new states
  ```kotlin
  data class PreviewUiState(
      val showLoading: Boolean = false,
      val modelStatus: ModelStatus = ModelStatus.NotDownloaded,
      val scanError: UiText? = null
  )
  ```

- [ ] **Update `PreviewViewModel`** to check model status before scanning
  ```kotlin
  fun scanReceipt(file: ByteArray?) {
      viewModelScope.launch {
          if (file == null) {
              _uiState.update { it.copy(scanError = UiText.StringResource(R.string.error_file_not_found)) }
              return@launch  // FIX: don't leave showLoading = true
          }

          // Check if model is ready
          if (!receiptScanner.isModelReady()) {
              // Trigger download flow
              _uiState.update { it.copy(modelStatus = ModelStatus.Downloading(0f)) }
              receiptScanner.downloadModel { progress ->
                  _uiState.update { it.copy(modelStatus = ModelStatus.Downloading(progress)) }
              }
          }

          _uiState.update { it.copy(showLoading = true, modelStatus = ModelStatus.Inferring) }
          // ... existing scan logic using receiptScanner.scanReceipt(file)
      }
  }
  ```

- [ ] **Add model download bottom sheet** in `PreviewScreen`
  - Shown when model is not yet downloaded
  - Displays: model size (~850 MB), Wi-Fi recommendation, download button, "Later" option
  - Progress bar during download
  - "Cancel" option during download

- [ ] **Update processing indicator** in `PreviewScreen`
  - Replace simple `CircularProgressIndicator` with phased messages:
    - `ModelStatus.Downloading(progress)` → "Downloading AI model: ${(progress * 100).toInt()}%"
    - `ModelStatus.LoadingModel` → "Loading AI model..."
    - `ModelStatus.Inferring` → "Analyzing receipt..."
  - Show estimated time remaining for download

- [ ] **Add new string resources** in `values/strings.xml` and `values-id/strings.xml`
  ```xml
  <string name="model_download_title">Download AI Model</string>
  <string name="model_download_description">To scan receipts offline, download the AI model (%s). This only needs to be done once.</string>
  <string name="model_download_wifi_hint">Wi-Fi recommended</string>
  <string name="model_downloading">Downloading AI model: %d%%</string>
  <string name="model_loading">Loading AI model…</string>
  <string name="model_analyzing">Analyzing receipt…</string>
  <string name="error_insufficient_storage">Not enough storage space. Free up at least %s to download the AI model.</string>
  <string name="error_model_load_failed">Failed to load AI model. Try freeing memory by closing other apps.</string>
  <string name="error_inference_timeout">Receipt analysis timed out. Please try again.</string>
  <string name="error_inference_failed">Could not read the receipt. Please try again with a clearer photo.</string>
  ```

- [ ] **Fix existing bug**: `PreviewViewModel.scanReceipt()` when `file == null` leaves `showLoading = true` permanently. Reset it on early return.

- [ ] **Implement "Retake Photo"** button (currently TODO in `PreviewScreen.kt` line 153) - navigate back to CameraScreen.

- [ ] **Disable scan button on Desktop/Web** - check platform and show "Receipt scanning is only available on mobile" message.

**Success criteria**: First-time users see a download prompt, download completes with progress, and scanning shows appropriate loading states.

---

### Phase 5: DI Wiring & Migration

**Goal**: Wire everything through Koin and cleanly handle the Gemini → Cactus transition.

#### Tasks

- [ ] **Update Koin DI modules**
  - Add new module: `composeApp/src/commonMain/kotlin/id/andriawan/cofinance/di/AiModule.kt`
    ```kotlin
    val aiModule = module {
        single<ReceiptScannerService> { createReceiptScanner() }
    }
    ```
  - Update `RepositoryModule.kt`: inject `ReceiptScannerService` instead of `GeminiDataSource` into `TransactionRepositoryImpl`
  - Update `App.kt` module list: add `aiModule`

- [ ] **Update `TransactionRepositoryImpl`**
  - Replace `GeminiDataSource` dependency with `ReceiptScannerService`
  - `scanReceipt()` delegates to `receiptScanner.scanReceipt(image)`

- [ ] **Keep `GeminiDataSource` and `GeminiHelper`** for now (Desktop fallback)
  - Mark with `@Deprecated` annotation
  - Plan removal in a future PR when Desktop gets its own solution

- [ ] **Update `ScanReceiptUseCase`** if needed (likely no changes - it just calls repository)

- [ ] **Update `PreviewViewModel`** constructor to also receive `ReceiptScannerService` for model status tracking

**Success criteria**: DI graph compiles and resolves correctly on all platforms. Android/iOS use Cactus, Desktop uses Gemini fallback, Web returns error.

---

### Phase 6: Testing & Polish

**Goal**: Ensure reliability across devices and edge cases.

#### Tasks

- [ ] **Test with representative receipts** (10-20 samples):
  - Indonesian bank transfer receipts (BCA, Mandiri, BNI, BRI)
  - QRIS payment receipts
  - E-wallet receipts (GoPay, OVO, Dana)
  - Faded/crumpled receipts
  - Receipts in Indonesian and English

- [ ] **Test edge cases**:
  - First scan with no model (download flow)
  - Scan while offline (model already downloaded)
  - Low storage scenario
  - App backgrounded during download
  - Back navigation during inference (verify no orphaned DRAFTs)
  - Double-tap on "Use Photo" (verify deduplication)

- [ ] **Add inference timeout handling** - 30s max, show retry option

- [ ] **Add model deletion in Settings/Profile screen**
  - Show model size on disk
  - "Delete AI Model" button to free storage
  - Confirmation dialog

- [ ] **Performance testing**:
  - Measure TTFT and total inference time on target devices
  - Verify RAM usage stays within acceptable bounds
  - Check battery impact of single scan

**Success criteria**: Receipt scanning works reliably on Android and iOS with acceptable accuracy and performance.

---

## Alternative Approaches Considered

| Approach | Pros | Cons | Decision |
|----------|------|------|----------|
| **Cactus VLM + Function Calling** (chosen) | Single model, structured output, vision-native | ~1GB download, 2-10s inference | Selected - best accuracy for structured extraction |
| **ML Kit OCR + Small Text LLM** | Fast OCR (<1s), smaller model | Two-step pipeline, OCR misses context | Rejected - more complex, receipt layout varies too much |
| **Gemma-3-1B + Tesseract OCR** | Proven text model | No native vision, OCR quality varies | Rejected - Cactus VLM handles images directly |
| **Bundle model in APK/IPA** | No download step | 1GB app size, app store rejection risk | Rejected - unacceptable app size |
| **WebAssembly inference for Web** | Cross-platform | Very slow, large WASM binary, immature | Rejected - not viable for 1.6B model |

---

## Risk Analysis & Mitigation

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| LFM2.5-VL accuracy insufficient for receipts | Medium | High | Test with 20+ real receipts before Phase 3 is complete. Fall back to Gemini as user toggle if needed. |
| 1GB model download scares users | Medium | Medium | Clear UX: show size upfront, recommend Wi-Fi, allow "Later" option. |
| RAM pressure on low-end devices | Medium | High | Unload model after 60s idle. Detect available RAM before loading. Offer 450M lite model. |
| Cactus SDK is beta (1.4.1-beta) | Medium | Medium | Pin version, test thoroughly, monitor GitHub issues. |
| New `mobileMain` source set breaks build | Low | High | Implement in Phase 1. Test all targets compile before proceeding. |
| Function calling output doesn't match ReceiptScanResponse | Medium | High | Define tool schema carefully. Add robust parsing with fallbacks. |

---

## Acceptance Criteria

### Functional Requirements
- [ ] Receipt scanning works fully offline on Android and iOS
- [ ] No network calls are made during the scan process (verified via network proxy)
- [ ] Receipt images are deleted from temp storage after processing
- [ ] Model downloads on first use with progress indication
- [ ] Structured output matches existing `ReceiptScanResponse` fields
- [ ] Desktop shows appropriate fallback (Gemini or "not available" message)
- [ ] Web platform compiles without errors (no-op scanner)

### Non-Functional Requirements
- [ ] Inference completes within 15 seconds on mid-range devices (2023+)
- [ ] Model download is resumable on interruption
- [ ] RAM usage during inference stays below 2GB
- [ ] App does not crash if model loading fails (graceful error)

### Privacy Requirements
- [ ] Receipt image `ByteArray` is never written to persistent storage (only temp file for Cactus, deleted in `finally` block)
- [ ] No analytics or telemetry includes receipt content or scan results
- [ ] Model inference uses `InferenceMode.LOCAL` (Cactus default - no cloud handoff)
- [ ] `CACTUS_CLOUD_API_KEY` is never set in the app

---

## Key File References

### Files to Modify
| File | Change |
|------|--------|
| `composeApp/build.gradle.kts` | Add `mobileMain` source set, Cactus dependency |
| `gradle/libs.versions.toml` | Add Cactus version and library entry |
| `composeApp/src/commonMain/.../di/NetworkModule.kt` | Remove GeminiDataSource from mobile path |
| `composeApp/src/commonMain/.../di/RepositoryModule.kt` | Inject `ReceiptScannerService` |
| `composeApp/src/commonMain/.../data/repository/TransactionRepositoryImpl.kt` | Use `ReceiptScannerService` |
| `composeApp/src/commonMain/.../pages/preview/PreviewViewModel.kt` | Add model status, fix null-file bug |
| `composeApp/src/commonMain/.../pages/preview/PreviewScreen.kt` | Download UI, processing states, fix Retake TODO |
| `composeApp/src/commonMain/composeResources/values/strings.xml` | New string resources |
| `composeApp/src/commonMain/composeResources/values-id/strings.xml` | Indonesian translations |

### Files to Create
| File | Purpose |
|------|---------|
| `composeApp/src/commonMain/.../data/datasource/ReceiptScannerService.kt` | Common interface + `ModelStatus` |
| `composeApp/src/mobileMain/.../data/datasource/CactusReceiptScanner.kt` | On-device Cactus implementation |
| `composeApp/src/mobileMain/.../data/datasource/CactusHelper.kt` | System instruction, tool definitions |
| `composeApp/src/desktopMain/.../data/datasource/ReceiptScannerFactory.kt` | Desktop fallback |
| `composeApp/src/webMain/.../data/datasource/ReceiptScannerFactory.kt` | Web no-op |
| `composeApp/src/commonMain/.../di/AiModule.kt` | Koin module for AI dependencies |
| `composeApp/src/commonMain/.../components/ModelDownloadBottomSheet.kt` | Download prompt UI |

### Files to Keep (Deprecated)
| File | Reason |
|------|--------|
| `composeApp/src/commonMain/.../data/datasource/GeminiDataSource.kt` | Desktop fallback until fully removed |
| `composeApp/src/commonMain/.../utils/GeminiHelper.kt` | Desktop fallback until fully removed |

---

## External References

### Cactus SDK
- [Cactus v1.7 Documentation](https://cactuscompute.com/docs/v1.7)
- [Cactus Kotlin SDK (GitHub)](https://github.com/cactus-compute/cactus-kotlin) - Maven: `com.cactuscompute:cactus:1.4.1-beta`
- [Cactus Core Engine (GitHub)](https://github.com/cactus-compute/cactus)
- [Cactus Function Calling Docs](https://cactuscompute.com/docs/v1.7/function-calling)
- [Cactus Hybrid AI / Offline Mode](https://cactuscompute.com/docs/v1.7/hybrid-ai)

### Models
- [LFM2.5-VL on HuggingFace](https://huggingface.co/Cactus-Compute) - Vision-language model by LiquidAI
- [Cactus Model Catalog](https://cactuscompute.com/dashboard/models) (requires login)

### KMP Native Integration
- [Kotlin/Native C Interop Docs](https://kotlinlang.org/docs/native-c-interop.html)
- [KMP JNI + cinterop Example](https://github.com/kristoffer-paulsson/kotlin-monorepo-gradle-multiproject-jni-cinterop)

### Best Practices
- [On-Device LLMs: State of the Union (Meta)](https://v-chandra.github.io/on-device-llms/)
- [Apple Guideline 5.1.2(i) - AI Data Sharing](https://dev.to/arshtechpro/apples-guideline-512i-the-ai-data-sharing-rule-that-will-impact-every-ios-developer-1b0p)

---

## Open Questions

1. **Desktop fallback**: Should Desktop use Gemini cloud scanning, or disable the feature entirely? (Recommendation: disable for now, add cloud toggle later)
2. **Model size budget**: Is ~1GB acceptable for users, or should we default to the 450M lite model? (Recommendation: offer both, default to 1.6B with option to switch)
3. **DRAFT cleanup**: Should abandoned DRAFTs be cleaned up after 24 hours? (Recommendation: yes, add cleanup on app launch)
4. **Analytics**: Should we track scan success/failure rates locally (no content, just counts) for quality monitoring? (Recommendation: yes, local-only counters)
