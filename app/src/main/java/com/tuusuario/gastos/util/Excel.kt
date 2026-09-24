package com.tuusuario.gastos.util

import android.content.Context
import android.net.Uri
import android.util.Xml
import com.tuusuario.gastos.data.Categoria
import com.tuusuario.gastos.data.Gasto
import com.tuusuario.gastos.data.Ingreso
import com.tuusuario.gastos.data.MetodoPago
import com.tuusuario.gastos.data.Presupuesto
import com.tuusuario.gastos.data.Repositorio
import com.tuusuario.gastos.data.Suscripcion
import kotlinx.coroutines.flow.first
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Exportacion/importacion a Excel (solo lectura/escritura del formato
 * .xlsx minimo: un Zip de documentos XML). No depende de Apache POI,
 * lo que lo hace ligero y evita el problema clasico de POI en Android.
 */
object Excel {
    const val MIME =
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    sealed class Celda {
        data class Texto(val valor: String) : Celda()
        data class Numero(val valor: Double) : Celda()
    }

    // ------------------------------------------------------------------
    // Exportar
    // ------------------------------------------------------------------

    suspend fun exportar(context: Context, uri: Uri) {
        val categorias = Repositorio.categorias.observarTodas().first()
        val gastos = Repositorio.gastos.observarTodos().first()
        val ingresos = Repositorio.ingresos.observarTodos().first()
        val suscripciones = Repositorio.suscripciones.observarActivas().first()
        val presupuestos = Repositorio.presupuestos.observarTodas().first()

        val nombreCategoria = categorias.associate { it.id to it.nombre }

        val tablas = listOf(
            Triple(
                "Categorias",
                listOf("Nombre", "Icono", "Color", "LimiteMensual"),
                categorias.map { c ->
                    listOf(
                        Celda.Texto(c.nombre),
                        Celda.Texto(c.icono),
                        Celda.Texto(c.colorHex),
                        c.limiteMensual?.let { Celda.Numero(it) } ?: Celda.Texto(""),
                    )
                }
            ),
            Triple(
                "Gastos",
                listOf("Descripcion", "Monto", "Fecha", "Metodo", "Categoria", "Fijo", "RutaTicket"),
                gastos.map { g ->
                    listOf(
                        Celda.Texto(g.descripcion),
                        Celda.Numero(g.monto),
                        Celda.Texto(g.fecha.toString()),
                        Celda.Texto(g.metodoPago.name),
                        Celda.Texto(g.categoriaId?.let { nombreCategoria[it] } ?: ""),
                        Celda.Texto(if (g.esFijo) "si" else "no"),
                        Celda.Texto(g.rutaTicket ?: ""),
                    )
                }
            ),
            Triple(
                "Ingresos",
                listOf("Descripcion", "Monto", "Fecha", "Recurrente"),
                ingresos.map { i ->
                    listOf(
                        Celda.Texto(i.descripcion),
                        Celda.Numero(i.monto),
                        Celda.Texto(i.fecha.toString()),
                        Celda.Texto(if (i.esRecurrente) "si" else "no"),
                    )
                }
            ),
            Triple(
                "Suscripciones",
                listOf("Nombre", "Monto", "DiaDeCobro", "Categoria", "Activa"),
                suscripciones.map { s ->
                    listOf(
                        Celda.Texto(s.nombre),
                        Celda.Numero(s.monto),
                        Celda.Numero(s.diaDeCobro.toDouble()),
                        Celda.Texto(s.categoriaId?.let { nombreCategoria[it] } ?: ""),
                        Celda.Texto(if (s.activa) "si" else "no"),
                    )
                }
            ),
            Triple(
                "Presupuestos",
                listOf("Categoria", "Limite", "Mes", "Anio"),
                presupuestos.map { p ->
                    listOf(
                        Celda.Texto(p.categoriaId?.let { nombreCategoria[it] } ?: "General"),
                        Celda.Numero(p.limite),
                        Celda.Numero(p.mes.toDouble()),
                        Celda.Numero(p.anio.toDouble()),
                    )
                }
            ),
        )

        val bytes = generarXlsx(tablas)
        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
    }

    // ------------------------------------------------------------------
    // Generador de .xlsx (sin dependencias)
    // ------------------------------------------------------------------

