package id.andriawan24.cofinance.andro.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import id.andriawan24.cofinance.andro.R

object BiometricAuthHelper {

    private const val ALLOWED_AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_STRONG

    fun getStatus(context: Context): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(ALLOWED_AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.Available
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NoneEnrolled
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HardwareUnavailable
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NoHardware
            else -> BiometricStatus.Unknown
        }
    }

    fun isBiometricAvailable(context: Context): Boolean {
        return getStatus(context) == BiometricStatus.Available
    }

    fun statusMessage(context: Context, status: BiometricStatus): String {
        return when (status) {
            BiometricStatus.Available -> ""
            BiometricStatus.NoneEnrolled -> context.getString(R.string.error_biometric_not_enrolled)
            BiometricStatus.NoHardware -> context.getString(R.string.error_biometric_no_hardware)
            BiometricStatus.HardwareUnavailable -> context.getString(R.string.error_biometric_unavailable)
            BiometricStatus.Unknown -> context.getString(R.string.error_biometric_generic)
        }
    }

    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onFallback: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        onFallback()
                    }
                }

                override fun onAuthenticationFailed() {
                    onError(activity.getString(R.string.error_biometric_failed))
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.title_biometric_login))
            .setSubtitle(activity.getString(R.string.description_biometric_login))
            .setNegativeButtonText(activity.getString(R.string.action_use_google_instead))
            .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
            .build()

        prompt.authenticate(promptInfo)
    }

    enum class BiometricStatus {
        Available,
        NoneEnrolled,
        NoHardware,
        HardwareUnavailable,
        Unknown
    }
}
