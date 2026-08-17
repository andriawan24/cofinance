package id.andriawan.cofinance.pages.lock

import id.andriawan.cofinance.data.keyring.EncryptionSessionState

/**
 * What counts as a PIN, in one place, because the unlock screen and the settings screen must agree.
 *
 * Six digits is the length Decision 4 and Decision 9 are written against: the threshold and the
 * escalating delay are what make a six-digit secret defensible, and a screen that quietly accepted
 * four would weaken the analysis without changing anything visible.
 */
object PinRules {

    /** The only length this app accepts. */
    const val LENGTH: Int = 6

    /** Keeps entry to digits and to [LENGTH], so a paste cannot produce an unsubmittable value. */
    fun sanitize(input: String): String = input.filter(Char::isDigit).take(LENGTH)

    /** Whether [pin] is a complete PIN. */
    fun isComplete(pin: String): Boolean = pin.length == LENGTH && pin.all(Char::isDigit)
}

/** Where launch sends the user, as far as the lock is concerned. */
enum class LaunchLockDecision {

    /** Straight into the app. Either there is no key material yet, or the key is already held. */
    Main,

    /** Key material exists and the key is not in memory, so the unlock screen comes first. */
    Unlock
}

/**
 * The lock's answer to "what does launch show?", which is a property of session state alone.
 *
 * [EncryptionSessionState.SetupIncomplete] is the local-only user and the not-yet-set-up user, and
 * both go straight to the main experience: the specification is explicit that a user who has never
 * signed in sees no unlock screen, and there is nothing for one to unlock. Only a session that holds
 * key material without holding the key is locked.
 *
 * This is a function rather than a branch inside navigation so that the rule can be asserted without
 * a navigation host, and so that a launch path added later cannot invent a different answer.
 */
fun launchLockDecision(state: EncryptionSessionState): LaunchLockDecision = when (state) {
    EncryptionSessionState.SetupIncomplete -> LaunchLockDecision.Main
    EncryptionSessionState.Locked -> LaunchLockDecision.Unlock
    EncryptionSessionState.Unlocked -> LaunchLockDecision.Main
}
