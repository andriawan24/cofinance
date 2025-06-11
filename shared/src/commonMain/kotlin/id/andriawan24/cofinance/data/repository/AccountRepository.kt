package id.andriawan24.cofinance.data.repository

import id.andriawan24.cofinance.data.datasource.SupabaseDataSource
import id.andriawan24.cofinance.domain.model.request.AccountParam
import id.andriawan24.cofinance.domain.model.request.AccountParam.Companion.toRequest
import id.andriawan24.cofinance.domain.model.response.Account

interface AccountRepository {
    suspend fun getAccounts(): List<Account>
    suspend fun addAccount(param: AccountParam)
}

class AccountRepositoryImpl(
    private val supabaseDataSource: SupabaseDataSource
) : AccountRepository {
    override suspend fun getAccounts(): List<Account> {
        return supabaseDataSource.getAccounts().map { Account.from(it) }
    }

    override suspend fun addAccount(param: AccountParam) {
        supabaseDataSource.addAccount(param.toRequest())
    }
}