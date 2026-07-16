package com.fenix.aprendiz

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cliente para los endpoints "circulo_publicar" y "circulo_listar" del
 * Space — el Círculo de Historias del Jardín. Mismo protocolo "call API"
 * de Gradio que GratitudApiClient / AltarApiClient.
 *
 * OJO — diferencia clave con el Templo y el Altar: el Círculo es COMUNAL.
 * Al listar NO se manda ningún nombre; el Space devuelve las historias de
 * todas las personas juntas, más recientes primero. Por eso cada historia
 * trae también 'nombre_visible': quién la compartió.
 */
object CirculoApiClient {

    private const val SPACE_BASE = "https://cristobal299-fenix-aprendiz.hf.space"

    data class Historia(val autor: String, val texto: String, val creadaEn: String)

    /** Publica una historia nueva a nombre de 'nombre'. onResultado recibe (ok, mensaje). */
    fun publicar(
        nombre: String,
        texto: String,
        onResultado: (ok: Boolean, mensaje: String) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val eventId = iniciarLlamada("circulo_publicar", JSONArray().put(nombre).put(texto))
                val resultado = leerResultado("circulo_publicar", eventId)
                if (resultado == null) {
                    onError("El Space no respondió a tiempo.")
                    return@Thread
                }
                val json = JSONObject(resultado.optString(0, "{}"))
                onResultado(json.optBoolean("ok", false), json.optString("mensaje", ""))
            } catch (e: Exception) {
                onError(e.message ?: "Error de conexión con el Space")
            }
        }.start()
    }

    /** Lista TODAS las historias del Círculo (comunal), más recientes primero. */
    fun listar(
        onResultado: (List<Historia>) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                // circulo_listar no recibe parámetros: array de datos vacío.
                val eventId = iniciarLlamada("circulo_listar", JSONArray())
                val resultado = leerResultado("circulo_listar", eventId)
                if (resultado == null) {
                    onError("El Space no respondió a tiempo.")
                    return@Thread
                }
                val arr = JSONArray(resultado.optString(0, "[]"))
                val lista = mutableListOf<Historia>()
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    lista.add(
                        Historia(
                            autor = item.optString("nombre_visible", "Alguien"),
                            texto = item.optString("texto", ""),
                            creadaEn = item.optString("created_at", "")
                        )
                    )
                }
                onResultado(lista)
            } catch (e: Exception) {
                onError(e.message ?: "Error de conexión con el Space")
            }
        }.start()
    }

    private fun iniciarLlamada(apiName: String, datos: JSONArray): String {
        val url = URL("$SPACE_BASE/gradio_api/call/$apiName")
        val con = url.openConnection() as HttpURLConnection
        con.requestMethod = "POST"
        con.doOutput = true
        con.setRequestProperty("Content-Type", "application/json")
        con.connectTimeout = 15000
        con.readTimeout = 15000

        val body = JSONObject().put("data", datos).toString()
        val os: OutputStream = con.outputStream
        os.write(body.toByteArray(Charsets.UTF_8))
        os.flush()
        os.close()

        if (con.responseCode !in 200..299) {
            throw RuntimeException("El Space respondió ${con.responseCode} al iniciar la llamada")
        }
        val resp = con.inputStream.bufferedReader().use { it.readText() }
        con.disconnect()
        return JSONObject(resp).getString("event_id")
    }

    private fun leerResultado(apiName: String, eventId: String): JSONArray? {
        val url = URL("$SPACE_BASE/gradio_api/call/$apiName/$eventId")
        val con = url.openConnection() as HttpURLConnection
        con.requestMethod = "GET"
        con.connectTimeout = 15000
        con.readTimeout = 30000

        val reader = BufferedReader(InputStreamReader(con.inputStream, Charsets.UTF_8))
        var linea: String?
        var esEventoCompleto = false
        try {
            while (reader.readLine().also { linea = it } != null) {
                val l = linea ?: continue
                when {
                    l.startsWith("event:") -> esEventoCompleto = l.contains("complete")
                    l.startsWith("data:") && esEventoCompleto -> {
                        val json = l.removePrefix("data:").trim()
                        return JSONArray(json)
                    }
                }
            }
        } finally {
            reader.close()
            con.disconnect()
        }
        return null
    }
}
