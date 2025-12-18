package com.docstencil.core.builtin

import com.docstencil.core.error.TemplaterException
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class BuiltinFormatter(private val locale: Locale) {
    fun format(value: Any?, pattern: String?): String? {
        if (value == null) return null

        return when (value) {
            // Date types: use default or custom pattern
            is LocalDate -> formatDate(value, pattern ?: "yyyy/MM/dd")
            is LocalDateTime -> formatDate(value, pattern ?: "yyyy/MM/dd HH:mm")
            is ZonedDateTime -> formatDate(value, pattern ?: "yyyy/MM/dd HH:mm")
            is Date -> formatDate(value, pattern ?: "yyyy/MM/dd HH:mm")

            // Integer types: thousand separators only
            is Byte, is Short, is Int, is Long ->
                formatNumber(value as Number, pattern ?: "#,##0")

            // Floating point: thousand separators + 2 decimals
            is Float, is Double ->
                formatNumber(value as Number, pattern ?: "#,##0.00")

            else -> throw TemplaterException.DeepRuntimeError(
                $$"$format: unsupported type $${value.javaClass.name}. " +
                        "Supported types: LocalDate, LocalDateTime, ZonedDateTime, Date, " +
                        "Byte, Short, Int, Long, Float, Double",
            )
        }
    }

    fun formatDate(date: Any?, pattern: String): String? {
        if (date == null) {
            return null
        }

        return when (date) {
            is LocalDateTime -> formatLocalDateTime(date, pattern)
            is LocalDate -> formatLocalDate(date, pattern)
            is ZonedDateTime -> formatZonedDateTime(date, pattern)
            is Date -> formatDate(date, pattern)
            else -> throw TemplaterException.DeepRuntimeError(
                "Can only format dates that have one of the following types: " +
                        "LocalDateTime, LocalDate, ZonedDateTime, Date. " +
                        "Actual type: " + date.javaClass.toString(),
            )
        }
    }

    fun formatNumber(number: Number?, pattern: String?): String {
        if (number == null) {
            return ""
        }
        if (pattern == null) {
            val formatter = NumberFormat.getNumberInstance(locale)
            return formatter.format(number)
        }

        val decimalFormat = DecimalFormat(pattern, DecimalFormatSymbols(locale))
        return decimalFormat.format(number)
    }

    private fun formatLocalDateTime(date: LocalDateTime, pattern: String): String {
        val formatter = DateTimeFormatter.ofPattern(pattern, locale)
        return date.format(formatter)
    }

    private fun formatLocalDate(date: LocalDate, pattern: String): String {
        val formatter = DateTimeFormatter.ofPattern(pattern, locale)
        return date.format(formatter)
    }

    private fun formatZonedDateTime(date: ZonedDateTime, pattern: String): String {
        val formatter = DateTimeFormatter.ofPattern(pattern, locale)
        return date.format(formatter)
    }

    private fun formatDate(date: Date, pattern: String): String {
        val formatter = java.text.SimpleDateFormat(pattern, locale)
        return formatter.format(date)
    }
}
