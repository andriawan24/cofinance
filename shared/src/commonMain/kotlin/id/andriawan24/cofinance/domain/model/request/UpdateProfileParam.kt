package id.andriawan24.cofinance.domain.model.request

import id.andriawan24.cofinance.data.model.request.UpdateProfileRequest

data class UpdateProfileParam(
    val name: String,
    val currentAvatarUrl: String? = null,
    val avatarBytes: ByteArray? = null,
    val mimeType: String? = null
) {
    init {
        if ((avatarBytes == null) xor (mimeType == null)) {
            throw IllegalArgumentException("Avatar bytes and mimeType must be provided together")
        }
    }

    companion object {
        fun UpdateProfileParam.toRequest(): UpdateProfileRequest {
            return UpdateProfileRequest(
                name = name,
                currentAvatarUrl = currentAvatarUrl,
                avatarBytes = avatarBytes,
                mimeType = mimeType
            )
        }
    }
}
