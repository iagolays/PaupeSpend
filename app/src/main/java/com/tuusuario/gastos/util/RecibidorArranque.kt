package com.tuusuario.gastos.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RecibidorArranque : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Notificaciones.programarComprobacion(context)
    }
}