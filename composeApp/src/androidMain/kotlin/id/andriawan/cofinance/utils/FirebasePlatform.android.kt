package id.andriawan.cofinance.utils

import dev.gitlive.firebase.storage.Data

internal actual fun ByteArray.toFirebaseData(): Data = Data(this)
