package id.andriawan24.cofinance.di

import id.andriawan24.cofinance.data.datasource.SupabaseDataSource
import id.andriawan24.cofinance.data.repository.AuthenticationRepository
import id.andriawan24.cofinance.utils.SupabaseClientHelper
import io.github.jan.supabase.SupabaseClient
import org.koin.dsl.module

val dataModule = module {
    single<SupabaseClient> { SupabaseClientHelper.createClient() }
    single<SupabaseDataSource> { SupabaseDataSource(get()) }
    single<AuthenticationRepository> { AuthenticationRepository(get()) }
}