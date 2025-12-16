package id.andriawan24.cofinance.domain.model.request

import id.andriawan24.cofinance.data.model.request.UpdateProfileRequest

data class UpdateProfileParam(
    val name: String,
    val avatarBytes: ByteArray? = null,
    val mimeType: String? = null,
    val currentAvatarUrl: String? = null
) {
    companion object {
        fun UpdateProfileParam.toRequest(): UpdateProfileRequest {
            return UpdateProfileRequest(
                name = name,
                avatarBytes = avatarBytes,
                mimeType = mimeType,
                currentAvatarUrl = currentAvatarUrl
            )
        }
    }
}
