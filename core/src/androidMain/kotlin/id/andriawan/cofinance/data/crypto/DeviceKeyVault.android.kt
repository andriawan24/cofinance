package id.andriawan.cofinance.data.crypto

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.ProviderException
import java.security.PublicKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECFieldFp
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec

/**
 * The [DeviceKeyVault] backed by the Android Keystore.
 *
 * ## What the hardware guarantee actually is, per API level
 *
 * The module floor is API 24, and the Keystore's capabilities differ enough across that range that
 * a single sentence would be a lie. Concretely:
 *
 * - **API 31 and above.** `KeyProperties.PURPOSE_AGREE_KEY` and
 *   `KeyAgreement.getInstance("ECDH", "AndroidKeyStore")` exist, so the P-256 agreement key is
 *   generated *inside* the Keystore and the private half never enters the process. Exporting it is
 *   impossible: `KeyStore.getKey(...).encoded` is `null` and ECDH runs inside the secure hardware.
 *   This is the full "an attacker holding the filesystem still cannot open it" guarantee.
 *
 * - **API 28 to 30.** The Keystore cannot perform EC key agreement at all — the purpose and the
 *   `KeyAgreement` provider were both added in API 31. The agreement key is therefore generated in
 *   software and sealed at rest with AES-256-GCM under a *non-extractable, hardware-backed*
 *   Keystore key. The guarantee degrades from "never in software" to "at rest it is unreadable
 *   without the device's secure hardware, and it is in process memory only for the duration of an
 *   ECDH operation". A filesystem copy is still useless off-device; a live root exploit on the
 *   device is not defeated. StrongBox is requested for the sealing key on these releases.
 *
 * - **API 24 to 27.** As above, minus StrongBox, which requires API 28. The sealing key is
 *   hardware-backed on any device with a TEE-backed Keymaster, which is effectively all of them at
 *   this API level, but a device whose Keystore is software-only silently gives a software sealing
 *   key. [protection] reports which of the three it got rather than assuming.
 *
 * The device secret is uniform across every API level: it is the HMAC-SHA256 of a fixed label under
 * a non-extractable Keystore HMAC key, so nothing has to be stored for it and the same 32 bytes come
 * back on every call. See [deviceSecret] for its lifetime.
 *
 * StrongBox is always opportunistic: it is requested on API 28 and above and the generation is
 * retried without it when the device reports it unavailable.
 */
