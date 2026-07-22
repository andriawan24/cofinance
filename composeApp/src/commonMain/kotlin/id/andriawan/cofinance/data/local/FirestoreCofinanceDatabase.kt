package id.andriawan.cofinance.data.local

import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.FirebaseFirestore
import id.andriawan.cofinance.data.model.response.AccountResponse
import id.andriawan.cofinance.data.model.response.TransactionResponse
import id.andriawan.cofinance.data.session.SessionPolicy
import kotlinx.serialization.Serializable

interface RemoteFinanceDataSource {
    suspend fun getAccounts(): List<AccountResponse>
    suspend fun getTransactions(): List<TransactionResponse>
    suspend fun upsertAccounts(accounts: List<AccountResponse>)
    suspend fun upsertTransactions(transactions: List<TransactionResponse>)
}

class FirestoreCofinanceDatabase(
    private val firestore: FirebaseFirestore,
    private val sessionPolicy: SessionPolicy
) : RemoteFinanceDataSource {
    override suspend fun getAccounts(): List<AccountResponse> {
        val userId = sessionPolicy.requireUserId()
        return accounts(userId)
            .orderBy(AccountDocument::createdAt.name, Direction.DESCENDING)
            .get()
            .documents
            .map { snapshot -> snapshot.data<AccountDocument>().toResponse(snapshot.id) }
    }

    override suspend fun getTransactions(): List<TransactionResponse> {
        val userId = sessionPolicy.requireUserId()
        return transactions(userId)
            .orderBy(TransactionDocument::date.name, Direction.DESCENDING)
            .get()
            .documents
            .map { snapshot -> snapshot.data<TransactionDocument>().toResponse(snapshot.id) }
    }

    override suspend fun upsertAccounts(accounts: List<AccountResponse>) {
        val userId = sessionPolicy.requireUserId()
        accounts.forEach { account ->
            val id = account.id ?: return@forEach
            accounts(userId).document(id).set(account.toDocument())
        }
    }

    override suspend fun upsertTransactions(transactions: List<TransactionResponse>) {
        val userId = sessionPolicy.requireUserId()
        transactions.forEach { transaction ->
            val id = transaction.id ?: return@forEach
            transactions(userId).document(id).set(transaction.toDocument())
        }
    }

    private fun accounts(userId: String) =
        firestore.collection(USERS_COLLECTION).document(userId).collection(ACCOUNTS_COLLECTION)

    private fun transactions(userId: String) =
        firestore.collection(USERS_COLLECTION).document(userId).collection(TRANSACTIONS_COLLECTION)

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val ACCOUNTS_COLLECTION = "accounts"
        private const val TRANSACTIONS_COLLECTION = "transactions"
    }
}

@Serializable
private data class AccountDocument(
    val name: String = "",
    val group: String = "",
    val balance: Long = 0,
    val accountType: String = "",
    val createdAt: String = ""
)

@Serializable
private data class TransactionDocument(
    val amount: Long = 0,
    val category: String = "",
    val date: String = "",
    val fee: Long = 0,
    val notes: String = "",
    val senderAccountId: String = "",
    val receiverAccountId: String? = null,
    val type: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

private fun AccountDocument.toResponse(id: String) =
    AccountResponse(id, name, group, balance, accountType, createdAt)

private fun AccountResponse.toDocument() =
    AccountDocument(name.orEmpty(), group.orEmpty(), balance ?: 0, accountType.orEmpty(), createdAt.orEmpty())

private fun TransactionDocument.toResponse(id: String) = TransactionResponse(
    id = id,
    amount = amount,
    category = category,
    date = date,
    fee = fee,
    notes = notes,
    senderAccountId = senderAccountId,
    receiverAccountId = receiverAccountId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    type = type,
    sender = null,
    receiver = null
)

private fun TransactionResponse.toDocument() = TransactionDocument(
    amount ?: 0,
    category.orEmpty(),
    date.orEmpty(),
    fee ?: 0,
    notes.orEmpty(),
    senderAccountId.orEmpty(),
    receiverAccountId,
    type.orEmpty(),
    createdAt.orEmpty(),
    updatedAt.orEmpty()
)
