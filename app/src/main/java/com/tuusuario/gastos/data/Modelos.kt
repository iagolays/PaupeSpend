package com.tuusuario.gastos.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class MetodoPago { EFECTIVO, TARJETA }

/**
 * Categoria de gasto: "Comida", "Transporte", "Ocio"...
 * icono guarda el nombre de un icono de lucide/material (o un emoji si prefieres eso).
 */
@Entity(tableName = "categorias")
data class Categoria(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val icono: String,      // ej: "utensils", "car", "film" -> o un emoji "🍔"
    val colorHex: String,   // ej: "#FF6B6B" para el grafico hexagonal
    val limiteMensual: Double? = null // null = sin limite en esta categoria
)

/**
 * Un gasto individual. Puede venir de un ticket escaneado (OCR) o metido a mano.
 */
@Entity(
    tableName = "gastos",
    foreignKeys = [
        ForeignKey(
            entity = Categoria::class,
            parentColumns = ["id"],
            childColumns = ["categoriaId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Gasto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val descripcion: String,
    val monto: Double,
    val fecha: LocalDate,
    val metodoPago: MetodoPago,
    val categoriaId: Long?,
    val rutaTicket: String? = null,  // path a la foto del ticket, si la hay
    val esFijo: Boolean = false      // alquiler, suscripciones, etc.
)

/**
 * Ingresos: beca, paga de los padres, nómina...
 */
@Entity(tableName = "ingresos")
data class Ingreso(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val descripcion: String,
    val monto: Double,
    val fecha: LocalDate,
    val esRecurrente: Boolean = false
)

/**
 * Gasto fijo recurrente tipo suscripcion (Netflix, Spotify, alquiler...).
 * Se usa para generar el apartado especifico que pediste y para
 * avisar antes de que se cobre.
 */
@Entity(tableName = "suscripciones")
data class Suscripcion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val monto: Double,
    val diaDeCobro: Int,       // 1-31
    val categoriaId: Long?,
    val activa: Boolean = true
)

/**
 * Presupuesto mensual global o por categoria, para la barra de
 * porcentaje empleado y las alertas.
 */
@Entity(tableName = "presupuestos")
data class Presupuesto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoriaId: Long?,   // null = presupuesto general
    val limite: Double,
    val mes: Int,
    val anio: Int,
    val notificar: Boolean = false // si esta activada la campana de aviso
)
