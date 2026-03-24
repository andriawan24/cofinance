package id.andriawan.cofinance.domain.usecases.accounts

import id.andriawan.cofinance.data.repository.AccountRepository
import id.andriawan.cofinance.data.repository.TransactionRepository
import id.andriawan.cofinance.domain.model.request.AddTransactionParam
import id.andriawan.cofinance.domain.model.response.Account
import id.andriawan.cofinance.utils.enums.TransactionType

class ResetAccountBalanceUseCase(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend fun execute(account: Account, cycleEndDate: String) {
        if (account.balance == 0L) return

        // Create a CYCLE_RESET transaction to zero out the balance
        transactionRepository.createTransaction(
            AddTransactionParam(
                amount = kotlin.math.abs(account.balance),
                category = "CYCLE_RESET",
                date = cycleEndDate,
                fee = 0L,
                notes = "Cycle reset - balance cleared",
                accountsId = account.id,
                type = TransactionType.CYCLE_RESET
            )
        )

        // Directly set balance to 0 by applying the negative delta
        accountRepository.updateAccountBalance(account.id, -account.balance)
    }
}
