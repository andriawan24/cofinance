package id.andriawan.cofinance.domain.usecases.accounts

import id.andriawan.cofinance.data.repository.AccountRepository
import id.andriawan.cofinance.domain.model.response.AccountByGroup
import id.andriawan.cofinance.utils.ResultState
import id.andriawan.cofinance.utils.enums.AccountGroupType.Companion.getBackgroundColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

class GetAccountsUseCase(private val accountRepository: AccountRepository) {
    fun execute(): Flow<ResultState<List<AccountByGroup>>> {
        return accountRepository.watchAccounts()
            .map { user ->
                val accounts = user.groupBy { account -> account.group }

                val accountsGroup = accounts.map { accountByGroup ->
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

                ResultState.Success(accountsGroup)
            }
            .catch { e -> ResultState.Error(Exception(e)) }
//        emit(ResultState.Loading)
//
//        try {
//            val user = accountRepository.getAccounts()
//            val accounts = user.groupBy { account -> account.group }
//
//            val accountsByGroup = accounts.map { accountByGroup ->
//                val backgroundColor = accountByGroup.key.getBackgroundColor()
//                val displayName = accountByGroup.key.displayName
//                val totalAmount = accountByGroup.value.sumOf { account -> account.balance }
//
//                AccountByGroup(
//                    groupLabel = displayName,
//                    backgroundColor = backgroundColor,
//                    totalAmount = totalAmount,
//                    accountGroupType = accountByGroup.key,
//                    accounts = accountByGroup.value
//                )
//            }
//
//            emit(ResultState.Success(accountsByGroup))
//        } catch (e: Exception) {
//            emit(ResultState.Error(e))
//        }
    }
}
