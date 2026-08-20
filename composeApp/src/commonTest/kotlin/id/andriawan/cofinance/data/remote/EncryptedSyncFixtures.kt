package id.andriawan.cofinance.data.remote

import id.andriawan.cofinance.data.crypto.DataKey
import id.andriawan.cofinance.data.crypto.KeyMaterialDocument
import id.andriawan.cofinance.data.crypto.KeyWrapType
import id.andriawan.cofinance.data.crypto.RecordCipher
import id.andriawan.cofinance.data.crypto.WrappedDataKey
import id.andriawan.cofinance.data.keyring.InMemoryEncryptionSession
import id.andriawan.cofinance.data.session.SessionPolicy

/**
 * Shared fixtures for the encrypted synchronization tests.
 *
 * The wraps built here are synthetic bytes rather than real wrapping output. What these tests assert
 * about key material is which copies reach the backend and when, not whether a wrap opens — that is
 * already covered by the wrapper tests, and using real wraps here would tie these tests to a
 * derivation they do not exercise.
 */
fun wrapOf(type: KeyWrapType, keyId: String, marker: String): WrappedDataKey = WrappedDataKey.of(
    type = type,
    keyId = keyId,
    wrappedKey = marker.encodeToByteArray(),
    parameters = mapOf(WrappedDataKey.NONCE_PARAMETER to ByteArray(12) { it.toByte() })
)

/** Key material holding one wrap of each named type, as it would exist on a set-up device. */
fun keyMaterialWith(keyId: String, vararg types: KeyWrapType): KeyMaterialDocument =
    KeyMaterialDocument(
        keyMaterialVersion = KeyMaterialDocument.CURRENT_VERSION,
        wrappedKeys = types.map { type -> wrapOf(type, keyId, "${type.id}-wrapped-bytes") }
    )

/** A session holding [dataKey], which is the only state in which anything may be synchronized. */
fun unlockedSession(dataKey: DataKey): InMemoryEncryptionSession =
    InMemoryEncryptionSession().apply { unlock(dataKey) }

/** A session that completed setup earlier and holds no key right now. */
fun lockedSession(): InMemoryEncryptionSession = InMemoryEncryptionSession().apply { markSetUp() }

/** A session policy whose signed-in user can change, so account switching is expressible. */
class MutableSessionPolicy(var userId: String? = "firebase-user") : SessionPolicy {
    override fun isSignedIn(): Boolean = userId != null
    override fun userIdOrNull(): String? = userId
}

/** Builds the pair of data sources over one store, as the coordinator sees them. */
class EncryptedRemoteFixture(
    val store: FakeFinanceDocumentStore = FakeFinanceDocumentStore(),
    val session: InMemoryEncryptionSession,
    val sessionPolicy: MutableSessionPolicy = MutableSessionPolicy(),
    cipher: RecordCipher = RecordCipher()
) {
    val keyMaterialGate: KeyMaterialGate = KeyMaterialGate(store, sessionPolicy)

    val accounts: EncryptedAccountDataSource =
        EncryptedAccountDataSource(store, keyMaterialGate, session, cipher)

    val transactions: EncryptedTransactionDataSource =
        EncryptedTransactionDataSource(store, keyMaterialGate, session, cipher)
}
