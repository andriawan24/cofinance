package id.andriawan24.cofinance.utils

import com.andreasgift.kmpweatherapp.BuildKonfig
import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
import dev.shreyaspatil.ai.client.generativeai.type.FunctionType
import dev.shreyaspatil.ai.client.generativeai.type.Schema
import dev.shreyaspatil.ai.client.generativeai.type.content
import dev.shreyaspatil.ai.client.generativeai.type.generationConfig

object GeminiHelper {
    private const val MODEL_NAME = "gemini-2.0-flash-lite-001"

    private const val JSON_MIME_TYPE = "application/json"

    private const val TRANSACTION_SCHEMA = "transaction"
    private const val TRANSACTION_SCHEMA_DESCRIPTION = "Transaction object received from the image"

    const val TOTAL_PRICE_FIELD = "total_price"
    private const val TOTAL_PRICE_FIELD_DESCRIPTION =
        "Total price derived from the receipt. Don't include fee if there is a fee, just put the pure transaction nominal before any additional cost"

    const val TRANSACTION_DATE_FIELD = "transaction_date"
    private const val TRANSACTION_DATE_FIELD_DESCRIPTION =
        "Transaction date derived. Use ISO 8601 compatible with complete time, date, and timezone"

    const val BANK_NAME_FIELD = "bank_name"
    private const val BANK_NAME_FIELD_DESCRIPTION = "Bank name from the receipt"

    const val FEE_FIELD = "fee"
    private const val FEE_FIELD_DESCRIPTION = "Fee from the receipt"

    const val TRANSACTION_TYPE_FIELD = "transaction_type"
    private const val TRANSACTION_TYPE_FIELD_DESCRIPTION =
        "Transaction type, whether it is QRIS, Transfer, etc. This one is nullable so return null if there isn't any"

    private const val CATEGORY_FIELD = "category"
    private const val CATEGORY_FIELD_DESCRIPTION =
        "Transaction category, it could be based on the receiver name or any description. This one is non nullable so just return Others if there's no category"

    const val SENDER_ACCOUNT_FIELD = "sender"
    private const val SENDER_ACCOUNT_FIELD_DESCRIPTION =
        "Sender account object, containing name and it's account number. This one is nullable so return null if there isn't any"

    private const val SENDER_NAME_FIELD = "name"
    private const val SENDER_NAME_FIELD_DESCRIPTION = "Name of the sender"

    const val SENDER_ACCOUNT_NUMBER_FIELD = "account_number"
    private const val SENDER_ACCOUNT_NUMBER_FIELD_DESCRIPTION = "Account number of the sender"

    const val RECEIVER_ACCOUNT_FIELD = "receiver"
    private const val RECEIVER_ACCOUNT_FIELD_DESCRIPTION =
        "Receiver account object, containing name and it's account number. This one is nullable so return null if there isn't any"

    private const val RECEIVER_NAME_FIELD = "name"
    private const val RECEIVER_NAME_FIELD_DESCRIPTION = "Name of the receiver"

    private const val RECEIVER_ACCOUNT_NUMBER_FIELD = "account_number"
    private const val RECEIVER_ACCOUNT_NUMBER_FIELD_DESCRIPTION = "Account number of the receiver"

    private const val SYSTEM_INSTRUCTION =
        "Extract information from the receipt image provided by the user. For date and time, please make it ISO 8601 compatible with complete time, date, and timezone. If there are any missing field, just return null, don't return any empty value just null"

    private val transactionSchema = Schema(
        name = TRANSACTION_SCHEMA,
        description = TRANSACTION_SCHEMA_DESCRIPTION,
        type = FunctionType.OBJECT,
        nullable = false,
        required = listOf(TOTAL_PRICE_FIELD, TRANSACTION_DATE_FIELD, BANK_NAME_FIELD),
        properties = mapOf(
            TOTAL_PRICE_FIELD to Schema(
                name = TOTAL_PRICE_FIELD,
                description = TOTAL_PRICE_FIELD_DESCRIPTION,
                type = FunctionType.LONG,
                nullable = true
            ),
            FEE_FIELD to Schema(
                name = FEE_FIELD,
                description = FEE_FIELD_DESCRIPTION,
                type = FunctionType.LONG,
                nullable = true
            ),
            TRANSACTION_DATE_FIELD to Schema(
                name = TRANSACTION_DATE_FIELD,
                description = TRANSACTION_DATE_FIELD_DESCRIPTION,
                type = FunctionType.STRING,
                nullable = true
            ),
            BANK_NAME_FIELD to Schema(
                name = BANK_NAME_FIELD,
                description = BANK_NAME_FIELD_DESCRIPTION,
                type = FunctionType.STRING,
                nullable = true
            ),
            TRANSACTION_TYPE_FIELD to Schema(
                name = TRANSACTION_TYPE_FIELD,
                description = TRANSACTION_TYPE_FIELD_DESCRIPTION,
                type = FunctionType.STRING,
                nullable = true
            ),
            CATEGORY_FIELD to Schema(
                name = CATEGORY_FIELD,
                description = CATEGORY_FIELD_DESCRIPTION,
                type = FunctionType.STRING,
                nullable = false
            ),
            SENDER_ACCOUNT_FIELD to Schema(
                name = SENDER_ACCOUNT_FIELD,
                description = SENDER_ACCOUNT_FIELD_DESCRIPTION,
                type = FunctionType.OBJECT,
                properties = mapOf(
                    SENDER_NAME_FIELD to Schema(
                        name = SENDER_NAME_FIELD,
                        description = SENDER_NAME_FIELD_DESCRIPTION,
                        type = FunctionType.STRING,
                        nullable = true
                    ),
                    SENDER_ACCOUNT_NUMBER_FIELD to Schema(
                        name = SENDER_ACCOUNT_NUMBER_FIELD,
                        description = SENDER_ACCOUNT_NUMBER_FIELD_DESCRIPTION,
                        type = FunctionType.STRING,
                        nullable = true
                    )
                )
            ),
            RECEIVER_ACCOUNT_FIELD to Schema(
                name = RECEIVER_ACCOUNT_FIELD,
                description = RECEIVER_ACCOUNT_FIELD_DESCRIPTION,
                type = FunctionType.OBJECT,
                properties = mapOf(
                    RECEIVER_NAME_FIELD to Schema(
                        name = RECEIVER_NAME_FIELD,
                        description = RECEIVER_NAME_FIELD_DESCRIPTION,
                        type = FunctionType.STRING,
                        nullable = true
                    ),
                    RECEIVER_ACCOUNT_NUMBER_FIELD to Schema(
                        name = RECEIVER_ACCOUNT_NUMBER_FIELD,
                        description = RECEIVER_ACCOUNT_NUMBER_FIELD_DESCRIPTION,
                        type = FunctionType.STRING,
                        nullable = true
                    )
                )
            )
        )
    )

    fun createModel(): GenerativeModel = GenerativeModel(
        modelName = MODEL_NAME,
        apiKey = BuildKonfig.GEMINI_API_KEY,
        systemInstruction = content { text(text = SYSTEM_INSTRUCTION) },
        generationConfig = generationConfig {
            temperature = 0.2f
            topP = 0.95f
            topK = 64
            responseMimeType = JSON_MIME_TYPE
            responseSchema = transactionSchema
        }
    )
}