    private fun generarXlsx(tablas: List<Triple<String, List<String>, List<List<Celda>>>>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            fun entrada(nombre: String, contenido: String) {
                zip.putNextEntry(ZipEntry(nombre))
                zip.write(contenido.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }

            entrada("[Content_Types].xml", contentTypes(tablas.size))
            entrada("_rels/.rels", RELS_RAIZ)
            entrada("xl/workbook.xml", workbookXml(tablas))
            entrada("xl/_rels/workbook.xml.rels", workbookRels(tablas.size))
            entrada("xl/styles.xml", ESTILOS)

            tablas.forEachIndexed { i, (_, cabeceras, filas) ->
                entrada("xl/worksheets/sheet${i + 1}.xml", hojaXml(cabeceras, filas))
            }
        }
        return baos.toByteArray()
    }

    private fun escapar(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun refCol(i: Int): String {
        var n = i
        var s = ""
        while (n >= 0) {
            s = ('A' + (n % 26)) + s
            n = n / 26 - 1
        }
        return s
    }

    private fun hojaXml(cabeceras: List<String>, filas: List<List<Celda>>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>")

        fun escribirFila(fila: List<Celda>, indiceFila: Int) {
            sb.append("<row r=\"").append(indiceFila).append("\">")
            fila.forEachIndexed { col, celda ->
                val ref = refCol(col) + indiceFila
                when (celda) {
                    is Celda.Texto -> {
                        sb.append("<c r=\"").append(ref).append("\" t=\"inlineStr\">")
                            .append("<is><t>").append(escapar(celda.valor)).append("</t></is></c>")
                    }
                    is Celda.Numero -> {
                        sb.append("<c r=\"").append(ref).append("\"><v>")
                            .append(celda.valor).append("</v></c>")
                    }
                }
            }
            sb.append("</row>")
        }

        escribirFila(cabeceras.map { Celda.Texto(it) }, 1)
        filas.forEachIndexed { i, fila -> escribirFila(fila, i + 2) }

        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun contentTypes(n: Int): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        sb.append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
        sb.append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
        sb.append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
        sb.append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
        for (i in 1..n) {
            sb.append("<Override PartName=\"/xl/worksheets/sheet$i.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>")
        }
        sb.append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>")
        sb.append("</Types>")
        return sb.toString()
    }

    private fun workbookXml(tablas: List<Triple<String, List<String>, List<List<Celda>>>>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        sb.append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
        sb.append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>")
        tablas.forEachIndexed { i, (nombre, _, _) ->
            sb.append("<sheet name=\"").append(escapar(nombre)).append("\" sheetId=\"")
                .append(i + 1).append("\" r:id=\"rId").append(i + 1).append("\"/>")
        }
        sb.append("</sheets></workbook>")
        return sb.toString()
    }

    private fun workbookRels(n: Int): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        sb.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">")
        for (i in 1..n) {
            sb.append("<Relationship Id=\"rId$i\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet$i.xml\"/>")
        }
        sb.append("</Relationships>")
        return sb.toString()
    }

    private val RELS_RAIZ = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
        "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
        "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
        "</Relationships>"

    private val ESTILOS = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
        "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +
        "<fonts count=\"1\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>" +
        "<fills count=\"1\"><fill><patternFill patternType=\"none\"/></fill></fills>" +
        "<borders count=\"1\"><border/></borders>" +
        "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
        "<cellXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/></cellXfs>" +
        "</styleSheet>"

    // ------------------------------------------------------------------
    // Importar
    // ------------------------------------------------------------------

    private fun stringCelda(c: Celda): String = when (c) {
        is Celda.Texto -> c.valor
        is Celda.Numero -> "%.0f".format(c.valor)
    }

    private fun numeroCelda(c: Celda): Double? = when (c) {
        is Celda.Numero -> c.valor
        is Celda.Texto -> c.valor.replace(",", ".").toDoubleOrNull()
    }

    private data class HojaLeida(val nombre: String, val filas: List<List<Celda>>)

    suspend fun importar(context: Context, uri: Uri): Int {
        val input = context.contentResolver.openInputStream(uri) ?: return 0
        val hojas = leerXlsx(input)
        var contados = 0

        // Categorias primero: hay que mapear nombres a ids.
        val categoriasExistentes = Repositorio.categorias.observarTodas().first().toMutableList()
        suspend fun idCategoria(nombre: String?): Long? {
            if (nombre.isNullOrBlank() || nombre.equals("General", true)) return null
            return categoriasExistentes.firstOrNull { it.nombre.equals(nombre, true) }?.id
                ?: Repositorio.categorias.insertar(Categoria(nombre = nombre.trim(), icono = "\uD83D\uDCE6", colorHex = "#6C5CE7"))
                    .also { nueva ->
                        categoriasExistentes.add(Categoria(id = nueva, nombre = nombre.trim(), icono = "\uD83D\uDCE6", colorHex = "#6C5CE7"))
                    }
        }

        for (hoja in hojas) {
            if (hoja.filas.size < 2) continue
            val cabeceras = hoja.filas.first().map { stringCelda(it).trim().lowercase() }
            fun filaMapa(fila: List<Celda>): Map<String, String> =
                cabeceras.mapIndexedNotNull { i, h -> if (h.isNotBlank()) h to stringCelda(fila.getOrNull(i) ?: Celda.Texto("")) else null }.toMap()

            when (hoja.nombre.trim().lowercase()) {
                "gastos" -> {
                    for (i in 1 until hoja.filas.size) {
                        val m = filaMapa(hoja.filas[i])
                        val monto = m["monto"]?.replace(",", ".")?.toDoubleOrNull() ?: continue
                        val fecha = try { LocalDate.parse(m["fecha"]) } catch (e: Exception) { continue }
                        Repositorio.gastos.insertar(
                            Gasto(
                                descripcion = m["descripcion"] ?: "Gasto",
                                monto = monto,
                                fecha = fecha,
                                metodoPago = runCatching { MetodoPago.valueOf(m["metodo"] ?: "") }.getOrDefault(MetodoPago.TARJETA),
                                categoriaId = idCategoria(m["categoria"]),
                                esFijo = m["fijo"]?.equals("si", true) == true,
                                rutaTicket = m["rutaticket"]?.takeIf { it.isNotBlank() },
                            )
                        )
                        contados++
                    }
                }
                "ingresos" -> {
                    for (i in 1 until hoja.filas.size) {
                        val m = filaMapa(hoja.filas[i])
                        val monto = m["monto"]?.replace(",", ".")?.toDoubleOrNull() ?: continue
                        val fecha = try { LocalDate.parse(m["fecha"]) } catch (e: Exception) { continue }
                        Repositorio.ingresos.insertar(
                            Ingreso(
                                descripcion = m["descripcion"] ?: "Ingreso",
                                monto = monto,
                                fecha = fecha,
                                esRecurrente = m["recurrente"]?.equals("si", true) == true,
                            )
                        )
                        contados++
                    }
                }
                "suscripciones" -> {
                    for (i in 1 until hoja.filas.size) {
                        val m = filaMapa(hoja.filas[i])
                        val monto = m["monto"]?.replace(",", ".")?.toDoubleOrNull() ?: continue
                        val dia = m["diadecobro"]?.toDoubleOrNull()?.toInt() ?: continue
                        Repositorio.suscripciones.insertar(
                            Suscripcion(
                                nombre = m["nombre"] ?: "Suscripcion",
                                monto = monto,
                                diaDeCobro = dia,
                                categoriaId = idCategoria(m["categoria"]),
                                activa = m["activa"]?.equals("si", true) ?: true,
                            )
                        )
                        contados++
                    }
                }
                "presupuestos" -> {
                    for (i in 1 until hoja.filas.size) {
                        val m = filaMapa(hoja.filas[i])
                        val limite = m["limite"]?.replace(",", ".")?.toDoubleOrNull() ?: continue
                        val mes = m["mes"]?.toDoubleOrNull()?.toInt() ?: continue
                        val anio = m["anio"]?.toDoubleOrNull()?.toInt() ?: continue
                        Repositorio.presupuestos.insertar(
                            Presupuesto(
                                categoriaId = idCategoria(m["categoria"]),
                                limite = limite,
                                mes = mes,
                                anio = anio,
                            )
                        )
                        contados++
                    }
                }
                "categorias" -> {
                    for (i in 1 until hoja.filas.size) {
                        val m = filaMapa(hoja.filas[i])
                        val nombre = m["nombre"]?.trim() ?: continue
                        if (nombre.isBlank()) continue
                        if (categoriasExistentes.any { it.nombre.equals(nombre, true) }) continue
                        Repositorio.categorias.insertar(
                            Categoria(
                                nombre = nombre,
                                icono = m["icono"]?.takeIf { it.isNotBlank() } ?: "\uD83D\uDCE6",
                                colorHex = m["color"]?.takeIf { it.startsWith("#") } ?: "#6C5CE7",
                                limiteMensual = m["limitemensual"]?.replace(",", ".")?.toDoubleOrNull(),
                            )
                        )
                        contados++
                    }
                }
            }
        }
        return contados
    }

    private fun leerXlsx(input: java.io.InputStream): List<HojaLeida> {
        var sharedStrings = listOf<String>()
        val rels: MutableMap<String, String> = mutableMapOf()
        val sheetsConNombre: MutableList<Pair<String, String>> = mutableListOf() // (nombre, ruta)
        val hojaConNombre: MutableList<HojaLeida> = mutableListOf()
        val hojaDatos: MutableMap<String, List<List<Celda>>> = mutableMapOf()

        ZipInputStream(input).use { zip ->
            var entrada = zip.nextEntry
            while (entrada != null) {
                val nombre = entrada.name
                when {
                    nombre == "xl/sharedStrings.xml" -> sharedStrings = leerSharedStrings(zip)
                    nombre == "xl/workbook.xml" -> sheetsConNombre.addAll(leerWorkbook(zip))
                    nombre == "xl/_rels/workbook.xml.rels" -> rels.putAll(leerRels(zip))
                    nombre.startsWith("xl/worksheets/") && nombre.endsWith(".xml") ->
                        hojaDatos[nombre.removePrefix("xl/")] = leerHoja(zip, sharedStrings)
                }
                zip.closeEntry()
                entrada = zip.nextEntry
            }
        }

        for ((nombreHoja, rId) in sheetsConNombre) {
            val ruta = rels[rId]?.takeIf { !it.startsWith("/") } ?: "worksheets/$rId"
            val clave = ruta.removePrefix("xl/").removePrefix("./")
            val filas = hojaDatos[clave] ?: hojaDatos["worksheets/${clave.substringAfter("worksheets/")}"] ?: continue
            hojaConNombre.add(HojaLeida(nombreHoja, filas))
        }
        return hojaConNombre
    }

    private fun leerSharedStrings(stream: java.io.InputStream): List<String> {
        val lista = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setInput(stream, null)
        var act = ""
        var enT = false
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> if (parser.name == "t") enT = true
                XmlPullParser.TEXT -> if (enT) act += parser.text
                XmlPullParser.END_TAG -> if (parser.name == "si") { lista.add(act); act = ""; enT = false }
            }
            parser.next()
        }
        return lista
    }

    private fun leerWorkbook(stream: java.io.InputStream): List<Pair<String, String>> {
        val lista = mutableListOf<Pair<String, String>>()
        val parser = Xml.newPullParser()
        parser.setInput(stream, null)
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "sheet") {
                val nombre = parser.getAttributeValue(null, "name") ?: ""
                val rid = parser.getAttributeValue("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id")
                    ?: parser.getAttributeValue(2)
                if (rid != null) lista.add(nombre to rid)
            }
            parser.next()
        }
        return lista
    }

    private fun leerRels(stream: java.io.InputStream): Map<String, String> {
        val mapa = mutableMapOf<String, String>()
        val parser = Xml.newPullParser()
        parser.setInput(stream, null)
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "Relationship") {
                val id = parser.getAttributeValue(null, "Id") ?: ""
                val target = parser.getAttributeValue(null, "Target") ?: ""
                if (target.contains("sheet")) mapa[id] = target
            }
            parser.next()
        }
        return mapa
    }

    private fun leerHoja(stream: java.io.InputStream, sharedStrings: List<String>): List<List<Celda>> {
        val filas = mutableListOf<List<Celda>>()
        val parser = Xml.newPullParser()
        parser.setInput(stream, null)

        var filaActual = mutableListOf<Celda>()
        var celdaActual: StringBuilder? = null
        var tipoActual = ""
        var enCelda = false
        var enV = false

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> filaActual = mutableListOf()
                    "c" -> {
                        enCelda = true
                        tipoActual = parser.getAttributeValue(null, "t") ?: ""
                        celdaActual = StringBuilder()
                    }
                    "v" -> enV = true
                }
                XmlPullParser.TEXT -> if (enV || (enCelda && tipoActual == "inlineStr")) {
                    if (celdaActual != null) celdaActual.append(parser.text)
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "v" -> enV = false
                    "c" -> {
                        if (celdaActual != null) {
                            val valor = celdaActual.toString()
                            filaActual.add(
                                when {
                                    tipoActual == "inlineStr" || tipoActual == "str" -> Celda.Texto(valor)
                                    tipoActual == "s" -> Celda.Texto(
                                        sharedStrings.getOrElse(valor.toIntOrNull() ?: -1) { "" }
                                    )
                                    else -> valor.toDoubleOrNull()?.let { Celda.Numero(it) } ?: Celda.Texto("")
                                }
                            )
                        }
                        enCelda = false
                        celdaActual = null
                    }
                    "row" -> if (filaActual.isNotEmpty()) filas.add(filaActual)
                }
            }
            parser.next()
        }
        return filas
    }
}