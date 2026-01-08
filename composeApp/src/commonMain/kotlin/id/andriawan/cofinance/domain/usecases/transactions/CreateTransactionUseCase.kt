package id.andriawan.cofinance.domain.usecases.transactions

import id.andriawan.cofinance.data.repository.AccountRepository
import id.andriawan.cofinance.data.repository.TransactionRepository
import id.andriawan.cofinance.domain.model.request.AddTransactionParam
import id.andriawan.cofinance.domain.model.request.UpdateBalanceParam
import id.andriawan.cofinance.domain.model.response.Transaction
import id.andriawan.cofinance.utils.ResultState
import id.andriawan.cofinance.utils.enums.TransactionType
import id.andriawan.cofinance.utils.extensions.orZero
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class CreateTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    fun execute(params: AddTransactionParam): Flow<ResultState<Transaction>> = flow {
        emit(ResultState.Loading)

        try {
            val response = transactionRepository.createTransaction(params)

            val updateBalanceParam = UpdateBalanceParam(
                accountsId = params.accountsId.orEmpty(),
                amount = params.amount.orZero()
            )

            when (params.type) {
                TransactionType.EXPENSE -> accountRepository.reduceAmount(param = updateBalanceParam)
                TransactionType.INCOME -> accountRepository.increaseAmount(param = updateBalanceParam)
                TransactionType.TRANSFER -> {
                    accountRepository.reduceAmount(param = updateBalanceParam)
                    params.receiverAccountsId?.let { receiverAccountId ->
                        val updateBalanceReceiverParam = UpdateBalanceParam(
                            accountsId = receiverAccountId,
                            amount = params.amount.orZero()
                        )
                        accountRepository.increaseAmount(param = updateBalanceReceiverParam)
                    }
                }

                else -> {
                    /* no-op */
                }
            }

            emit(ResultState.Success(response))
        } catch (e: Exception) {
            emit(ResultState.Error(e))
        }
    }
}
