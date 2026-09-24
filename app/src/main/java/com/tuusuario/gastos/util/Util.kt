package com.tuusuario.gastos.util

import android.graphics.Color.parseColor
import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

fun String.aColor(): Color =
    try {
        Color(parseColor(this))
    } catch (e: IllegalArgumentException) {
        Color(0xFF6C5CE7)
    }

fun Double.formatearDinero(): String =
    String.format(Locale.getDefault(), "%.2f €", this)

fun Double.formatearPorcentaje(): String =
    String.format(Locale.getDefault(), "%.0f%%", this)

fun LocalDate.formatear(): String =
    format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

val LocalDate.primeroDelMes: LocalDate
    get() = withDayOfMonth(1)

val LocalDate.ultimoDelMes: LocalDate
    get() = YearMonth.from(this).atEndOfMonth()