package id.andriawan24.cofinance.android.utils

import android.content.Context
import java.util.Calendar

object TimeHelper {
    fun Context.getGreeting(): String {
        val cal = Calendar.getInstance()
        val time = cal.get(Calendar.HOUR_OF_DAY)
        return when (time) {
            in 2..11 -> "Good Morning"
            in 11..16 -> "Good Afternoon"
            else -> "Good Night"
        }
    }
}