package com.tuusuario.gastos.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.tuusuario.gastos.data.Repositorio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

object Notificaciones {
    const val CANAL_PRESUPUESTO = "presupuesto"
    private const val ID_UNICO = 2001
    private const val ACCION_COMPROBAR = "com.tuusuario.gastos.COMPROBAR_PRESUPUESTO"

    fun crearCanal(context: Context) {
        val canal = NotificationChannel(
            CANAL_PRESUPUESTO,
            "Alertas de presupuesto",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Avisos cuando un presupuesto esta cerca de agotarse o se supera"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    fun programarComprobacion(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, RecibidorPresupuesto::class.java).setAction(ACCION_COMPROBAR)
        val pendiente = PendingIntent.getBroadcast(
            context,
            ACCION_COMPROBAR.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val hora = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (hora.timeInMillis <= System.currentTimeMillis()) {
            hora.add(Calendar.DAY_OF_YEAR, 1)
        }
        // Alarma diaria de un solo disparo: el propio receptor la re-programa
        // al dia siguiente, asi es mas fiable que una repeticion inexacta.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, hora.timeInMillis, pendiente)
    }

    fun comprobarAhora(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val alertas = Repositorio.alertasPresupuesto()
            val pendientes = alertas.filter { it.sobrepasado || it.critico }
            if (pendientes.isEmpty()) return@launch
            pendientes.forEach { a ->
                val titulo = if (a.sobrepasado) "Presupuesto superado" else "Presupuesto casi agotado"
                val mensaje = "${a.nombre}: gastados ${a.gastado.formatearDinero()} de " +
                    "${a.limite.formatearDinero()} (${a.porcentaje.formatearPorcentaje()})"
                notificar(context, titulo, mensaje)
            }
        }
    }

    fun notificar(context: Context, titulo: String, mensaje: String) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val notificacion = NotificationCompat.Builder(context, CANAL_PRESUPUESTO)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setAutoCancel(true)
            .build()
        nm.notify(ID_UNICO, notificacion)
    }
}