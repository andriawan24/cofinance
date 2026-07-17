package id.andriawan.cofinance.data.datasource

import id.andriawan.cofinance.data.model.request.IdTokenRequest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.JsonPrimitive


class SupabaseDataSource(private val supabase: SupabaseClient) {

    // region Auth

    fun getUser(): UserInfo? = supabase.auth.currentUserOrNull()

    suspend fun fetchUser(): UserInfo {
        return supabase.auth.retrieveUserForCurrentSession(updateSession = true)
    }

    suspend fun login(idTokenRequest: IdTokenRequest) {
        supabase.auth.signInWith(IDToken) {
            idToken = idTokenRequest.idToken
            provider = Google
        }
    }

    suspend fun logout() {
        supabase.auth.signOut(scope = SignOutScope.GLOBAL)
    }

    // endregion

    // region Storage

    suspend fun uploadAvatar(userId: String, bytes: ByteArray): String {
        val bucket = supabase.storage.from("avatars")
        val path = "$userId/avatar.jpg"
        bucket.upload(path, bytes) { upsert = true }
        return bucket.publicUrl(path)
    }

    suspend fun updateUserMetadata(name: String, avatarUrl: String?): UserInfo {
        return supabase.auth.updateUser {
            data {
                put("name", JsonPrimitive(name))
                put("custom_name", JsonPrimitive(name))
                if (avatarUrl != null) {
                    put("custom_avatar_url", JsonPrimitive(avatarUrl))
                }
            }
        }
    }

    suspend fun updateCycleStartDay(day: Int): UserInfo {
        return supabase.auth.updateUser {
            data {
                put("cycle_start_day", JsonPrimitive(day))
            }
        }
    }

    suspend fun updateLastCycleResetDate(date: String): UserInfo {
        return supabase.auth.updateUser {
            data {
                put("last_cycle_reset_date", JsonPrimitive(date))
            }
        }
    }

    // endregion
}
