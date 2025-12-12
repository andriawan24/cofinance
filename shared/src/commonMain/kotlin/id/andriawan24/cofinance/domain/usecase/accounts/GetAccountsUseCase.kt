package id.andriawan24.cofinance.domain.usecase.accounts

import id.andriawan24.cofinance.data.repository.AccountRepository
import id.andriawan24.cofinance.domain.model.response.Account
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


class GetAccountsUseCase(private val accountRepository: AccountRepository) {
    fun execute(): Flow<Result<List<Account>>> = flow {
        try {
            val user = accountRepository.getAccounts()
            emit(Result.success(user))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}