package id.andriawan24.cofinance.domain.usecase.transaction

import id.andriawan24.cofinance.data.repository.AccountRepository
import id.andriawan24.cofinance.data.repository.TransactionRepository
import id.andriawan24.cofinance.domain.model.request.AddTransactionParam
import id.andriawan24.cofinance.domain.model.request.UpdateBalanceParam
import id.andriawan24.cofinance.domain.model.response.Transaction
import id.andriawan24.cofinance.utils.enums.TransactionType
import id.andriawan24.cofinance.utils.ext.orZero
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class CreateTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    fun execute(params: AddTransactionParam): Flow<Result<Transaction>> = flow {
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
            }

            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
}