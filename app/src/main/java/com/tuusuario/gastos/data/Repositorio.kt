package com.tuusuario.gastos.data

import android.content.Context
import com.tuusuario.gastos.data.dao.CategoriaDao
import com.tuusuario.gastos.data.dao.GastoDao
import com.tuusuario.gastos.data.dao.IngresoDao
import com.tuusuario.gastos.data.dao.PresupuestoDao
import com.tuusuario.gastos.data.dao.SuscripcionDao
import com.tuusuario.gastos.util.primeroDelMes
import com.tuusuario.gastos.util.ultimoDelMes
import kotlinx.coroutines.flow.first
import java.time.LocalDate

object Repositorio {
    private var _db: AppDatabase? = null

    val db: AppDatabase
        get() = checkNotNull(_db) { "Repositorio no inicializado: llama a Repositorio.inicializar(context)" }

    fun inicializar(context: Context) {
        _db = AppDatabase.obtener(context)
    }

    val categorias: CategoriaDao get() = db.categoriaDao()
    val gastos: GastoDao get() = db.gastoDao()
    val presupuestos: PresupuestoDao get() = db.presupuestoDao()
    val suscripciones: SuscripcionDao get() = db.suscripcionDao()
    val ingresos: IngresoDao get() = db.ingresoDao()

    suspend fun sembrarCategoriasIniciales() {
        if (categorias.observarTodas().first().isNotEmpty()) return
        val iniciales = listOf(
            Categoria(nombre = "Comida", icono = "\uD83C\uDF54", colorHex = "#FF6B6B"),
            Categoria(nombre = "Transporte", icono = "\uD83D\uDE97", colorHex = "#00B894"),
            Categoria(nombre = "Ocio", icono = "\uD83C\uDFAE", colorHex = "#0984E3"),
            Categoria(nombre = "Hogar", icono = "\uD83C\uDFE0", colorHex = "#FDCB6E"),
            Categoria(nombre = "Salud", icono = "\uD83D\uDC8A", colorHex = "#E17055"),
            Categoria(nombre = "Otros", icono = "\uD83D\uDCE6", colorHex = "#6C5CE7"),
        )
        iniciales.forEach { categorias.insertar(it) }
    }

    suspend fun totalCategorizado(hoy: LocalDate = LocalDate.now()): Map<Long?, Double> =
        gastos.observarTotalesPorCategoria(hoy.primeroDelMes, hoy.ultimoDelMes)
            .first()
            .associate { it.categoriaId to it.total }

    suspend fun gastadoGeneral(hoy: LocalDate = LocalDate.now()): Double =
        gastos.observarTotalEntre(hoy.primeroDelMes, hoy.ultimoDelMes).first()

    suspend fun ingresado(hoy: LocalDate = LocalDate.now()): Double =
        ingresos.observarTotalEntre(hoy.primeroDelMes, hoy.ultimoDelMes).first()

    suspend fun presupuestosDelMes(hoy: LocalDate = LocalDate.now()): List<Presupuesto> =
        presupuestos.observarDelMes(hoy.monthValue, hoy.year).first()

    suspend fun alertasPresupuesto(hoy: LocalDate = LocalDate.now()): List<AlertaPresupuesto> {
        val presus = presupuestosDelMes(hoy).filter { it.notificar }
        if (presus.isEmpty()) return emptyList()

        val porCat = totalCategorizado(hoy)
        val general = gastadoGeneral(hoy)
        val categoriaNombre = categorias.observarTodas().first().associate { it.id to it.nombre }

        return presus.map { p ->
            val gastado = if (p.categoriaId == null) general else porCat[p.categoriaId] ?: 0.0
            val nombre = if (p.categoriaId == null) "General" else categoriaNombre[p.categoriaId] ?: "Categoria"
            AlertaPresupuesto(nombre, p.limite, gastado)
        }
    }
}

data class AlertaPresupuesto(val nombre: String, val limite: Double, val gastado: Double) {
    val porcentaje: Double get() = if (limite <= 0) 0.0 else gastado / limite * 100.0
    val sobrepasado: Boolean get() = porcentaje >= 100.0
    val critico: Boolean get() = porcentaje >= 90.0
}