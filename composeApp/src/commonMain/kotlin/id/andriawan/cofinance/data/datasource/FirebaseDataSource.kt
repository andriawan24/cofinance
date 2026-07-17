package id.andriawan.cofinance.data.datasource

import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.storage.FirebaseStorage
import id.andriawan.cofinance.data.model.request.IdTokenRequest
import id.andriawan.cofinance.data.model.response.FirebaseUserResponse
import id.andriawan.cofinance.utils.toFirebaseData
import kotlinx.serialization.Serializable

class FirebaseDataSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private var cachedProfile: FirebaseProfileDocument? = null

    fun getUser(): FirebaseUserResponse? = auth.currentUser?.toResponse(cachedProfile)

    suspend fun fetchUser(): FirebaseUserResponse {
        val current = requireUser()
        current.reload()
        val refreshed = requireUser()
        val profile = loadOrCreateProfile(refreshed)
        cachedProfile = profile
        return refreshed.toResponse(profile)
    }

    suspend fun login(idTokenRequest: IdTokenRequest) {
        val credential = GoogleAuthProvider.credential(
            idToken = idTokenRequest.idToken,
            accessToken = null
        )

        val user = auth.signInWithCredential(credential).user
            ?: error("Firebase authentication returned no user")

        cachedProfile = loadOrCreateProfile(user)
    }

    suspend fun logout() {
        auth.signOut()
        cachedProfile = null
    }

    suspend fun uploadAvatar(userId: String, bytes: ByteArray): String {
        check(requireUser().uid == userId) { "Firebase user scope mismatch" }
        require(bytes.isNotEmpty()) { "Avatar data is empty" }
        val reference = storage.reference("avatars/$userId/avatar.jpg")
        reference.putData(bytes.toFirebaseData())
        return reference.getDownloadUrl()
    }

    suspend fun updateUserMetadata(name: String, avatarUrl: String?): FirebaseUserResponse {
        val user = requireUser()
        val currentProfile = cachedProfile ?: loadOrCreateProfile(user)

        val updatedProfile = currentProfile.copy(
            customName = name,
            customAvatarUrl = avatarUrl ?: currentProfile.customAvatarUrl
        )

        user.updateProfile(
            displayName = name,
            photoUrl = updatedProfile.customAvatarUrl.ifBlank { user.photoURL }
        )

        profileDocument(user.uid).set(updatedProfile, merge = true)
        cachedProfile = updatedProfile
        return user.toResponse(updatedProfile)
    }

    suspend fun updateCycleStartDay(day: Int): FirebaseUserResponse {
        val user = requireUser()
        val updatedProfile = (cachedProfile ?: loadOrCreateProfile(user)).copy(
            cycleStartDay = day.coerceIn(1, 28)
        )

        profileDocument(user.uid).set(updatedProfile, merge = true)
        cachedProfile = updatedProfile
        return user.toResponse(updatedProfile)
    }

    suspend fun updateLastCycleResetDate(date: String): FirebaseUserResponse {
        val user = requireUser()
        val updatedProfile = (cachedProfile ?: loadOrCreateProfile(user)).copy(
            lastCycleResetDate = date
        )
        profileDocument(user.uid).set(updatedProfile, merge = true)
        cachedProfile = updatedProfile
        return user.toResponse(updatedProfile)
    }

    private fun requireUser(): FirebaseUser =
        auth.currentUser ?: error("No authenticated Firebase user")

    private fun profileDocument(userId: String) =
        firestore.collection(USERS_COLLECTION).document(userId)

    private suspend fun loadOrCreateProfile(user: FirebaseUser): FirebaseProfileDocument {
        val snapshot = profileDocument(user.uid).get()
        if (snapshot.exists) return snapshot.data()

        val profile = FirebaseProfileDocument(
            customName = user.displayName.orEmpty(),
            customAvatarUrl = user.photoURL.orEmpty()
        )

        profileDocument(user.uid).set(profile)

        return profile
    }

    private fun FirebaseUser.toResponse(profile: FirebaseProfileDocument?): FirebaseUserResponse {
        return FirebaseUserResponse(
            avatarUrl = profile?.customAvatarUrl?.ifBlank { null } ?: photoURL.orEmpty(),
            name = profile?.customName?.ifBlank { null } ?: displayName.orEmpty(),
            email = email.orEmpty(),
            id = uid,
            cycleStartDay = profile?.cycleStartDay?.coerceIn(1, 28) ?: 1,
            lastCycleResetDate = profile?.lastCycleResetDate
        )
    }

    companion object {
        private const val USERS_COLLECTION = "users"
    }
}

@Serializable
private data class FirebaseProfileDocument(
    val customName: String = "",
    val customAvatarUrl: String = "",
    val cycleStartDay: Int = 1,
    val lastCycleResetDate: String? = null
)
