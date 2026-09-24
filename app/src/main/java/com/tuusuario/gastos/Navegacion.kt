package com.tuusuario.gastos

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tuusuario.gastos.ui.screens.PantallaAnadirGasto
import com.tuusuario.gastos.ui.screens.PantallaAnadirIngreso
import com.tuusuario.gastos.ui.screens.PantallaCategorias
import com.tuusuario.gastos.ui.screens.PantallaConfirmarTicket
import com.tuusuario.gastos.ui.screens.PantallaAjustes
import com.tuusuario.gastos.ui.screens.PantallaEscaner
import com.tuusuario.gastos.ui.screens.PantallaGrafica
import com.tuusuario.gastos.ui.screens.PantallaHexagono
import com.tuusuario.gastos.ui.screens.PantallaPresupuestos
import com.tuusuario.gastos.ui.screens.PantallaPrincipal
import com.tuusuario.gastos.ui.screens.PantallaReporte
import com.tuusuario.gastos.ui.screens.PantallaSuscripciones

object Rutas {
    const val PRINCIPAL = "principal"
    const val CATEGORIAS = "categorias"
    const val PRESUPUESTOS = "presupuestos"
    const val GRAFICA = "grafica"
    const val RADIAL = "radial"
    const val GASTO = "gasto"
    const val GASTO_FIJO = "gasto_fijo"
    const val INGRESO = "ingreso"
    const val ESCANER = "escaner"
    const val CONFIRMAR_TICKET = "confirmar_ticket"
    const val SUSCRIPCIONES = "suscripciones"
    const val REPORTE = "reporte"
    const val AJUSTES = "ajustes"
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Rutas.PRINCIPAL) {
        composable(Rutas.PRINCIPAL) {
            PantallaPrincipal(
                onAnadirGastoManual = { navController.navigate(Rutas.GASTO) },
                onEscanearTicket = { navController.navigate(Rutas.ESCANER) },
                onAnadirIngreso = { navController.navigate(Rutas.INGRESO) },
                onVerGrafica = { navController.navigate(Rutas.GRAFICA) },
                onCategorias = { navController.navigate(Rutas.CATEGORIAS) },
                onPresupuestos = { navController.navigate(Rutas.PRESUPUESTOS) },
                onSuscripciones = { navController.navigate(Rutas.SUSCRIPCIONES) },
                onReporte = { navController.navigate(Rutas.REPORTE) },
                onAjustes = { navController.navigate(Rutas.AJUSTES) },
            )
        }
        composable(Rutas.CATEGORIAS) {
            PantallaCategorias(onAtras = { navController.popBackStack() })
        }
        composable(Rutas.PRESUPUESTOS) {
            PantallaPresupuestos(onAtras = { navController.popBackStack() })
        }
        composable(Rutas.GRAFICA) {
            PantallaGrafica(
                onAtras = { navController.popBackStack() },
                onRadial = { navController.navigate(Rutas.RADIAL) },
            )
        }
        composable(Rutas.RADIAL) {
            PantallaHexagono(onAtras = { navController.popBackStack() })
        }
        composable(Rutas.GASTO) {
            PantallaAnadirGasto(esFijoInicial = false, onAtras = { navController.popBackStack() })
        }
        composable(Rutas.GASTO_FIJO) {
            PantallaAnadirGasto(esFijoInicial = true, onAtras = { navController.popBackStack() })
        }
        composable(Rutas.INGRESO) {
            PantallaAnadirIngreso(onAtras = { navController.popBackStack() })
        }
        composable(Rutas.ESCANER) {
            PantallaEscaner(
                onAtras = { navController.popBackStack() },
                onConfirmar = { navController.navigate(Rutas.CONFIRMAR_TICKET) },
            )
        }
        composable(Rutas.CONFIRMAR_TICKET) {
            PantallaConfirmarTicket(
                onGuardado = {
                    navController.popBackStack(Rutas.PRINCIPAL, inclusive = false)
                },
                onAtras = { navController.popBackStack() },
            )
        }
        composable(Rutas.SUSCRIPCIONES) {
            PantallaSuscripciones(
                onAtras = { navController.popBackStack() },
                onAnadirFijo = { navController.navigate(Rutas.GASTO_FIJO) },
            )
        }
        composable(Rutas.REPORTE) {
            PantallaReporte(onAtras = { navController.popBackStack() })
        }
        composable(Rutas.AJUSTES) {
            PantallaAjustes(onAtras = { navController.popBackStack() })
        }
    }
}