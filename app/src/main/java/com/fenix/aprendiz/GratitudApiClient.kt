package com.fenix.aprendiz

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cliente para los endpoints "gratitud_guardar" y "gratitud_listar" del
 * Space — el Templo de Gratitud del Jardín. Mismo protocolo "call API" de
 * Gradio que IdentidadApiClient y FenixApiClient, cada uno hablando con su
 * propio api_name, sin pisarse entre sí.
 */
object GratitudApiClient {

    private const val SPACE_BASE = "https://cristobal299-fenix-aprendiz.hf.space"

    data class Gratitud(val texto: String, val creadaEn: String)

    /** Guarda una gratitud nueva para 'nombre'. onResultado recibe (ok, mensaje). */
    fun guardar(
        nombre: String,
        texto: String,
        onResultado: (ok: Boolean, mensaje: String) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val eventId = iniciarLlamada("gratitud_guardar", JSONArray().put(nombre).put(texto))
                val resultado = leerResultado("gratitud_guardar", eventId)
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

    /** Lista las gratitudes guardadas de 'nombre', más recientes primero. */
    fun listar(
        nombre: String,
        onResultado: (List<Gratitud>) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val eventId = iniciarLlamada("gratitud_listar", JSONArray().put(nombre))
                val resultado = leerResultado("gratitud_listar", eventId)
                if (resultado == null) {
                    onError("El Space no respondió a tiempo.")
                    return@Thread
                }
                val arr = JSONArray(resultado.optString(0, "[]"))
                val lista = mutableListOf<Gratitud>()
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    lista.add(Gratitud(item.optString("texto", ""), item.optString("created_at", "")))
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
