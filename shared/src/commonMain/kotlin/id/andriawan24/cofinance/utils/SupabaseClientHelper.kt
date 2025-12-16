package id.andriawan24.cofinance.utils

import com.andreasgift.kmpweatherapp.BuildKonfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

object SupabaseClientHelper {
    fun createClient(
        url: String = BuildKonfig.SUPABASE_URL,
        apiKey: String = BuildKonfig.SUPABASE_API_KEY
    ): SupabaseClient {
        return createSupabaseClient(url, apiKey) {
            install(Auth.Companion)
            install(Postgrest.Companion)
            install(Realtime.Companion)
            install(Storage.Companion)
        }
    }
}