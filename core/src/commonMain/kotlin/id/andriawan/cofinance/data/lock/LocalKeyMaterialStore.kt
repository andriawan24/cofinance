package id.andriawan.cofinance.data.lock

import id.andriawan.cofinance.data.crypto.KeyMaterialDocument

/**
 * The device's own copy of the key material, as the lock needs to see it.
 *
 * The lock reads it to find the PIN wrap it must open, rewrites it when the PIN changes, and erases
 * it when consecutive failures reach the threshold. Nothing here reaches the network: the uploaded
 * copy is the recovery-phrase wrap alone — see [KeyMaterialDocument.uploadableKeyMaterial] — and it
 * is what makes destruction survivable, so the erase path must be structurally unable to touch it.
 * That is why this port has no notion of a remote at all.
 *
 * Setup and restore own the writing side of local key material and will supply the implementation;
 * the lock only needs these three operations and is written against them so it can be tested with a
 * fake.
 */
interface LocalKeyMaterialStore {
    suspend fun read(): KeyMaterialDocument?
    suspend fun write(document: KeyMaterialDocument)
    suspend fun erase()
}
