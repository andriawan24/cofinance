package id.andriawan.cofinance.data.migration

import id.andriawan.cofinance.data.crypto.DataKey
import id.andriawan.cofinance.data.crypto.EncryptedEnvelope
import id.andriawan.cofinance.data.crypto.RecordCipher
import id.andriawan.cofinance.data.model.document.AccountDocument
import id.andriawan.cofinance.data.model.document.TransactionDocument
import id.andriawan.cofinance.data.model.response.AccountResponse
import id.andriawan.cofinance.data.model.response.TransactionResponse
import id.andriawan.cofinance.data.remote.FinanceCollection
import kotlinx.serialization.KSerializer

/**
 * How one collection's legacy plaintext documents become the record shape the cipher seals.
 *
 * Two serializers appear here for one collection because the stored plaintext shape and the sealed
 * record shape are genuinely different: `AccountDocument` is what an earlier build wrote to
 * Firestore, and `AccountResponse` is what `EncryptedAccountDataSource` seals today. Migration has
 * to read the first and produce the second, so that a migrated record and a record the mirror
 * uploads afterwards are byte-for-byte the same kind of thing rather than two dialects the reader
 * has to tell apart.
 *
 * [scan] returns type-erased [ScannedRecord]s on purpose. The migration flow walks both collections
 * in one list and has no use for either type parameter; keeping them out of its signature is what
 * lets it hold a `List<LegacyFinanceCollection<*, *>>` without star-projection contortions.
 */
class LegacyFinanceCollection<L, R>(
    val collection: FinanceCollection,
    private val legacy: KSerializer<L>,
    private val record: KSerializer<R>,
    private val toRecord: (id: String, legacy: L) -> R
) {

    /** The field names an earlier build wrote for this collection, taken from the legacy shape. */
    val plaintextFields: Set<String> = legacy.descriptor.fieldNames()

    /** Reads every document in this collection and classifies it against the envelope marker. */
    suspend fun scan(
        store: PlaintextFinanceDocumentStore,
        cipher: RecordCipher
    ): List<ScannedRecord> = store.readDocuments(collection, legacy).map { document ->
        ScannedRecord(
            collection = collection,
            id = document.id,
            plaintextFields = plaintextFields,
            isMigrated = document.isMigrated,
            needsConversion = document.needsConversion,
            seal = document.plaintext?.let { plaintext -> sealer(document.id, plaintext, cipher) }
        )
    }

    private fun sealer(
        id: String,
        plaintext: L,
        cipher: RecordCipher
    ): suspend (DataKey) -> EncryptedEnvelope =
        { dataKey -> cipher.seal(toRecord(id, plaintext), record, dataKey) }
}

/**
 * One scanned document with its type parameters discharged.
 *
 * [seal] is null exactly when the stored plaintext could not be read as a finance record, which is
 * the one per-record failure migration can encounter that retrying will never fix.
 */
class ScannedRecord(
    val collection: FinanceCollection,
    val id: String,
    val plaintextFields: Set<String>,
    val isMigrated: Boolean,
    val needsConversion: Boolean,
    internal val seal: (suspend (DataKey) -> EncryptedEnvelope)?
) {

    /** Encrypted already, with plaintext still beside it — an interrupted conversion to finish. */
    val needsPlaintextRemoval: Boolean get() = !isMigrated && !needsConversion

    override fun toString(): String = "${collection.path}/$id"
}

/**
 * The collections migration covers, which is every collection the mirror uploads.
 *
 * Both are listed in one place so that adding a synchronized collection later cannot silently leave
 * a plaintext one behind: the flow iterates this list rather than naming collections itself.
 */
object LegacyFinanceCollections {

    val ACCOUNTS: LegacyFinanceCollection<AccountDocument, AccountResponse> = LegacyFinanceCollection(
        collection = FinanceCollection.ACCOUNTS,
        legacy = AccountDocument.serializer(),
        record = AccountResponse.serializer()
    ) { id, document ->
        AccountResponse(
            id = id,
            name = document.name,
            group = document.group,
            balance = document.balance,
            accountType = document.accountType,
            createdAt = document.createdAt
        )
    }

    val TRANSACTIONS: LegacyFinanceCollection<TransactionDocument, TransactionResponse> =
        LegacyFinanceCollection(
            collection = FinanceCollection.TRANSACTIONS,
            legacy = TransactionDocument.serializer(),
            record = TransactionResponse.serializer()
        ) { id, document ->
            // Sender and receiver stay null, matching EncryptedTransactionDataSource: both are
            // hydrated locally from the account table, and sealing them would store a second
            // encrypted copy of account data under every transaction.
            TransactionResponse(
                id = id,
                amount = document.amount,
                category = document.category,
                date = document.date,
                fee = document.fee,
                notes = document.notes,
                senderAccountId = document.senderAccountId,
                receiverAccountId = document.receiverAccountId,
                type = document.type,
                createdAt = document.createdAt,
                updatedAt = document.updatedAt
            )
        }

    val ALL: List<LegacyFinanceCollection<*, *>> = listOf(ACCOUNTS, TRANSACTIONS)
}
