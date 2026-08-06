package id.andriawan.cofinance.data.remote

import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.FirebaseFirestore
import id.andriawan.cofinance.data.model.document.TransactionDocument
import id.andriawan.cofinance.data.model.response.TransactionResponse
import id.andriawan.cofinance.data.session.SessionPolicy

class FirestoreTransactionDataSource(
    private val firestore: FirebaseFirestore,
    private val sessionPolicy: SessionPolicy
) : TransactionRemoteDataSource {
    override suspend fun getTransactions(): List<TransactionResponse> {
        val userId = sessionPolicy.requireUserId()
        return transactions(userId)
            .orderBy(TransactionDocument::date.name, Direction.DESCENDING)
            .get()
            .documents
            .map { snapshot -> snapshot.data<TransactionDocument>().toResponse(snapshot.id) }
    }

    override suspend fun upsertTransactions(transactions: List<TransactionResponse>) {
        val userId = sessionPolicy.requireUserId()
        transactions.forEach { transaction ->
            val id = transaction.id ?: return@forEach
            transactions(userId).document(id).set(transaction.toDocument())
        }
    }

    private fun transactions(userId: String) =
        firestore.collection(USERS_COLLECTION).document(userId).collection(TRANSACTIONS_COLLECTION)

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val TRANSACTIONS_COLLECTION = "transactions"
    }
}

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
