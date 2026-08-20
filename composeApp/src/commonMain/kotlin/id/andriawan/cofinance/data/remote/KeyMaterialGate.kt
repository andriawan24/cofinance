package id.andriawan.cofinance.data.remote

import id.andriawan.cofinance.data.crypto.KeyMaterialDocument
import id.andriawan.cofinance.data.crypto.KeyWrapType
import id.andriawan.cofinance.data.session.SessionPolicy

/**
 * Holds the rule that key material reaches the backend before the first encrypted record does.
 *
 * The ordering is not a convention for callers to remember. A record written before its key material
 * exists is undecryptable on any other device: the device wrap never leaves this device, so the
 * recovery-phrase wrap in the backend is the only route back to those records, and a device failure
 * between the two writes would lose them permanently. Every encrypted write therefore passes
 * [requireKeyMaterial] first, and refuses rather than proceeds.
 *
 * [publishKeyMaterial] is the only upload path, and it uploads
 * [KeyMaterialDocument.uploadableKeyMaterial] rather than filtering the wraps itself. That is what
 * keeps a wrap type added later — a second device, a rotated key — excluded from the backend by
 * default instead of uploaded because a filter here was not extended.
 */
class KeyMaterialGate(
    private val store: FinanceDocumentStore,
    private val sessionPolicy: SessionPolicy
) {

    /**
     * The user whose key material is known to exist, so the check costs one read per session rather
     * than one per record.
     *
     * It records who rather than whether on purpose. A signed-out and signed-in different account
     * would otherwise inherit the previous user's answer and write its first records before its own
     * key material existed, which is precisely the failure this class exists to prevent.
     */
    private var confirmedForUser: String? = null

    /**
     * Uploads the recovery-phrase wrap of [material] and opens the gate.
     *
     * @throws MissingKeyMaterialException when [material] carries no recovery-phrase wrap. Uploading
     * key material that holds only device-bound copies would satisfy the gate while leaving the
     * records unrecoverable, which is the exact failure the gate exists to prevent.
     */
    suspend fun publishKeyMaterial(material: KeyMaterialDocument) {
        val uploadable = material.uploadableKeyMaterial()
        if (uploadable.wrapsOf(KeyWrapType.RecoveryPhrase).isEmpty()) {
            throw MissingKeyMaterialException(
                "Key material carries no recovery-phrase wrap, so uploading it would strand records"
            )
        }
        store.writeKeyMaterial(uploadable)
        confirmedForUser = sessionPolicy.userIdOrNull()
    }

    /** Returns the stored key material, for the restore path that unwraps it with a phrase. */
    suspend fun storedKeyMaterial(): KeyMaterialDocument? = store.readKeyMaterial()

    /**
     * Passes when key material already exists for this user.
     *
     * @throws MissingKeyMaterialException otherwise, before anything is written.
     */
    suspend fun requireKeyMaterial() {
        val userId = sessionPolicy.requireUserId()
        if (userId == confirmedForUser) return

        val stored = store.readKeyMaterial()
        if (stored == null || stored.wrapsOf(KeyWrapType.RecoveryPhrase).isEmpty()) {
            throw MissingKeyMaterialException(
                "No recovery-phrase wrapped key material is stored, so no encrypted record may be written"
            )
        }
        confirmedForUser = userId
    }
}

/** Raised when an encrypted record would be written before its key material exists. */
class MissingKeyMaterialException(message: String) : IllegalStateException(message)
