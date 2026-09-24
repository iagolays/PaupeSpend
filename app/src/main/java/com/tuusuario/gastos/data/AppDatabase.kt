package com.tuusuario.gastos.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tuusuario.gastos.data.dao.*
import java.time.LocalDate

class Conversores {
    @TypeConverter
    fun deEpochDay(valor: Long?): LocalDate? = valor?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun aEpochDay(fecha: LocalDate?): Long? = fecha?.toEpochDay()

    @TypeConverter
    fun deMetodoPago(valor: String?): MetodoPago? = valor?.let { MetodoPago.valueOf(it) }

    @TypeConverter
    fun aMetodoPago(metodo: MetodoPago?): String? = metodo?.name
}

@Database(
    entities = [Categoria::class, Gasto::class, Ingreso::class, Suscripcion::class, Presupuesto::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Conversores::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoriaDao(): CategoriaDao
    abstract fun gastoDao(): GastoDao
    abstract fun presupuestoDao(): PresupuestoDao
    abstract fun suscripcionDao(): SuscripcionDao
    abstract fun ingresoDao(): IngresoDao

    companion object {
        @Volatile private var instancia: AppDatabase? = null

        val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE presupuestos ADD COLUMN notificar INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun obtener(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gastos.db"
                )
                    .addMigrations(MIGRACION_1_2)
                    .build().also { instancia = it }
            }
    }
}
