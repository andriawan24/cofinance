package id.andriawan.cofinance.data.session

import dev.gitlive.firebase.auth.FirebaseAuth

interface SessionPolicy {
    fun isSignedIn(): Boolean
    fun userIdOrNull(): String?

    fun requireUserId(): String = userIdOrNull() ?: throw SignedInSessionRequiredException()
}

class FirebaseSessionPolicy(
    private val firebaseAuth: FirebaseAuth
) : SessionPolicy {
    override fun isSignedIn(): Boolean = firebaseAuth.currentUser != null
    override fun userIdOrNull(): String? = firebaseAuth.currentUser?.uid
}

class SignedInSessionRequiredException : IllegalStateException("Sign in is required for this feature")
