package id.andriawan.cofinance.data.lock

import id.andriawan.cofinance.data.crypto.DeviceKeyVault
import id.andriawan.cofinance.data.keyring.InMemoryEncryptionSession

/**
 * Destroys everything on this device that could open the data key, and nothing else.
 *
 * This is what the tenth consecutive failed PIN attempt reaches. Three things go, in this order:
 *
 * 1. the locally stored wrapped copies — the device wrap and the PIN wrap — through
 *    [LocalKeyMaterialStore.erase],
 * 2. the platform key material both of those depend on, through [DeviceKeyVault.destroyKeyMaterial],
 *    so that a copy of the local document taken before destruction is inert afterwards,
 * 3. the in-memory session, which is returned to `SetupIncomplete` so the next launch reaches
 *    restore rather than an unlock screen it can never satisfy.
 *
 * What deliberately does not happen is any remote call. The recovery-phrase wrap in the backend is
 * the surviving copy, and the user restores from it with their 12 words; that is the whole reason
 * the design holds two independent wraps, so destruction is written to be structurally incapable of
 * reaching the uploaded one — this class has no remote dependency to reach it with.
 *
 * The order matters in one direction only: the platform keys go after the local document, because
 * erasing the document first means an interruption between the two leaves key material that opens
 * nothing rather than a document with no key behind it. Both orders end at the same place; this one
 * has the harmless failure.
 */
class LocalKeyMaterialDestroyer(
    private val keyMaterial: LocalKeyMaterialStore,
    private val vault: DeviceKeyVault,
    private val session: InMemoryEncryptionSession
) {

    /** Erases local key material. Records stay recoverable through the recovery phrase. */
    suspend fun destroy() {
        keyMaterial.erase()
        vault.destroyKeyMaterial()
        session.forgetSetup()
    }
}
