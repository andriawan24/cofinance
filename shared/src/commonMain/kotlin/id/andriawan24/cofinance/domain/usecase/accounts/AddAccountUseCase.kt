package id.andriawan24.cofinance.domain.usecase.accounts

import id.andriawan24.cofinance.data.repository.AccountRepository
import id.andriawan24.cofinance.domain.model.request.AccountParam
import id.andriawan24.cofinance.utils.None
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


class AddAccountUseCase(private val accountRepository: AccountRepository) {
    fun execute(param: AccountParam): Flow<Result<None>> = flow {
        try {
            accountRepository.addAccount(param)
            emit(Result.success(None))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}