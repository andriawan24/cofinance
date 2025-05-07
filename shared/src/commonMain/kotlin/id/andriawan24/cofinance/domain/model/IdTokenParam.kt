package id.andriawan24.cofinance.domain.model

import id.andriawan24.cofinance.data.model.request.IdTokenRequest

data class IdTokenParam(
    val idToken: String
) {
    companion object {
        fun IdTokenParam.toRequest(): IdTokenRequest {
            return IdTokenRequest(idToken = this.idToken)
        }
    }
}
