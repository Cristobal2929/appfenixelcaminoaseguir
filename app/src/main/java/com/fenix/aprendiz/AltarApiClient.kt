package com.fenix.aprendiz

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cliente para los endpoints "altar_encender" y "altar_listar" del Space —
 * el Altar de la Luz del Jardín. Calcado de GratitudApiClient: mismo
 * protocolo "call API" de Gradio, cada uno con su propio api_name, sin
 * pisarse entre sí ni con el resto del Jardín.
 */
object AltarApiClient {

    private const val SPACE_BASE = "https://cristobal299-fenix-aprendiz.hf.space"

    data class Luz(val intencion: String, val creadaEn: String)

    /** Enciende una luz nueva con una intención para 'nombre'. onResultado recibe (ok, mensaje). */
    fun encender(
        nombre: String,
        intencion: String,
        onResultado: (ok: Boolean, mensaje: String) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val eventId = iniciarLlamada("altar_encender", JSONArray().put(nombre).put(intencion))
                val resultado = leerResultado("altar_encender", eventId)
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

    /** Lista las luces encendidas de 'nombre', más recientes primero. */
    fun listar(
        nombre: String,
        onResultado: (List<Luz>) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val eventId = iniciarLlamada("altar_listar", JSONArray().put(nombre))
                val resultado = leerResultado("altar_listar", eventId)
                if (resultado == null) {
                    onError("El Space no respondió a tiempo.")
                    return@Thread
                }
                val arr = JSONArray(resultado.optString(0, "[]"))
                val lista = mutableListOf<Luz>()
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    lista.add(Luz(item.optString("intencion", ""), item.optString("created_at", "")))
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
