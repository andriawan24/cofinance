package id.andriawan.cofinance.domain.model.request

import id.andriawan.cofinance.data.model.request.IdTokenRequest

data class IdTokenParam(
    val idToken: String
) {
    companion object {
        fun IdTokenParam.toRequest(): IdTokenRequest {
            return IdTokenRequest(idToken = this.idToken)
        }
    }
}
