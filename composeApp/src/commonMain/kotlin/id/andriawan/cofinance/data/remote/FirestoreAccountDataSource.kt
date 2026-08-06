package id.andriawan.cofinance.data.remote

import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.FirebaseFirestore
import id.andriawan.cofinance.data.model.document.AccountDocument
import id.andriawan.cofinance.data.model.response.AccountResponse
import id.andriawan.cofinance.data.session.SessionPolicy

class FirestoreAccountDataSource(
    private val firestore: FirebaseFirestore,
    private val sessionPolicy: SessionPolicy
) : AccountRemoteDataSource {
    override suspend fun getAccounts(): List<AccountResponse> {
        val userId = sessionPolicy.requireUserId()
        return accounts(userId)
            .orderBy(AccountDocument::createdAt.name, Direction.DESCENDING)
            .get()
            .documents
            .map { snapshot -> snapshot.data<AccountDocument>().toResponse(snapshot.id) }
    }

    override suspend fun upsertAccounts(accounts: List<AccountResponse>) {
        val userId = sessionPolicy.requireUserId()
        accounts.forEach { account ->
            val id = account.id ?: return@forEach
            accounts(userId).document(id).set(account.toDocument())
        }
    }

    private fun accounts(userId: String) =
        firestore.collection(USERS_COLLECTION).document(userId).collection(ACCOUNTS_COLLECTION)

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val ACCOUNTS_COLLECTION = "accounts"
    }
}

private fun AccountDocument.toResponse(id: String) =
    AccountResponse(id, name, group, balance, accountType, createdAt)

private fun AccountResponse.toDocument() =
    AccountDocument(name.orEmpty(), group.orEmpty(), balance ?: 0, accountType.orEmpty(), createdAt.orEmpty())
