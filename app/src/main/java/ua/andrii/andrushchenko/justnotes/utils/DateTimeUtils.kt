package ua.andrii.andrushchenko.justnotes.utils

import android.text.format.DateFormat

object DateTimeUtils {

    fun getFormattedString(timeMillis: Long): String =
        DateFormat.format("MMM d, HH:mm", timeMillis).toString()
}