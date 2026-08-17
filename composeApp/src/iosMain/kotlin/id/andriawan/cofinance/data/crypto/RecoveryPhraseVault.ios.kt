package id.andriawan.cofinance.data.crypto

import platform.Foundation.NSUserDefaults

/**
 * The sealed recovery phrase in `NSUserDefaults`.
 *
 * Not the Keychain, and not for lack of one: the blob is already sealed under the data key, so
 * Keychain protection would only bind it to this device, and the phrase is precisely the thing that
 * has to outlive this device. Ordinary defaults also mean a reinstall starts with no phrase to show
 * rather than with one no key on the device can open.
 */
actual fun createRecoveryPhraseVault(): RecoveryPhraseVault = SealedRecoveryPhraseVault(
    readSealed = { NSUserDefaults.standardUserDefaults.stringForKey(PHRASE_KEY) },
    writeSealed = { value -> NSUserDefaults.standardUserDefaults.setObject(value, forKey = PHRASE_KEY) },
    clearSealed = { NSUserDefaults.standardUserDefaults.removeObjectForKey(PHRASE_KEY) }
)

private const val PHRASE_KEY = "id.andriawan.cofinance.recovery_phrase.sealed"
