package id.andriawan24.cofinance.data.repository

import id.andriawan24.cofinance.data.datasource.SupabaseDataSource
import id.andriawan24.cofinance.domain.model.request.AccountParam
import id.andriawan24.cofinance.domain.model.request.AccountParam.Companion.toRequest
import id.andriawan24.cofinance.domain.model.request.UpdateBalanceParam
import id.andriawan24.cofinance.domain.model.request.UpdateBalanceParam.Companion.toRequest
import id.andriawan24.cofinance.domain.model.response.Account

interface AccountRepository {
    suspend fun getAccounts(): List<Account>
    suspend fun addAccount(param: AccountParam)
    suspend fun reduceAmount(param: UpdateBalanceParam)
    suspend fun increaseAmount(param: UpdateBalanceParam)
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

    override suspend fun reduceAmount(param: UpdateBalanceParam) {
        supabaseDataSource.reduceBalance(param.toRequest())
    }

    override suspend fun increaseAmount(param: UpdateBalanceParam) {
        supabaseDataSource.increaseBalance(param.toRequest())
    }
}