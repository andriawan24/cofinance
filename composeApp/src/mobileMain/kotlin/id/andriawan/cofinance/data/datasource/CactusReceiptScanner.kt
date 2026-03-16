package id.andriawan.cofinance.data.datasource

import com.cactus.CactusCompletionParams
import com.cactus.CactusCompletionResult
import com.cactus.CactusInitParams
import com.cactus.CactusLM
import com.cactus.CactusModelManager
import com.cactus.ChatMessage
import com.cactus.InferenceMode
import com.cactus.models.ToolParameter
import com.cactus.models.createTool
import com.diamondedge.logging.logging
import id.andriawan.cofinance.data.model.response.ReceiptScanResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class CactusReceiptScanner : ReceiptScannerService {

    private val lm = CactusLM()
    private val _modelStatus = MutableStateFlow<ModelStatus>(ModelStatus.NotDownloaded)
    private var isModelInitialized = false

    override fun getModelStatus(): StateFlow<ModelStatus> = _modelStatus.asStateFlow()

    override suspend fun isModelReady(): Boolean {
        val isModelDownloaded = CactusModelManager.isModelDownloaded(MODEL_SLUG)
        if (isModelDownloaded) {
            _modelStatus.value = ModelStatus.Ready
        }
        return isModelDownloaded
    }

    override suspend fun downloadModel(onProgress: (Float) -> Unit) {
        _modelStatus.value = ModelStatus.Downloading(0f)
        try {
            lm.downloadModel(MODEL_SLUG)
            _modelStatus.value = ModelStatus.Ready
            onProgress(1f)
        } catch (e: Exception) {
            _modelStatus.value = ModelStatus.Error(e.message ?: "Download failed")
            throw e
        }
    }

    override suspend fun scanReceipt(image: ByteArray): ReceiptScanResponse =
        withContext(Dispatchers.IO) {
            if (!isModelReady()) {
                downloadModel()
            }

            if (!isModelInitialized) {
                _modelStatus.value = ModelStatus.LoadingModel
                lm.initializeModel(CactusInitParams(model = MODEL_SLUG, contextSize = 4096))
                isModelInitialized = true
            }

            _modelStatus.value = ModelStatus.Inferring

            val compressedImage = compressImage(image, maxWidth = 1024, quality = 80)
            val tempImagePath = saveTempImage(compressedImage)
            try {
                val result = withTimeout(INFERENCE_TIMEOUT_MS) {
                    lm.generateCompletion(
                        messages = listOf(
                            ChatMessage(content = SYSTEM_INSTRUCTION, role = "system"),
                            ChatMessage(
                                content = "Extract information from the given receipt image",
                                role = "user",
                            )
                        ),
                        params = CactusCompletionParams(
                            tools = listOf(receiptExtractionTool),
                            forceTools = true,
                            mode = InferenceMode.LOCAL
                        )
                    )
                }

                lm.unload()

                logging(CactusReceiptScanner::class.simpleName).info {
                    "Result $result"
                }

                _modelStatus.value = ModelStatus.Ready
                parseToolCallToResponse(result)
            } catch (e: Exception) {
                _modelStatus.value = ModelStatus.Ready
                throw e
            } finally {
                deleteTempFile(tempImagePath)
            }
        }

    private fun parseToolCallToResponse(result: CactusCompletionResult?): ReceiptScanResponse {
        check(result?.success == true) { "Receipt scan inference failed" }

        val toolCall = result.toolCalls?.firstOrNull()
            ?: throw IllegalStateException("No structured data extracted from receipt")

        val args = toolCall.arguments

        return ReceiptScanResponse(
            totalPrice = args["total_price"]?.toLongOrNull(),
            transactionDate = args["transaction_date"],
            bankName = args["bank_name"],
            fee = args["fee"]?.toLongOrNull(),
            transactionType = args["transaction_type"],
            category = args["category"] ?: "Others",
            sender = buildBankAccount(args["sender_name"], args["sender_account_number"]),
            receiver = buildBankAccount(args["receiver_name"], args["receiver_account_number"])
        )
    }

    private fun buildBankAccount(
        name: String?,
        accountNumber: String?
    ): ReceiptScanResponse.BankAccount? {
        name ?: return null
        return ReceiptScanResponse.BankAccount(name = name, accountNumber = accountNumber)
    }

    companion object {
        private const val MODEL_SLUG = "lfm2-vl-1.6b"
        private const val INFERENCE_TIMEOUT_MS = 30_000L

        private const val SYSTEM_INSTRUCTION = """
            You are an expert financial planner who helped the user to track their money
            Extract information from the receipt image provided by the user.
            For date and time, please make it ISO 8601 compatible with complete time, date, and timezone.
            If there are any missing field, just return null, don't return any empty value just null

            Categories: FOOD (restaurants, groceries, cafes, food delivery), TRANSPORT (ride-hailing, fuel, parking, tolls, public transit),
            HOUSING (rent, mortgage, utilities), APPAREL (clothing, shoes, accessories), HEALTH (medical, pharmacy, fitness),
            EDUCATION (courses, books, tuition), SUBSCRIPTION (streaming, software subscriptions), INTERNET (internet service, phone bills),
            DEBT (loan payments, credit card), GIFT (gifts, donations), ADMINISTRATION (government fees, legal, banking fees),
            SALARY (salary, wages - income), INVEST (investments, dividends - income), OTHERS (anything else).
            Classify the transaction into one of these categories based on the merchant, receiver, or description.
        """

        private val receiptExtractionTool = createTool(
            name = "extract_receipt_data",
            description = "Extract structured transaction data from a receipt image",
            parameters = mapOf(
                "total_price" to ToolParameter(
                    type = "number",
                    description = "Total transaction amount in smallest currency unit (e.g., rupiah). Exclude fees.",
                    required = true
                ),
                "transaction_date" to ToolParameter(
                    type = "string",
                    description = "Transaction date in ISO 8601 format with timezone",
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
    }
}

expect fun compressImage(image: ByteArray, maxWidth: Int, quality: Int): ByteArray
expect fun saveTempImage(image: ByteArray): String
expect fun deleteTempFile(path: String)
