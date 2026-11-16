# App lock

**Question:** How do the PIN, biometric shortcut, and auto-lock work?

**Last verified:** 2026-11-16 against `ac66286`. Established by reading the source. Android behaviour was observed on a physical device in August 2026, before the test suite was removed. Path shorthand is defined in [README.md](README.md).

## Summary

The app lock gates access to the decrypted data key described in [client-side-encryption.md](client-side-encryption.md). A six-digit PIN is the base; a biometric shortcut can be added on top of it. Both are optional and are set from Profile after encryption setup.

Lock state is a property of whether the data key is held in memory, not a screen in front of the data. Locking clears the key.

A user who has not completed encryption setup — every local-only user — never sees an unlock screen and has no security settings.

## PIN

The PIN is exactly six digits (`app/pages/lock/PinRules.kt:12-22`).

Unlocking **derives** the data key from the PIN; it does not compare the PIN with a stored value. The PIN wrap is sealed under `HKDF(scrypt(PIN) || deviceSecret)`, and nothing about the PIN is stored (`core/crypto/PinKeyWrapper.kt:29-134`). A comparison-based check would leave the key reachable by any code path that skipped the comparison; derivation makes the PIN structurally necessary.

`AppLock` (`core/lock/AppLock.kt:67-223`):

- Setting the first PIN requires an unlocked session. Changing it requires the current PIN.
- Removing the PIN also disables biometric.

## Failed attempts

`LockoutPolicy` fixes the schedule (`core/lock/LockoutPolicy.kt:18-51`):

| Consecutive failures | Effect |
|----------------------|--------|
| 1 to 4 | None |
| 5 onward | A delay before the next attempt: 30 seconds, doubling to a 5-minute cap |
| 10 | Local key material is destroyed |

After destruction, access requires the recovery phrase. `LocalKeyMaterialDestroyer` erases the local key material and the platform keys and makes no remote call, so the uploaded recovery-phrase wrap is out of its reach (`core/lock/LocalKeyMaterialDestroyer.kt:28-38`).

The threshold is not configurable. The delay is what defeats a search over a million PINs; the threshold only bounds an attacker who holds the device indefinitely. Five was rejected because pocket entry and shared-device use produce accidental runs of that length.

`FailedAttemptGuard` applies the delay before derivation. A successful unlock writes a zero rather than deleting the counter, so a missing or unreadable counter on a device that still holds a PIN wrap is treated as tampering and destroys local key material. This closes the "delete the counter every four tries" hole (`core/lock/FailedAttemptGuard.kt:68-185`).

### Reinstalling does not reset the count

| Platform | Counter storage | Effect of reinstall |
|----------|-----------------|---------------------|
| iOS | Keychain item, `AfterFirstUnlockThisDeviceOnly` | The counter survives, because Keychain items outlive the app |
| Android | File sealed with AES-GCM under a Keystore key | The counter is deleted together with the Keystore entries the PIN wrap is derived from, so no openable wrap is left to attack |

On Android the integer does not persist, but the attacker gains nothing: a reinstalled app needs the recovery phrase, which is where ten failures would have led.

## Biometric

Biometric is a shortcut over the PIN, never a replacement. Enabling it requires a PIN to be set and re-entered, and the key it seals is obtained by deriving it from that PIN — there is no path that seals an already-unlocked session's key (`core/lock/AppLock.kt:205-215`).

| | Android | iOS |
|---|---------|-----|
| Storage | Keystore AES-256-GCM key, authentication required per use, `BIOMETRIC_STRONG` only | Keychain item with `kSecAccessControlBiometryCurrentSet` |
| Invalidated by | A new biometric enrollment | A change to the enrolled biometric set |
| Prompt | `androidx.biometric` `BiometricPrompt` with a `CryptoObject` | The Keychain shows the system prompt |

Sources: `core-android/lock/BiometricKeyBox.android.kt:62-315`, `core-ios/lock/BiometricKeyBox.ios.kt:66-212`.

