package com.tuusuario.gastos.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RecibidorPresupuesto : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Notificaciones.comprobarAhora(context)
        Notificaciones.programarComprobacion(context)
    }
}