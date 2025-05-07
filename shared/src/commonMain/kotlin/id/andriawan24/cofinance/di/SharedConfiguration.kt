package id.andriawan24.cofinance.di

import org.koin.dsl.KoinAppDeclaration

fun koinSharedConfiguration(): KoinAppDeclaration = {
    modules(dataModule)
    modules(domainModule)
}