Device credential is deliberately excluded on Android. When the platform reports invalidation, the sealed copy is discarded, so the state after an enrollment change is the state before biometric was enabled. Because a PIN is always underneath, that costs the user six digits rather than a recovery-phrase restore.

Every non-success outcome — cancelled, unavailable, invalidated — falls back to PIN entry, and a cancelled prompt does not count as a failed attempt.

On Android the prompt needs a `FragmentActivity`. `MainActivity` extends `AppCompatActivity` and registers itself as `BiometricPromptHost.current` (`androidApp/src/main/kotlin/id/andriawan/cofinance/MainActivity.kt:39-58`). `AppCompatActivity` rather than a bare `FragmentActivity` because below API 28 the biometric library draws its own dialog from AppCompat resources.

## Auto-lock

`AutoLockController` locks the session after the app has been in the background longer than the timeout (`core/lock/AutoLockController.kt:47-83`). Two mechanisms cover the two ways background time passes: a scheduled job locks while backgrounded, and an elapsed-time check on return covers a process that was frozen and never ran the timer.

| Option | |
|--------|---|
| Immediately | |
| 1 minute | Default |
| 5 minutes | |
| 15 minutes | |

One minute is the default because switching to a banking or messaging app to read an amount and returning is the app's core entry flow; locking on every such switch would make the lock the dominant interaction.

The controller is driven from `ON_STOP` and `ON_START` at the root of the composition, not pause and resume, which a permission dialog also fires (`app/pages/App.kt:70-75`). The choice is persisted in SharedPreferences on Android and `NSUserDefaults` on iOS.

## Where the lock appears

- **At launch**, for a signed-in user whose device holds key material and has a PIN set (`app/pages/splash/SplashViewModel.kt:121-123`). The unlock screen offers the biometric prompt automatically when it is enabled.
- **After a timeout**, navigation returns to the launch sequence when the session locks on any other screen (`app/navigations/MainNavigation.kt:69-82`).
- **After ten failures**, the unlock screen routes to recovery-phrase restore.

## Settings

`SecuritySettingsSection` renders inside Profile and is absent, not disabled, before encryption setup (`app/pages/profile/security/SecuritySettingsSection.kt:120-128`). It offers: set, change, or remove the PIN; turn biometric on or off; choose the auto-lock timeout; and reveal, copy, or save the recovery phrase.

One PIN prompt stands in front of every change and counts against the same failed-attempt guard as the unlock screen, so settings are not an unthrottled way to test PINs. Turning biometric off requires the PIN too, even though it only tightens the lock.

Two actions have no current PIN to require: setting the first PIN, and changing the auto-lock timeout when no PIN is set.

## Evidence

Before the test suite was removed, host tests covered the lockout schedule, destruction at ten failures, reset on success, biometric refusal without a PIN, fallback on cancellation, and auto-lock timing. On a physical Android device, instrumented tests confirmed that the biometric key requires authentication, is invalidated by enrollment, and never leaves the Keystore.

Never observed:

- A real, successful biometric prompt. No test can present a finger or a face.
- The failed-attempt counter's behaviour across an actual uninstall and reinstall, on either platform.
- Anything about the iOS biometric box or the iOS Keychain stores, which could not run in the test harness.

## Open

- **A signed-in user with no PIN can be locked out until the app is restarted.** Traced in code, not reproduced. Auto-lock moves the session to `Locked` whether or not a PIN is set. The launch sequence only unlocks silently through the device wrap when the session is `SetupIncomplete`, so it routes to the unlock screen, which reports that no PIN is set and offers nothing else. Killing the process clears it, because a fresh launch starts from `SetupIncomplete`.
- **`NSFaceIDUsageDescription` is missing.** Neither `Info.plist` nor the Xcode project declares it. iOS requires the key before an app may use Face ID, so the biometric shortcut is likely to fail on Face ID devices.
- **Stale comment.** `BiometricKeyBox.android.kt:29-34` still says `MainActivity` extends `ComponentActivity` and must be changed. It was changed.
- The three items under "Never observed" need a manual pass on real hardware.
