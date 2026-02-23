package id.andriawan.cofinance.utils

import com.andriawan.cofinance.BuildKonfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.logging.LogLevel
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseHelper {
    fun createClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildKonfig.SUPABASE_URL,
            supabaseKey = BuildKonfig.SUPABASE_API_KEY
        ) {
            defaultLogLevel = LogLevel.DEBUG

            install(Auth)
            install(Postgrest)
            install(Storage)
        }
    }
}
