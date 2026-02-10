package id.andriawan.cofinance.utils

import com.andriawan.cofinance.BuildKonfig
import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
import dev.shreyaspatil.ai.client.generativeai.type.FunctionType
import dev.shreyaspatil.ai.client.generativeai.type.Schema
import dev.shreyaspatil.ai.client.generativeai.type.content
import dev.shreyaspatil.ai.client.generativeai.type.generationConfig

object GeminiHelper {
    private const val MODEL_NAME = "gemini-3-flash-preview"
    private const val JSON_MIME_TYPE = "application/json"

    const val TOTAL_PRICE_FIELD = "total_price"
    const val TRANSACTION_DATE_FIELD = "transaction_date"
    const val BANK_NAME_FIELD = "bank_name"
    const val FEE_FIELD = "fee"
    const val TRANSACTION_TYPE_FIELD = "transaction_type"
    const val SENDER_ACCOUNT_FIELD = "sender"
    const val SENDER_ACCOUNT_NUMBER_FIELD = "account_number"
    const val RECEIVER_ACCOUNT_FIELD = "receiver"

    private const val SYSTEM_INSTRUCTION =
        """
            You are an expert financial planner who helped the user to track their money
            Extract information from the receipt image provided by the user. 
            For date and time, please make it ISO 8601 compatible with complete time, date, and timezone. 
            If there are any missing field, just return null, don't return any empty value just null
        """

    private val transactionSchema = Schema(
        name = "transaction",
        description = "Transaction object received from the image",
        type = FunctionType.OBJECT,
        nullable = false,
        required = listOf("total_price", "transaction_date", "bank_name"),
        properties = mapOf(
            "total_price" to Schema(
                name = "total_price",
                description = "Total price derived from the receipt. Don't include fee if there is a fee, just put the pure transaction nominal before any additional cost",
                type = FunctionType.LONG,
                nullable = true
            ),
            "fee" to Schema(
                name = "fee",
                description = "Fee from the receipt",
                type = FunctionType.LONG,
                nullable = true
            ),
            "transaction_date" to Schema(
                name = "transaction_date",
                description = "Transaction date derived. Use ISO 8601 compatible with complete time, date, and timezone",
                type = FunctionType.STRING,
                nullable = true
            ),
            "bank_name" to Schema(
                name = "bank_name",
                description = "Bank name from the receipt",
                type = FunctionType.STRING,
                nullable = true
            ),
            "transaction_type" to Schema(
                name = "transaction_type",
                description = "Transaction type, whether it is QRIS, Transfer, etc. This one is nullable so return null if there isn't any",
                type = FunctionType.STRING,
                nullable = true
            ),
            "category" to Schema(
                name = "category",
                description = "Transaction category, it could be based on the receiver name or any description. This one is non nullable so just return Others if there's no category",
                type = FunctionType.STRING,
                nullable = false
            ),
            "sender" to Schema(
                name = "sender",
                description = "Sender account object, containing name and it's account number. This one is nullable so return null if there isn't any",
                type = FunctionType.OBJECT,
                properties = mapOf(
                    "name" to Schema(
                        name = "name",
                        description = "Name of the sender",
                        type = FunctionType.STRING,
                        nullable = true
                    ),
                    "account_number" to Schema(
                        name = "account_number",
                        description = "Account number of the sender",
                        type = FunctionType.STRING,
                        nullable = true
                    )
                )
            ),
            "receiver" to Schema(
                name = "receiver",
                description = "Receiver account object, containing name and it's account number. This one is nullable so return null if there isn't any",
                type = FunctionType.OBJECT,
                properties = mapOf(
                    "name" to Schema(
                        name = "name",
                        description = "Name of the receiver",
                        type = FunctionType.STRING,
                        nullable = true
                    ),
                    "account_number" to Schema(
                        name = "account_number",
                        description = "Account number of the receiver",
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
