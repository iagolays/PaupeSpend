package com.tuusuario.gastos

import android.app.Application
import com.tuusuario.gastos.data.Repositorio
import com.tuusuario.gastos.util.Notificaciones
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GastosApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Repositorio.inicializar(this)
        Notificaciones.crearCanal(this)
        Notificaciones.programarComprobacion(this)
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            Repositorio.sembrarCategoriasIniciales()
        }
    }
}