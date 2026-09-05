package id.andriawan.cofinance.domain.usecases.accounts

import id.andriawan.cofinance.data.repository.AccountRepository
import id.andriawan.cofinance.data.repository.TransactionRepository
import id.andriawan.cofinance.utils.None
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Removes an account together with every transaction recorded against it.
 *
 * The transactions go first, while both sides of each one still exist: removing them gives the other
 * end of a transfer its money back, which cannot be worked out once the account row is gone. Leaving
 * them behind instead was not an option — a transaction whose account no longer exists renders with
 * a blank account name and still counts toward the balance card.
 */
class DeleteAccountUseCase(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) {
    fun execute(accountId: String): Flow<Result<None>> = flow {
        try {
            transactionRepository.deleteTransactionsForAccount(accountId)
            accountRepository.deleteAccount(accountId)
            emit(Result.success(None))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
