# feat: AI-powered transaction category prediction from receipt scan

## Overview

The AI receipt scan feature currently extracts transaction data (amount, date, bank name, etc.) but the **category field is not constrained to the app's predefined categories** and the **scanned category is discarded** before reaching the transaction form. This plan adds enum-constrained category prediction in the Gemini schema and wires the result through to the AddTransaction form as a pre-filled suggestion.

## Problem Statement

1. **Schema gap**: `GeminiHelper.kt:68-73` defines `category` as a free-text `FunctionType.STRING` — Gemini can return arbitrary values like "Food & Beverage" that don't match `TransactionCategory` enum names (e.g., `FOOD`).
2. **Data discarded**: `PreviewViewModel.kt:71-75` only maps `amount` and `date` from the scan result. The `category`, `fee`, `bankName`, `sender`, `receiver`, and `transactionType` fields are all thrown away.
3. **Draft restoration incomplete**: `AddTransactionViewModel.kt:142-171` loads a draft but only restores `amount` and `dateTime`, ignoring `category` and `fee`.

## Proposed Solution

### Step 1: Constrain Gemini schema with enum values

**File:** `GeminiHelper.kt:68-73`

Replace the free-text `category` schema with an enum-constrained schema using the SDK's `enum` parameter (confirmed available in `generativeai-google:0.9.0-1.1.0`):

```kotlin
"category" to Schema(
    name = "category",
    description = "Transaction category based on the merchant, receiver, or transaction description. Use OTHERS if no category clearly fits.",
    type = FunctionType.STRING,
    nullable = true,
    enum = listOf(
        "FOOD", "TRANSPORT", "HOUSING", "APPAREL", "HEALTH",
        "EDUCATION", "SUBSCRIPTION", "INTERNET", "DEBT", "GIFT",
        "ADMINISTRATION", "SALARY", "INVEST", "OTHERS"
    )
)
```

Key changes:
- Add `enum` parameter with all `TransactionCategory` entry names
- Change `nullable` to `true` so Gemini returns `null` instead of forcing "OTHERS" when uncertain — this lets the user decide
- Update description to guide classification
- Remove the "non nullable" instruction from the description

Also enhance `SYSTEM_INSTRUCTION` with category definitions so the model classifies more accurately:

```
Categories: FOOD (restaurants, groceries, cafes, food delivery), TRANSPORT (ride-hailing, fuel, parking, tolls, public transit),
HOUSING (rent, mortgage, utilities), APPAREL (clothing, shoes, accessories), HEALTH (medical, pharmacy, fitness),
EDUCATION (courses, books, tuition), SUBSCRIPTION (streaming, software subscriptions), INTERNET (internet service, phone bills),
DEBT (loan payments, credit card), GIFT (gifts, donations), ADMINISTRATION (government fees, legal, banking fees),
SALARY (salary, wages - income), INVEST (investments, dividends - income), OTHERS (anything else)
```

### Step 2: Pass category from scan result to draft transaction

**File:** `PreviewViewModel.kt:71-75`

Add `category` and `fee` to the `AddTransactionParam` when creating the draft:

```kotlin
val input = AddTransactionParam(
    amount = result.data.totalPrice,
    category = result.data.category.ifBlank { null },
    fee = if (result.data.fee > 0) result.data.fee else null,
    date = result.data.transactionDate,
    type = TransactionType.DRAFT
)
```

### Step 3: Restore category when loading draft in AddTransaction form

**File:** `AddTransactionViewModel.kt:142-171`

In `checkDraftTransaction()`, resolve the stored category string to a `TransactionCategory` enum and set it in the UI state:

```kotlin
transactions.getOrNull(0)?.transactions?.getOrNull(0)?.let { transaction ->
    val category = transaction.category.takeIf { it.isNotBlank() }
        ?.let { TransactionCategory.getCategoryByName(it) }

    _uiState.update {
        it.copy(
            transactionId = transaction.id,
            amount = it.amount.ifBlank { transaction.amount.toString() },
            dateTime = transaction.date.toDate(),
            expenseCategory = category,
            fee = if (transaction.fee > 0) transaction.fee.toString() else it.fee,
            includeFee = transaction.fee > 0
        )
    }
}
```

Note: Setting `expenseCategory` as the default since receipt scans are most commonly expenses. The user can switch to income type and select an income category if needed.

## Acceptance Criteria

- [ ] Gemini schema's `category` field uses `enum` parameter with all `TransactionCategory` entry names
- [ ] `category` is `nullable = true` so Gemini returns `null` when uncertain
- [ ] System instruction includes category definitions for better classification accuracy
- [ ] `PreviewViewModel` passes `category` and `fee` from scan result to `AddTransactionParam`
- [ ] `AddTransactionViewModel.checkDraftTransaction()` restores `category` and `fee` from draft
- [ ] Category appears pre-selected in the AddTransaction form after receipt scan
- [ ] User can still change the pre-selected category manually
- [ ] When Gemini returns `null` category, the category field is left empty for user selection

## Files to Modify

| File | Change |
|------|--------|
| `GeminiHelper.kt` | Add `enum` param to category schema, enhance system instruction |
| `PreviewViewModel.kt` | Pass `category` and `fee` to `AddTransactionParam` |
| `AddTransactionViewModel.kt` | Restore `category` and `fee` from draft in `checkDraftTransaction()` |

## References

- SDK Schema class supports `enum: List<String>?` parameter (confirmed in `generativeai-google:0.9.0-1.1.0` sources)
- `Schema.enum()` factory method also available but not needed here (embedded in larger object schema)
- `TransactionCategory.getCategoryByName()` at `TransactionCategory.kt:129-131` already handles fallback to OTHERS
- [Gemini structured output docs](https://ai.google.dev/gemini-api/docs/structured-output)
