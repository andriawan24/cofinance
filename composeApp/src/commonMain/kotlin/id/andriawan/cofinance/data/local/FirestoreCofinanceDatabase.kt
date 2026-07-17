package id.andriawan.cofinance.data.local

import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import id.andriawan.cofinance.data.model.response.AccountResponse
import id.andriawan.cofinance.data.model.response.TransactionResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlin.time.Clock

class FirestoreCofinanceDatabase(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : CofinanceDatabase {

    override fun watchAccounts(userId: String): Flow<List<AccountResponse>> {
        requireUserId(userId)
        return accounts(userId)
            .orderBy(AccountDocument::createdAt.name, Direction.DESCENDING)
            .snapshots
            .map { snapshot -> snapshot.documents.map(::mapAccount) }
    }

    override suspend fun getAccounts(userId: String): List<AccountResponse> {
        requireUserId(userId)
        return accounts(userId)
            .orderBy(AccountDocument::createdAt.name, Direction.DESCENDING)
            .get()
            .documents
            .map(::mapAccount)
    }

    override suspend fun insertAccount(
        id: String,
        name: String,
        group: String,
        balance: Long,
        accountType: String,
        userId: String
    ) {
        requireUserId(userId)
        accounts(userId).document(id).set(
            AccountDocument(
                name = name,
                group = group,
                balance = balance,
                accountType = accountType,
                createdAt = Clock.System.now().toString()
            )
        )
    }

    override suspend fun updateAccountBalance(accountId: String, delta: Long) {
        val userId = requireUserId()
        val reference = accounts(userId).document(accountId)
        firestore.runTransaction {
            val account = get(reference).requireAccount()
            set(reference, account.copy(balance = account.balance + delta))
        }
    }

    override suspend fun updateAccountType(accountId: String, accountType: String) {
        mutateAccount(accountId) { it.copy(accountType = accountType) }
    }

    override suspend fun updateAccount(
        accountId: String,
        name: String,
        balance: Long,
        group: String,
        accountType: String
    ) {
        mutateAccount(accountId) {
            it.copy(name = name, balance = balance, group = group, accountType = accountType)
        }
    }

    override suspend fun deleteAccount(accountId: String) {
        accounts(requireUserId()).document(accountId).delete()
    }

    override fun watchTransactions(
        userId: String,
        startDate: String?,
        endDate: String?,
        isDraft: Boolean,
        transactionId: String?
    ): Flow<List<TransactionResponse>> {
        requireUserId(userId)
        return transactions(userId)
            .orderBy(TransactionDocument::date.name, Direction.DESCENDING)
            .snapshots
            .map { snapshot ->
                hydrateTransactions(
                    userId = userId,
                    documents = snapshot.documents,
                    startDate = startDate,
                    endDate = endDate,
                    isDraft = isDraft,
                    transactionId = transactionId
                )
            }
    }

    override suspend fun getTransactions(
        userId: String,
        startDate: String?,
        endDate: String?,
        isDraft: Boolean,
        transactionId: String?
    ): List<TransactionResponse> {
        requireUserId(userId)
        val documents = if (transactionId != null) {
            listOf(transactions(userId).document(transactionId).get()).filter(DocumentSnapshot::exists)
        } else {
            transactions(userId)
                .orderBy(TransactionDocument::date.name, Direction.DESCENDING)
                .get()
                .documents
        }
        return hydrateTransactions(userId, documents, startDate, endDate, isDraft, transactionId)
    }

    override suspend fun insertTransaction(
        id: String,
        amount: Long,
        category: String,
        date: String,
        fee: Long,
        notes: String,
        accountsId: String,
        receiverAccountsId: String?,
        type: String,
        userId: String
    ): TransactionResponse {
        requireUserId(userId)
        val now = Clock.System.now().toString()
        val document = TransactionDocument(
            amount = amount,
            category = category,
            date = date,
            fee = fee,
            notes = notes,
            senderAccountId = accountsId,
            receiverAccountId = receiverAccountsId,
            type = type,
            createdAt = now,
            updatedAt = now
        )
        val transactionReference = transactions(userId).document(id)

        firestore.runTransaction {
            val deltas = balanceDeltas(document)
            val accountUpdates = deltas.mapValues { (accountId, delta) ->
                val reference = accounts(userId).document(accountId)
                reference to get(reference).requireAccount().let { it.copy(balance = it.balance + delta) }
            }
            accountUpdates.values.forEach { (reference, account) -> set(reference, account) }
            set(transactionReference, document)
        }

        return hydrateTransactions(
            userId = userId,
            documents = listOf(transactionReference.get()),
            transactionId = id,
            includeHiddenTypes = true
        ).first()
    }

    override suspend fun updateTransaction(
        id: String,
        amount: Long,
        category: String,
        date: String,
        fee: Long,
        notes: String,
        accountsId: String,
        receiverAccountsId: String?,
        type: String,
        userId: String
    ): TransactionResponse {
        requireUserId(userId)
        val reference = transactions(userId).document(id)

        firestore.runTransaction {
            val old = get(reference).requireTransaction()
            val replacement = old.copy(
                amount = amount,
                category = category,
                date = date,
                fee = fee,
                notes = notes,
                senderAccountId = accountsId,
                receiverAccountId = receiverAccountsId,
                type = type,
                updatedAt = Clock.System.now().toString()
            )
            val deltas = mutableMapOf<String, Long>()
            balanceDeltas(old, multiplier = -1).forEach { (accountId, delta) ->
                deltas[accountId] = deltas.getOrElse(accountId) { 0L } + delta
            }
            balanceDeltas(replacement).forEach { (accountId, delta) ->
                deltas[accountId] = deltas.getOrElse(accountId) { 0L } + delta
            }

            val accountUpdates = deltas.filterValues { it != 0L }.mapValues { (accountId, delta) ->
                val accountReference = accounts(userId).document(accountId)
                accountReference to get(accountReference).requireAccount().let {
                    it.copy(balance = it.balance + delta)
                }
            }
            accountUpdates.values.forEach { (accountReference, account) -> set(accountReference, account) }
            set(reference, replacement)
        }

        return hydrateTransactions(
            userId = userId,
            documents = listOf(reference.get()),
            transactionId = id,
            includeHiddenTypes = true
        ).first()
    }

    private suspend fun mutateAccount(accountId: String, transform: (AccountDocument) -> AccountDocument) {
        val userId = requireUserId()
        val reference = accounts(userId).document(accountId)
        firestore.runTransaction {
            set(reference, transform(get(reference).requireAccount()))
        }
    }

    private suspend fun hydrateTransactions(
        userId: String,
        documents: List<DocumentSnapshot>,
        startDate: String? = null,
        endDate: String? = null,
        isDraft: Boolean = false,
        transactionId: String? = null,
        includeHiddenTypes: Boolean = false
    ): List<TransactionResponse> {
        val filtered = documents.map { it.id to it.data<TransactionDocument>() }.filter { (id, transaction) ->
            (transactionId == null || id == transactionId) &&
                (startDate == null || endDate == null || transaction.date >= startDate && transaction.date < endDate) &&
                if (includeHiddenTypes) true
                else if (isDraft) transaction.type == TYPE_DRAFT
                else transaction.type != TYPE_DRAFT && transaction.type != TYPE_CYCLE_RESET
        }
        val accountIds = filtered.flatMap { (_, transaction) ->
            listOfNotNull(transaction.senderAccountId.takeIf(String::isNotBlank), transaction.receiverAccountId)
        }.toSet()
        val accountMap = accountIds.associateWith { accountId ->
            accounts(userId).document(accountId).get().takeIf(DocumentSnapshot::exists)?.let(::mapAccount)
        }
        return filtered.map { (id, transaction) ->
            TransactionResponse(
                id = id,
                amount = transaction.amount,
                category = transaction.category,
                date = transaction.date,
                fee = transaction.fee,
                notes = transaction.notes,
                senderAccountId = transaction.senderAccountId,
                receiverAccountId = transaction.receiverAccountId,
                createdAt = transaction.createdAt,
                updatedAt = transaction.updatedAt,
                type = transaction.type,
                sender = accountMap[transaction.senderAccountId],
                receiver = transaction.receiverAccountId?.let(accountMap::get)
            )
        }
    }

    private fun balanceDeltas(
        transaction: TransactionDocument,
        multiplier: Long = 1L
    ): Map<String, Long> {
        val deltas = mutableMapOf<String, Long>()
        fun add(accountId: String?, delta: Long) {
            if (!accountId.isNullOrBlank()) {
                deltas[accountId] = deltas.getOrElse(accountId) { 0L } + delta * multiplier
            }
        }

        when (transaction.type) {
            TYPE_INCOME -> add(transaction.senderAccountId, transaction.amount)
            TYPE_EXPENSE -> add(transaction.senderAccountId, -(transaction.amount + transaction.fee))
            TYPE_TRANSFER -> {
                add(transaction.senderAccountId, -(transaction.amount + transaction.fee))
                add(transaction.receiverAccountId, transaction.amount)
            }
        }
        return deltas
    }

    private fun mapAccount(snapshot: DocumentSnapshot): AccountResponse {
        val account = snapshot.data<AccountDocument>()
        return AccountResponse(
            id = snapshot.id,
            name = account.name,
            group = account.group,
            balance = account.balance,
            accountType = account.accountType,
            createdAt = account.createdAt
        )
    }

    private fun DocumentSnapshot.requireAccount(): AccountDocument {
        check(exists) { "Account document does not exist" }
        return data()
    }

    private fun DocumentSnapshot.requireTransaction(): TransactionDocument {
        check(exists) { "Transaction document does not exist" }
        return data()
    }

    private fun requireUserId(expected: String? = null): String {
        val userId = auth.currentUser?.uid ?: error("No authenticated Firebase user")
        check(expected == null || expected == userId) { "Firebase user scope mismatch" }
        return userId
    }

    private fun accounts(userId: String) =
        firestore.collection(USERS_COLLECTION).document(userId).collection(ACCOUNTS_COLLECTION)

    private fun transactions(userId: String) =
        firestore.collection(USERS_COLLECTION).document(userId).collection(TRANSACTIONS_COLLECTION)

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val ACCOUNTS_COLLECTION = "accounts"
        private const val TRANSACTIONS_COLLECTION = "transactions"
        private const val TYPE_INCOME = "INCOME"
        private const val TYPE_EXPENSE = "EXPENSE"
        private const val TYPE_TRANSFER = "TRANSFER"
        private const val TYPE_DRAFT = "DRAFT"
        private const val TYPE_CYCLE_RESET = "CYCLE_RESET"
    }
}

@Serializable
private data class AccountDocument(
    val name: String = "",
    val group: String = "",
    val balance: Long = 0L,
    val accountType: String = "",
    val createdAt: String = ""
)

@Serializable
private data class TransactionDocument(
    val amount: Long = 0L,
    val category: String = "",
    val date: String = "",
    val fee: Long = 0L,
    val notes: String = "",
    val senderAccountId: String = "",
    val receiverAccountId: String? = null,
    val type: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)
