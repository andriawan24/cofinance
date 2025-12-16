package id.andriawan24.cofinance.domain.usecase.accounts

import id.andriawan24.cofinance.data.repository.AccountRepository
import id.andriawan24.cofinance.domain.model.response.AccountByGroup
import id.andriawan24.cofinance.utils.ResultState
import id.andriawan24.cofinance.utils.enums.AccountGroupType.Companion.getBackgroundColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GetAccountsUseCase(private val accountRepository: AccountRepository) {
    fun execute(): Flow<ResultState<List<AccountByGroup>>> = flow {
        emit(ResultState.Loading)

        try {
            val user = accountRepository.getAccounts()
            val accounts = user.groupBy { account -> account.group }

            val accountsByGroup = accounts.map { accountByGroup ->
                val backgroundColor = accountByGroup.key.getBackgroundColor()
                val displayName = accountByGroup.key.displayName
                val totalAmount = accountByGroup.value.sumOf { account -> account.balance }

                AccountByGroup(
                    groupLabel = displayName,
                    backgroundColor = backgroundColor,
                    totalAmount = totalAmount,
                    accountGroupType = accountByGroup.key,
                    accounts = accountByGroup.value
                )
            }

            emit(ResultState.Success(accountsByGroup))
        } catch (e: Exception) {
            emit(ResultState.Error(e))
        }
    }
}