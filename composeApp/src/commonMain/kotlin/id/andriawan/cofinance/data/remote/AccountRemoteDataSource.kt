package id.andriawan.cofinance.data.remote

import id.andriawan.cofinance.data.model.AccountResponse

interface AccountRemoteDataSource {
    suspend fun getAccounts(): List<AccountResponse>
    suspend fun upsertAccounts(accounts: List<AccountResponse>)
    suspend fun deleteAccount(id: String)
}
