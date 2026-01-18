package id.andriawan.cofinance.utils

import com.andriawan.cofinance.BuildKonfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.googleNativeLogin
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.logging.LogLevel
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseHelper {
    fun createClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildKonfig.SUPABASE_URL,
            supabaseKey = BuildKonfig.SUPABASE_API_KEY
        ) {
            defaultLogLevel = LogLevel.DEBUG

            install(Auth)
            install(Postgrest)
            install(ComposeAuth) {
                googleNativeLogin(serverClientId = BuildKonfig.GOOGLE_AUTH_API_KEY)
            }
        }
    }
}
