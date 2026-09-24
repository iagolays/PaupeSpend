package com.tuusuario.gastos.data.dao

import androidx.room.*
import com.tuusuario.gastos.data.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface CategoriaDao {
    @Query("SELECT * FROM categorias ORDER BY nombre")
    fun observarTodas(): Flow<List<Categoria>>

    @Insert
    suspend fun insertar(categoria: Categoria): Long

    @Update
    suspend fun actualizar(categoria: Categoria)

    @Delete
    suspend fun borrar(categoria: Categoria)
}

@Dao
interface GastoDao {
    @Query("SELECT * FROM gastos ORDER BY fecha DESC")
    fun observarTodos(): Flow<List<Gasto>>

    @Query("SELECT * FROM gastos WHERE fecha BETWEEN :desde AND :hasta ORDER BY fecha DESC")
    fun observarEntreFechas(desde: LocalDate, hasta: LocalDate): Flow<List<Gasto>>

    @Query("""
        SELECT categoriaId, SUM(monto) as total
        FROM gastos
        WHERE fecha BETWEEN :desde AND :hasta
        GROUP BY categoriaId
    """)
    fun observarTotalesPorCategoria(desde: LocalDate, hasta: LocalDate): Flow<List<TotalPorCategoria>>

    @Query("SELECT * FROM gastos WHERE esFijo = 1 ORDER BY fecha DESC")
    fun observarFijos(): Flow<List<Gasto>>

    @Query("SELECT COALESCE(SUM(monto), 0.0) FROM gastos WHERE fecha BETWEEN :desde AND :hasta")
    fun observarTotalEntre(desde: LocalDate, hasta: LocalDate): Flow<Double>

    @Insert
    suspend fun insertar(gasto: Gasto): Long

    @Update
    suspend fun actualizar(gasto: Gasto)

    @Delete
    suspend fun borrar(gasto: Gasto)
}

data class TotalPorCategoria(
    val categoriaId: Long?,
    val total: Double
)

@Dao
interface PresupuestoDao {
    @Query("SELECT * FROM presupuestos WHERE mes = :mes AND anio = :anio")
    fun observarDelMes(mes: Int, anio: Int): Flow<List<Presupuesto>>

    @Query("SELECT * FROM presupuestos ORDER BY anio DESC, mes DESC")
    fun observarTodas(): Flow<List<Presupuesto>>

    @Insert
    suspend fun insertar(presupuesto: Presupuesto): Long

    @Update
    suspend fun actualizar(presupuesto: Presupuesto)

    @Delete
    suspend fun borrar(presupuesto: Presupuesto)
}

@Dao
interface SuscripcionDao {
    @Query("SELECT * FROM suscripciones WHERE activa = 1 ORDER BY diaDeCobro")
    fun observarActivas(): Flow<List<Suscripcion>>

    @Insert
    suspend fun insertar(suscripcion: Suscripcion): Long

    @Update
    suspend fun actualizar(suscripcion: Suscripcion)
}

@Dao
interface IngresoDao {
    @Query("SELECT * FROM ingresos ORDER BY fecha DESC")
    fun observarTodos(): Flow<List<Ingreso>>

    @Query("SELECT COALESCE(SUM(monto), 0.0) FROM ingresos WHERE fecha BETWEEN :desde AND :hasta")
    fun observarTotalEntre(desde: LocalDate, hasta: LocalDate): Flow<Double>

    @Insert
    suspend fun insertar(ingreso: Ingreso): Long
}
