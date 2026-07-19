package com.fenix.aprendiz

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cliente mínimo para el endpoint de API "identidad_login" del Space,
 * separado por completo de FenixApiClient (que habla con "responder", el
 * chat 1-a-1 con Fénix). Este es el primer ladrillo del Jardín: registro
 * ligero de nombre + PIN de 4 dígitos, usado más adelante por el Templo de
 * Gratitud, el Círculo de Historias y el Río de Conversación.
 *
 * Mismo protocolo "call API" de Gradio que ya usa FenixApiClient:
 *   1) POST {space}/gradio_api/call/identidad_login  { "data": [nombre, pin] }
 *      -> { "event_id": "..." }
 *   2) GET  {space}/gradio_api/call/identidad_login/{event_id}
 *      -> stream de eventos "event: complete" + "data: ["<json>"]"
 *
 * El Space devuelve un único string con JSON dentro:
 *   {"ok": bool, "mensaje": str, "nombre_visible": str, "es_nuevo": bool}
 */
object IdentidadApiClient {

    private const val SPACE_BASE = "https://cristobal299-fenix-aprendiz.hf.space"
    private const val API_NAME = "identidad_login"

    data class ResultadoIdentidad(
        val ok: Boolean,
        val mensaje: String,
        val nombreVisible: String,
        val esNuevo: Boolean
    )

    /**
     * Ejecuta en background. Llama a onResultado/onError en ese mismo hilo
     * (usa runOnUiThread en el callback si necesitas tocar la UI).
     */
    fun entrarORegistrar(
        nombre: String,
        pin: String,
        onResultado: (ResultadoIdentidad) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val eventId = iniciarLlamada(nombre, pin)
                val resultado = leerResultado(eventId)
                if (resultado == null) {
                    onError("El Space no respondió a tiempo.")
                    return@Thread
                }
                val crudo = resultado.optString(0, "")
                val json = JSONObject(crudo)
                onResultado(
                    ResultadoIdentidad(
                        ok = json.optBoolean("ok", false),
                        mensaje = json.optString("mensaje", ""),
                        nombreVisible = json.optString("nombre_visible", nombre),
                        esNuevo = json.optBoolean("es_nuevo", false)
                    )
                )
            } catch (e: Exception) {
                onError(e.message ?: "Error de conexión con el Space")
            }
        }.start()
    }

    private fun iniciarLlamada(nombre: String, pin: String): String {
        val url = URL("$SPACE_BASE/gradio_api/call/$API_NAME")
        val con = url.openConnection() as HttpURLConnection
        con.requestMethod = "POST"
        con.doOutput = true
        con.setRequestProperty("Content-Type", "application/json")
        con.connectTimeout = 15000
        con.readTimeout = 15000

        val datos = JSONArray()
        datos.put(nombre)
        datos.put(pin)

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

    private fun leerResultado(eventId: String): JSONArray? {
        val url = URL("$SPACE_BASE/gradio_api/call/$API_NAME/$eventId")
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
