package id.andriawan.cofinance.data.remote

import id.andriawan.cofinance.data.model.response.AccountResponse

interface AccountRemoteDataSource {
    suspend fun getAccounts(): List<AccountResponse>
    suspend fun upsertAccounts(accounts: List<AccountResponse>)
}
