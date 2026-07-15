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
 * Protocolo Gradio "call API":
 *   1) POST {space}/gradio_api/call/responder  { "data": [...] }
 *      -> { "event_id": "..." }
 *   2) GET  {space}/gradio_api/call/responder/{event_id}
 *      -> stream de eventos "event: complete" + "data: [...]"
 *
 * La función `responder(mensaje, historial, voz_seleccionada, clave_usuario)`
 * devuelve [chatbot, txt, audio_respuesta, historial_view].
 *
 * FIX FASE D: el `chatbot` de este Space usa el formato "messages" de
 * Gradio (cada turno es {"role": "user"|"assistant", "content": "..."}),
 * NO el formato viejo de pares [usuario, fenix]. El parseo de abajo
 * soporta ambos formatos por si el Space cambia de tipo en el futuro.
 * También se extrae la URL del audio de respuesta (audio_respuesta,
 * índice 2) para poder reproducirlo nativamente en el chat flotante.
 */
object FenixApiClient {

    private const val SPACE_BASE = "https://cristobal299-fenix-aprendiz.hf.space"
    private const val API_NAME = "responder"

    /**
     * Ejecuta en background. Llama a onResultado/onError en ese mismo hilo
     * (usa runOnUiThread en el callback si necesitas tocar la UI).
     *
     * onResultado recibe:
     *  - respuestaTexto: el texto de la última respuesta de Fénix
     *  - historialActualizado: el array "chatbot" tal cual lo devolvió el Space
     *  - audioUrl: URL reproducible del audio_respuesta generado (o null si
     *    el Space no devolvió audio para este turno)
     */
    fun enviarMensaje(
        mensaje: String,
        historialGradio: JSONArray,
        claveUsuario: String?,
        vozSeleccionada: String = "Anciano Sabio (Hombre Místico)",
        onResultado: (respuestaTexto: String, historialActualizado: JSONArray, audioUrl: String?) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val eventId = iniciarLlamada(mensaje, historialGradio, claveUsuario, vozSeleccionada)
                val resultado = leerResultado(eventId)
                if (resultado == null) {
                    onError("El Space no respondió a tiempo.")
                    return@Thread
                }

                val chatbot = resultado.optJSONArray(0) ?: JSONArray()
                val ultimaRespuesta = extraerUltimaRespuesta(chatbot)
                val audioUrl = extraerAudioUrl(resultado.opt(2))

                onResultado(ultimaRespuesta, chatbot, audioUrl)
            } catch (e: Exception) {
                onError(e.message ?: "Error de conexión con el Space")
            }
        }.start()
    }

    /** Soporta formato "messages" ({"role","content"}) y el viejo de pares [usuario, fenix]. */
    private fun extraerUltimaRespuesta(chatbot: JSONArray): String {
        if (chatbot.length() == 0) return "(sin respuesta)"

        // Formato "messages": recorre desde el final buscando el último role=assistant
        for (i in chatbot.length() - 1 downTo 0) {
            val item = chatbot.opt(i)
            if (item is JSONObject && item.optString("role") == "assistant") {
                return item.optString("content", "(sin respuesta)")
            }
        }

        // Formato viejo de pares [usuario, fenix]
        val ultimo = chatbot.opt(chatbot.length() - 1)
        if (ultimo is JSONArray) {
            return ultimo.optString(1, "(sin respuesta)")
        }

        return "(sin respuesta)"
    }

    /** El componente gr.Audio llega como FileData: {"path":..,"url":..,"meta":{...}} o null. */
    private fun extraerAudioUrl(valor: Any?): String? {
        if (valor == null || valor == JSONObject.NULL) return null
        val obj = valor as? JSONObject ?: return null
        val url = obj.optString("url", "")
        if (url.isNotBlank()) return url
        // Algunas versiones de Gradio solo mandan "path" (relativo al Space)
        val path = obj.optString("path", "")
        if (path.isNotBlank()) return "$SPACE_BASE/file=$path"
        return null
    }

    private fun iniciarLlamada(
        mensaje: String,
        historial: JSONArray,
        claveUsuario: String?,
        vozSeleccionada: String
    ): String {
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
        datos.put(vozSeleccionada)
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
