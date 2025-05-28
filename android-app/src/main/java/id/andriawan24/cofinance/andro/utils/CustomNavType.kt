package id.andriawan24.cofinance.andro.utils

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import id.andriawan24.cofinance.domain.model.response.ReceiptScan
import kotlinx.serialization.json.Json

object CustomNavType {
    val receiptScanType = object : NavType<ReceiptScan>(isNullableAllowed = false) {
        override fun get(bundle: SavedState, key: String): ReceiptScan? {
            return parseValue(bundle.getString(key) ?: return null)
        }

        override fun put(bundle: SavedState, key: String, value: ReceiptScan) {
            bundle.putString(key, serializeAsValue(value))
        }

        override fun parseValue(value: String): ReceiptScan = Json.decodeFromString(value)
        override fun serializeAsValue(value: ReceiptScan): String = Json.encodeToString(value)
    }
}