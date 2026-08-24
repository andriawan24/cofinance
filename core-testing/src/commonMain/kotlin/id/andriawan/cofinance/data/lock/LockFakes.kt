package id.andriawan.cofinance.data.lock

import id.andriawan.cofinance.data.crypto.KeyMaterialDocument

/**
 * The test doubles the lock is driven against.
 *
 * Everything platform-specific about the lock is behind one of three narrow ports — where the
 * counter is stored, where the local key material is stored, and what the biometric hardware does —
 * so the whole state machine above them is exercisable here. The properties these fakes cannot
 * stand in for, and that the platform tests carry instead, are the ones about *where* bytes live:
 * that the counter is sealed by a Keystore key, and that the biometric key is destroyed by an
 * enrollment change.
 */

/** An in-memory [FailedAttemptStore] that also reports how it was written to. */
class FakeFailedAttemptStore(
    var stored: StoredFailedAttempts = StoredFailedAttempts.None
) : FailedAttemptStore {

    var writes: Int = 0
        private set

    var clears: Int = 0
        private set

    override suspend fun read(): StoredFailedAttempts = stored

    override suspend fun write(record: FailedAttemptRecord) {
        writes++
        stored = StoredFailedAttempts.Recorded(record)
    }

    override suspend fun clear() {
        clears++
        stored = StoredFailedAttempts.None
    }
}

/**
 * An in-memory [LocalKeyMaterialStore] that keeps every version it was handed.
 *
 * [writtenDocuments] is what the "nothing unwrapped is persisted" assertion reads: the serialized
 * form of everything this store was ever asked to hold.
 */
class FakeLocalKeyMaterialStore(
    private var document: KeyMaterialDocument? = null
) : LocalKeyMaterialStore {

    val writtenDocuments: MutableList<KeyMaterialDocument> = mutableListOf()

    var erases: Int = 0
        private set

    override suspend fun read(): KeyMaterialDocument? = document

    override suspend fun write(document: KeyMaterialDocument) {
        this.document = document
        writtenDocuments += document
    }

    override suspend fun erase() {
        erases++
        document = null
    }
}

/**
 * A [BiometricKeyBox] whose every answer is scripted.
 *
 * It holds the sealed bytes in a field, which is exactly as much fidelity as the common layer
 * needs: whether the platform key was gated on a fingerprint is not observable from here, and
 * pretending otherwise in a unit test would be the kind of assertion that passes while the product
 * is broken.
 */
class FakeBiometricKeyBox(
    var capability: BiometricCapability = BiometricCapability.Available,
    var sealResult: BiometricSealResult? = null,
    var openResult: BiometricOpenResult? = null
) : BiometricKeyBox {

    var sealed: ByteArray? = null
        private set

    var clears: Int = 0
        private set

    override suspend fun capability(): BiometricCapability = capability

    override suspend fun hasSealedSecret(): Boolean = sealed != null

    override suspend fun seal(
        plaintext: ByteArray,
        prompt: BiometricPromptText
    ): BiometricSealResult {
        val scripted = sealResult
        if (scripted != null && scripted != BiometricSealResult.Sealed) return scripted
        sealed = plaintext.copyOf()
        return BiometricSealResult.Sealed
    }

    override suspend fun open(prompt: BiometricPromptText): BiometricOpenResult {
        openResult?.let { scripted ->
            // The invalidation case discards the sealed copy on the real implementations, so the
            // fake does too; a test that re-checks isEnabled afterwards must see the same thing.
            if (scripted == BiometricOpenResult.Invalidated) sealed = null
            return scripted
        }
        return sealed?.let { BiometricOpenResult.Opened(it.copyOf()) } ?: BiometricOpenResult.Absent
    }

    override suspend fun clear() {
        clears++
        sealed = null
    }
}

/** A [LockClock] the test moves by hand. */
class MutableLockClock(private var millis: Long = 0) : LockClock {

    override fun nowMillis(): Long = millis

    fun advanceBy(delta: Long) {
        millis += delta
    }

    fun set(value: Long) {
        millis = value
    }
}
