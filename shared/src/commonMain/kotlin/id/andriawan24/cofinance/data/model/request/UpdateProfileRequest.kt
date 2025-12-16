package id.andriawan24.cofinance.data.model.request

data class UpdateProfileRequest(
    val name: String,
    val avatarBytes: ByteArray? = null,
    val mimeType: String? = null,
    val currentAvatarUrl: String? = null
)