class AndroidDeviceKeyVault internal constructor(
    private val storageDirectory: () -> File
) : DeviceKeyVault {

    private val mutex = Mutex()

    private var agreementKey: AgreementKey? = null

    override suspend fun devicePublicKey(): ByteArray = guarded {
        encodeUncompressedPoint(loadOrCreateAgreementKey().publicKey)
    }

    override suspend fun sharedSecretWith(peerPublicKey: ByteArray): ByteArray = guarded {
        val peer = decodePeerPublicKey(peerPublicKey)
        val key = loadOrCreateAgreementKey()
        val agreement = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            KeyAgreement.getInstance(ECDH_ALGORITHM, KEYSTORE_PROVIDER)
        } else {
            KeyAgreement.getInstance(ECDH_ALGORITHM)
        }
        agreement.init(key.privateKey)
        agreement.doPhase(peer, true)
        agreement.generateSecret()
    }

    /**
     * Returns the device secret, deriving it from a Keystore-held HMAC key rather than storing it.
     *
     * Lifetime: the HMAC key is created on first call and lives in the Keystore under this
     * application's UID. It therefore survives app restarts, process death, and device reboots, and
     * is destroyed by [destroyKeyMaterial], by clearing the app's data, and by uninstalling the app.
     * It is not part of any device backup and cannot be moved to another device, because Keystore
     * key material never leaves the device. The derived 32 bytes are readable in process, which the
     * interface requires so the PIN composition can combine them.
     */
    override suspend fun deviceSecret(): ByteArray = guarded {
        val key = loadOrCreateSecretKey()
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(key)
        mac.doFinal(DEVICE_SECRET_LABEL.toByteArray(Charsets.UTF_8))
    }

    override suspend fun destroyKeyMaterial(): Unit = guarded {
        val keyStore = keyStore()
        for (alias in listOf(AGREEMENT_KEY_ALIAS, SEALING_KEY_ALIAS, SECRET_KEY_ALIAS)) {
            if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias)
        }
        sealedAgreementKeyFile().delete()
        agreementKey = null
    }

    /**
     * Reports where each piece of key material actually ended up, so a device test can assert the
     * hardware guarantee instead of trusting the request.
     *
     * Entries are `null` when the corresponding key does not exist on this API level or has not been
     * created yet.
     */
    suspend fun protection(): DeviceKeyProtection = guarded {
        val keyStore = keyStore()
        DeviceKeyProtection(
            agreement = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                securityLevelOfPrivateKey(keyStore, AGREEMENT_KEY_ALIAS)
            } else {
                null
            },
            sealing = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                null
            } else {
                securityLevelOfSecretKey(keyStore, SEALING_KEY_ALIAS)
            },
            secret = securityLevelOfSecretKey(keyStore, SECRET_KEY_ALIAS)
        )
    }

    private suspend fun <T> guarded(block: () -> T): T = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                block()
            } catch (cause: DeviceKeyVaultException) {
                throw cause
            } catch (cause: GeneralSecurityException) {
                throw DeviceKeyVaultException(KEY_STORAGE_FAILURE, cause)
            } catch (cause: ProviderException) {
                throw DeviceKeyVaultException(KEY_STORAGE_FAILURE, cause)
            } catch (cause: IOException) {
                throw DeviceKeyVaultException(KEY_STORAGE_FAILURE, cause)
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // Agreement key
    // ---------------------------------------------------------------------------------------
    private fun loadOrCreateAgreementKey(): AgreementKey {
        agreementKey?.let { return it }
        val created = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            keystoreAgreementKey() ?: generateKeystoreAgreementKey()
        } else {
            sealedAgreementKey() ?: generateSealedAgreementKey()
        }
        agreementKey = created
        return created
    }

    /** API 31+: the private key is a Keystore handle and never materializes in this process. */
    private fun keystoreAgreementKey(): AgreementKey? {
        val keyStore = keyStore()
        if (!keyStore.containsAlias(AGREEMENT_KEY_ALIAS)) return null
        val privateKey = keyStore.getKey(AGREEMENT_KEY_ALIAS, null) as? PrivateKey
            ?: throw DeviceKeyVaultException("Device agreement key entry is not a private key")
        val certificate = keyStore.getCertificate(AGREEMENT_KEY_ALIAS)
            ?: throw DeviceKeyVaultException("Device agreement key has no public certificate")
        return AgreementKey(privateKey, certificate.publicKey.asEcPublicKey())
    }

    private fun generateKeystoreAgreementKey(): AgreementKey {
        val generator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            KEYSTORE_PROVIDER
        )

        val pair = withStrongBoxFallback { strongBox ->
            val spec = KeyGenParameterSpec.Builder(
                AGREEMENT_KEY_ALIAS,
                KeyProperties.PURPOSE_AGREE_KEY
            )
                .setAlgorithmParameterSpec(ECGenParameterSpec(SEC_CURVE_NAME))
                .apply { if (strongBox) setIsStrongBoxBacked(true) }
                .build()
            generator.initialize(spec)
            generator.generateKeyPair()
        }

        return AgreementKey(pair.private, pair.public.asEcPublicKey())
    }

    /**
     * API 24-30: the Keystore cannot agree, so the key pair is generated in software and its
     * encodings are sealed under the hardware-backed AES key before touching the disk.
     */
    private fun sealedAgreementKey(): AgreementKey? {
        val file = sealedAgreementKeyFile()
        if (!file.isFile) return null
        val opened = try {
            unseal(file.readBytes())
        } catch (cause: GeneralSecurityException) {
            throw DeviceKeyVaultException("Sealed device agreement key cannot be opened", cause)
        }
        return try {
            decodeAgreementKey(opened)
        } finally {
            opened.fill(0)
        }
    }

    private fun generateSealedAgreementKey(): AgreementKey {
        val generator = KeyPairGenerator.getInstance(EC_ALGORITHM)
        generator.initialize(ECGenParameterSpec(SEC_CURVE_NAME))
        val pair = generator.generateKeyPair()
        val plaintext = encodeAgreementKey(pair.private.encoded, pair.public.encoded)
        try {
            writeAtomically(sealedAgreementKeyFile(), seal(plaintext))
        } finally {
            plaintext.fill(0)
        }
        return AgreementKey(pair.private, pair.public.asEcPublicKey())
    }

    private fun sealedAgreementKeyFile(): File =
        File(storageDirectory().apply { mkdirs() }, SEALED_AGREEMENT_KEY_FILE)

    // ---------------------------------------------------------------------------------------
    // Sealing key (API 24-30 only) and device secret key
    // ---------------------------------------------------------------------------------------

    private fun seal(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, loadOrCreateSealingKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        return ByteArray(1 + iv.size + ciphertext.size).also { out ->
            out[0] = iv.size.toByte()
            iv.copyInto(out, 1)
            ciphertext.copyInto(out, 1 + iv.size)
        }
    }

    private fun unseal(sealed: ByteArray): ByteArray {
        if (sealed.isEmpty()) throw DeviceKeyVaultException("Sealed device key material is empty")
        val ivLength = sealed[0].toInt() and 0xFF
        if (sealed.size < 1 + ivLength + GCM_TAG_BITS / 8) {
            throw DeviceKeyVaultException("Sealed device key material is truncated")
        }
        val iv = sealed.copyOfRange(1, 1 + ivLength)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            loadOrCreateSealingKey(),
            GCMParameterSpec(GCM_TAG_BITS, iv)
        )
        return cipher.doFinal(sealed, 1 + ivLength, sealed.size - 1 - ivLength)
    }

    private fun loadOrCreateSealingKey(): SecretKey =
        existingSecretKey(SEALING_KEY_ALIAS) ?: withStrongBoxFallback { strongBox ->
            val generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )

            generator.init(
                KeyGenParameterSpec.Builder(
                    SEALING_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setKeySize(AES_KEY_BITS)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .apply { if (strongBox) setIsStrongBoxBacked(true) }
                    .build()
            )

            generator.generateKey()
        }

    private fun loadOrCreateSecretKey(): SecretKey =
        existingSecretKey(SECRET_KEY_ALIAS) ?: withStrongBoxFallback { strongBox ->
            val generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_HMAC_SHA256,
                KEYSTORE_PROVIDER
            )
            generator.init(
                KeyGenParameterSpec.Builder(SECRET_KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
                    .setKeySize(DeviceKeyVault.DEVICE_SECRET_SIZE * 8)
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .apply { if (strongBox) setIsStrongBoxBacked(true) }
                    .build()
            )
            generator.generateKey()
        }

    private fun existingSecretKey(alias: String): SecretKey? {
        val keyStore = keyStore()
        if (!keyStore.containsAlias(alias)) return null
        return keyStore.getKey(alias, null) as? SecretKey
            ?: throw DeviceKeyVaultException("Keystore entry $alias is not a secret key")
    }

    // ---------------------------------------------------------------------------------------
    // Key protection reporting
    // ---------------------------------------------------------------------------------------

    private fun securityLevelOfPrivateKey(
        keyStore: KeyStore,
        alias: String
    ): DeviceKeySecurityLevel? {
        if (!keyStore.containsAlias(alias)) return null
        val key = keyStore.getKey(alias, null) as? PrivateKey ?: return null
        val info = KeyFactory.getInstance(key.algorithm, KEYSTORE_PROVIDER)
            .getKeySpec(key, KeyInfo::class.java)
        return info.toSecurityLevel()
    }

    private fun securityLevelOfSecretKey(
        keyStore: KeyStore,
        alias: String
    ): DeviceKeySecurityLevel? {
        if (!keyStore.containsAlias(alias)) return null
        val key = keyStore.getKey(alias, null) as? SecretKey ?: return null
        val info = SecretKeyFactory.getInstance(key.algorithm, KEYSTORE_PROVIDER)
            .getKeySpec(key, KeyInfo::class.java) as KeyInfo
        return info.toSecurityLevel()
    }

    @Suppress("DEPRECATION")
    private fun KeyInfo.toSecurityLevel(): DeviceKeySecurityLevel =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            when (securityLevel) {
                KeyProperties.SECURITY_LEVEL_STRONGBOX -> DeviceKeySecurityLevel.STRONG_BOX
                KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT ->
                    DeviceKeySecurityLevel.TRUSTED_ENVIRONMENT

                KeyProperties.SECURITY_LEVEL_SOFTWARE -> DeviceKeySecurityLevel.SOFTWARE
                // SECURITY_LEVEL_UNKNOWN_SECURE: secure hardware of an unreported kind.
                KeyProperties.SECURITY_LEVEL_UNKNOWN_SECURE ->
                    DeviceKeySecurityLevel.TRUSTED_ENVIRONMENT

                else -> DeviceKeySecurityLevel.UNKNOWN
            }
        } else {
            // Before API 31 the Keystore reports only "secure hardware or not"; StrongBox and a TEE
            // are indistinguishable here, so the weaker of the two is reported.
            if (isInsideSecureHardware) {
                DeviceKeySecurityLevel.TRUSTED_ENVIRONMENT
            } else {
                DeviceKeySecurityLevel.SOFTWARE
            }
        }

    // ---------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------

    private fun keyStore(): KeyStore =
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

    /**
     * Runs [generate] asking for StrongBox first on API 28 and above, then without it.
     *
     * Devices report the absence of StrongBox inconsistently — the documented
     * [StrongBoxUnavailableException] on most, a bare [ProviderException] on some — so both are
     * treated as "fall back", and only a failure of the non-StrongBox attempt propagates.
     */
    private fun <T> withStrongBoxFallback(generate: (strongBox: Boolean) -> T): T {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            strongBoxAttempt(generate)?.let { return it }
        }
        return generate(false)
    }

    private fun <T> strongBoxAttempt(generate: (strongBox: Boolean) -> T): T? = try {
        generate(true)
    } catch (_: StrongBoxUnavailableException) {
        null
    } catch (_: ProviderException) {
        null
    }

    private class AgreementKey(
        val privateKey: PrivateKey,
        val publicKey: ECPublicKey
    )

    private fun encodeAgreementKey(pkcs8: ByteArray, x509: ByteArray): ByteArray {
        val out = ByteArray(4 + pkcs8.size + x509.size)
        out[0] = (pkcs8.size ushr 24).toByte()
        out[1] = (pkcs8.size ushr 16).toByte()
        out[2] = (pkcs8.size ushr 8).toByte()
        out[3] = pkcs8.size.toByte()
        pkcs8.copyInto(out, 4)
        x509.copyInto(out, 4 + pkcs8.size)
        return out
    }

    private fun decodeAgreementKey(encoded: ByteArray): AgreementKey {
        if (encoded.size < 4) {
            throw DeviceKeyVaultException("Sealed device agreement key is malformed")
        }
        val privateLength = ((encoded[0].toInt() and 0xFF) shl 24) or
                ((encoded[1].toInt() and 0xFF) shl 16) or
                ((encoded[2].toInt() and 0xFF) shl 8) or
                (encoded[3].toInt() and 0xFF)
        if (privateLength <= 0 || 4 + privateLength >= encoded.size) {
            throw DeviceKeyVaultException("Sealed device agreement key is malformed")
        }
        val factory = KeyFactory.getInstance(EC_ALGORITHM)
        val privateKey = factory.generatePrivate(
            PKCS8EncodedKeySpec(encoded.copyOfRange(4, 4 + privateLength))
        )
        val publicKey = factory.generatePublic(
            X509EncodedKeySpec(encoded.copyOfRange(4 + privateLength, encoded.size))
        )
        return AgreementKey(privateKey, publicKey.asEcPublicKey())
    }

    private fun writeAtomically(target: File, content: ByteArray) {
        val temporary = File(target.parentFile, "${target.name}.tmp")
        temporary.writeBytes(content)
        if (!temporary.renameTo(target)) {
            temporary.delete()
            throw DeviceKeyVaultException("Sealed device agreement key could not be stored")
        }
    }

    private fun PublicKey.asEcPublicKey(): ECPublicKey =
        this as? ECPublicKey
            ?: throw DeviceKeyVaultException("Device key is not an EC public key")

    private fun encodeUncompressedPoint(key: ECPublicKey): ByteArray {
        val x = key.w.affineX.toFixedLength(COORDINATE_SIZE)
        val y = key.w.affineY.toFixedLength(COORDINATE_SIZE)
        return ByteArray(DeviceKeyVault.PUBLIC_KEY_SIZE).also { out ->
            out[0] = UNCOMPRESSED_POINT_TAG
            x.copyInto(out, 1)
            y.copyInto(out, 1 + COORDINATE_SIZE)
        }
    }

    private fun decodePeerPublicKey(encoded: ByteArray): PublicKey {
        if (encoded.size != DeviceKeyVault.PUBLIC_KEY_SIZE ||
            encoded[0] != UNCOMPRESSED_POINT_TAG
        ) {
            throw DeviceKeyVaultException("Peer public key is not an uncompressed P-256 point")
        }
        val parameters = p256Parameters()
        val x = BigInteger(1, encoded.copyOfRange(1, 1 + COORDINATE_SIZE))
        val y = BigInteger(
            1,
            encoded.copyOfRange(1 + COORDINATE_SIZE, DeviceKeyVault.PUBLIC_KEY_SIZE)
        )
        val point = ECPoint(x, y)
        requireOnCurve(point, parameters)
        return try {
            KeyFactory.getInstance(EC_ALGORITHM).generatePublic(
                ECPublicKeySpec(point, parameters)
            )
        } catch (cause: GeneralSecurityException) {
            throw DeviceKeyVaultException("Peer public key is not a P-256 point", cause)
        }
    }

    /**
     * Rejects points that are not on P-256.
     *
     * The JCA does not validate this on every provider, and feeding an off-curve point into ECDH is
     * the classic invalid-curve attack that leaks the private scalar. P-256 has cofactor 1, so
     * membership in the curve group is the whole check.
     */
    private fun requireOnCurve(point: ECPoint, parameters: ECParameterSpec) {
        val field = parameters.curve.field as? ECFieldFp
            ?: throw DeviceKeyVaultException("P-256 parameters are not over a prime field")
        val prime = field.p
        val x = point.affineX
        val y = point.affineY
        val inRange = x.signum() >= 0 && x < prime && y.signum() >= 0 && y < prime
        val onCurve = inRange && y.modPow(BigInteger.valueOf(2), prime) ==
                (x.modPow(
                    BigInteger.valueOf(3),
                    prime
                ) + parameters.curve.a * x + parameters.curve.b)
                    .mod(prime)
        if (!onCurve) {
            throw DeviceKeyVaultException("Peer public key is not a point on P-256")
        }
    }

    private fun p256Parameters(): ECParameterSpec =
        AlgorithmParameters.getInstance(EC_ALGORITHM).run {
            init(ECGenParameterSpec(SEC_CURVE_NAME))
            getParameterSpec(ECParameterSpec::class.java)
        }

    private fun BigInteger.toFixedLength(length: Int): ByteArray {
        val magnitude = toByteArray()
        val offset = (magnitude.size - length).coerceAtLeast(0)
        val copied = magnitude.size - offset
        if (copied > length) throw DeviceKeyVaultException("Device key coordinate is oversized")
        return ByteArray(length).also { magnitude.copyInto(it, length - copied, offset) }
    }

    companion object {
        const val AGREEMENT_KEY_ALIAS: String = "id.andriawan.cofinance.device.agreement"
        const val SEALING_KEY_ALIAS: String = "id.andriawan.cofinance.device.sealing"
        const val SECRET_KEY_ALIAS: String = "id.andriawan.cofinance.device.secret"
        const val SEALED_AGREEMENT_KEY_FILE: String = "device-agreement-key.bin"

        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val EC_ALGORITHM = "EC"
        private const val ECDH_ALGORITHM = "ECDH"
        private const val HMAC_ALGORITHM = "HmacSHA256"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val SEC_CURVE_NAME = "secp256r1"
        private const val DEVICE_SECRET_LABEL = "id.andriawan.cofinance.device-secret.v1"
        private const val AES_KEY_BITS = 256
        private const val GCM_TAG_BITS = 128
        private const val COORDINATE_SIZE = 32
        private const val UNCOMPRESSED_POINT_TAG = 0x04.toByte()
        private const val KEY_STORAGE_FAILURE = "Platform key storage cannot serve the device key"
    }
}

/** Where a piece of device key material ended up, as the platform reports it. */
enum class DeviceKeySecurityLevel {
    SOFTWARE,
    TRUSTED_ENVIRONMENT,
    STRONG_BOX,
    UNKNOWN
}

/**
 * Where each piece of device key material is held.
 *
 * [agreement] is populated only on API 31 and above, where the P-256 key is Keystore-resident;
 * [sealing] only below it, where a hardware-backed AES key protects the software agreement key at
 * rest. Both being `null` for a given key means it has not been created yet.
 */
data class DeviceKeyProtection(
    val agreement: DeviceKeySecurityLevel?,
    val sealing: DeviceKeySecurityLevel?,
    val secret: DeviceKeySecurityLevel?
)

actual fun createDeviceKeyVault(): DeviceKeyVault =
    AndroidDeviceKeyVault { DeviceKeyVaultStorage.directory() }
