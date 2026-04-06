package id.andriawan.cofinance.domain.usecases.accounts

import id.andriawan.cofinance.data.repository.AccountRepository
import id.andriawan.cofinance.utils.None
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DeleteAccountUseCase(private val accountRepository: AccountRepository) {
    fun execute(accountId: String): Flow<Result<None>> = flow {
        try {
            accountRepository.deleteAccount(accountId)
            emit(Result.success(None))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
