package id.andriawan.cofinance.data.local

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import id.andriawan.cofinance.data.datasource.SupabaseDataSource
import org.koin.compose.koinInject

@Composable
actual fun rememberCofinanceDatabase(): CofinanceDatabase {
    val supabaseDataSource = koinInject<SupabaseDataSource>()
    return remember { OnlineOnlyDatabase(supabaseDataSource) }
}
