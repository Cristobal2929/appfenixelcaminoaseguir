package com.fenix.aprendiz

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cliente mínimo para hablar con el endpoint de API de Gradio del Space de
 * Fénix Aprendiz, sin depender de ninguna librería externa (solo
 * HttpURLConnection + org.json, ambas ya incluidas en Android). Así no
 * tocamos build.gradle ni arriesgamos otro fallo de resolución de
 * dependencias como el de aapt2.
 *
 * Protocolo Gradio 4.x "call API":
 *   1) POST {space}/gradio_api/call/responder  { "data": [...] }
 *      -> { "event_id": "..." }
 *   2) GET  {space}/gradio_api/call/responder/{event_id}
 *      -> stream de eventos "event: complete" + "data: [...]"
 *
 * La función `responder(mensaje, historial, voz_seleccionada, clave_usuario)`
 * devuelve [chatbot, txt, audio_respuesta, historial_view]; nos interesa
 * chatbot (lista de pares [usuario, fenix]) para sacar la última respuesta.
 */
object FenixApiClient {

    private const val SPACE_BASE = "https://cristobal299-fenix-aprendiz.hf.space"
    private const val API_NAME = "responder"

    /** Ejecuta en background. Llama a onResultado/onError en ese mismo hilo. */
    fun enviarMensaje(
        mensaje: String,
        historialGradio: JSONArray,
        claveUsuario: String?,
        onResultado: (respuestaTexto: String, historialActualizado: JSONArray) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val eventId = iniciarLlamada(mensaje, historialGradio, claveUsuario)
                val resultado = leerResultado(eventId)
                if (resultado == null) {
                    onError("El Space no respondió a tiempo.")
                    return@Thread
                }
                val chatbot = resultado.optJSONArray(0) ?: JSONArray()
                val ultimaRespuesta = if (chatbot.length() > 0) {
                    val ultimoPar = chatbot.getJSONArray(chatbot.length() - 1)
                    ultimoPar.optString(1, "(sin respuesta)")
                } else "(sin respuesta)"
                onResultado(ultimaRespuesta, chatbot)
            } catch (e: Exception) {
                onError(e.message ?: "Error de conexión con el Space")
            }
        }.start()
    }

    private fun iniciarLlamada(mensaje: String, historial: JSONArray, claveUsuario: String?): String {
        val url = URL("$SPACE_BASE/gradio_api/call/$API_NAME")
        val con = url.openConnection() as HttpURLConnection
        con.requestMethod = "POST"
        con.doOutput = true
        con.setRequestProperty("Content-Type", "application/json")
        con.connectTimeout = 15000
        con.readTimeout = 15000

        val datos = JSONArray()
        datos.put(mensaje)
        datos.put(historial)
        datos.put("es-MX-DaliaNeural") // voz por defecto del Space
        if (claveUsuario.isNullOrBlank()) datos.put(JSONObject.NULL) else datos.put(claveUsuario)

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
        con.readTimeout = 60000

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
