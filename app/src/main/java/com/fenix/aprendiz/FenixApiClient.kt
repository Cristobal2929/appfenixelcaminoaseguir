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
 * La función `responder_movil(mensaje, historial, voz_seleccionada, clave_usuario, nombre_jardin)`
 * devuelve [chatbot, txt, audio_respuesta, historial_view].
 *
 * FIX FASE D: el `chatbot` de este Space usa el formato "messages" de
 * Gradio (cada turno es {"role": "user"|"assistant", "content": "..."}),
 * NO el formato viejo de pares [usuario, fenix]. El parseo de abajo
 * soporta ambos formatos por si el Space cambia de tipo en el futuro.
 * También se extrae la URL del audio de respuesta (audio_respuesta,
 * índice 2) para poder reproducirlo nativamente en el chat flotante.
 *
 * CONEXIÓN CON EL JARDÍN: este endpoint ("fenix_chat_movil") es distinto
 * del que usa la web del Space (que no tiene api_name explícito y no debe
 * tocarse desde aquí). Es exclusivo para la app y, si la persona tiene una
 * identidad guardada en el Jardín (Prefs.leerNombreJardin), Fénix recibe
 * su nombre y usa su lección actual + gratitudes recientes como contexto.
 */
object FenixApiClient {

    private const val SPACE_BASE = "https://cristobal299-fenix-aprendiz.hf.space"
    private const val API_NAME = "fenix_chat_movil"

    /**
     * Ejecuta en background. Llama a onResultado/onError en ese mismo hilo
     * (usa runOnUiThread en el callback si necesitas tocar la UI).
     *
     * onResultado recibe:
     *  - respuestaTexto: el texto de la última respuesta de Fénix
     *  - historialActualizado: el array "chatbot" tal cual lo devolvió el Space
     *  - audioUrl: URL reproducible del audio_respuesta generado (o null si
     *    el Space no devolvió audio para este turno)
     *  - categoria: la "lección"/tema que el maestro interno detectó para este
     *    turno (p.ej. "tema espiritual"), o null si el Space no incluyó la
     *    anotación "_(maestro: veredicto · categoria)_" en la respuesta.
     */
    fun enviarMensaje(
        mensaje: String,
        historialGradio: JSONArray,
        claveUsuario: String?,
        vozSeleccionada: String = "Anciano Sabio (Hombre Místico)",
        nombreJardin: String? = null,
        onResultado: (respuestaTexto: String, historialActualizado: JSONArray, audioUrl: String?, categoria: String?) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val eventId = iniciarLlamada(mensaje, historialGradio, claveUsuario, vozSeleccionada, nombreJardin)
                val resultado = leerResultado(eventId)
                if (resultado == null) {
                    onError("El Space no respondió a tiempo.")
                    return@Thread
                }

                val chatbot = resultado.optJSONArray(0) ?: JSONArray()
                val crudo = extraerUltimaRespuestaCruda(chatbot)
                val categoria = extraerCategoria(crudo)
                val ultimaRespuesta = limpiarTexto(crudo)
                val audioUrl = extraerAudioUrl(resultado.opt(2))

                onResultado(ultimaRespuesta, chatbot, audioUrl, categoria)
            } catch (e: Exception) {
                onError(e.message ?: "Error de conexión con el Space")
            }
        }.start()
    }

    /** Soporta formato "messages" ({"role","content"}) y el viejo de pares [usuario, fenix]. Sin limpiar. */
    private fun extraerUltimaRespuestaCruda(chatbot: JSONArray): String {
        if (chatbot.length() == 0) return "(sin respuesta)"

        // Formato "messages": recorre desde el final buscando el último role=assistant
        for (i in chatbot.length() - 1 downTo 0) {
            val item = chatbot.opt(i)
            if (item is JSONObject && item.optString("role") == "assistant") {
                return extraerTextoDeContenido(item.opt("content"))
            }
        }

        // Formato viejo de pares [usuario, fenix]
        val ultimo = chatbot.opt(chatbot.length() - 1)
        if (ultimo is JSONArray) {
            return extraerTextoDeContenido(ultimo.opt(1))
        }

        return "(sin respuesta)"
    }

    /**
     * Lee la anotación interna "_(maestro: veredicto · categoria)_" que
     * fenix_core.py deja pegada al final de la respuesta y devuelve solo la
     * categoría (la "lección"/tema de este turno), para mostrarla en el
     * subtítulo del chat. Devuelve null si la respuesta no trae esa anotación.
     */
    private fun extraerCategoria(textoCrudo: String): String? {
        val m = Regex("(?i)_?\\(\\s*maestro\\s*:\\s*[^·)]+·\\s*([^)]+)\\)_?").find(textoCrudo)
        val categoria = m?.groupValues?.getOrNull(1)?.trim()
        return if (categoria.isNullOrBlank()) null else categoria
    }

    /**
     * El campo "content" puede llegar como:
     *  - String plano: "hola"
     *  - Lista de bloques estilo OpenAI: [{"type":"text","text":"hola"}, ...]
     *  - Un solo objeto: {"type":"text","text":"hola"}
     * Aquí se extrae y concatena solo el texto real, sin volcar el JSON crudo.
     */
    private fun extraerTextoDeContenido(contenido: Any?): String {
        return when (contenido) {
            is String -> contenido
            is JSONArray -> {
                val partes = StringBuilder()
                for (i in 0 until contenido.length()) {
                    val bloque = contenido.opt(i)
                    val texto = when (bloque) {
                        is JSONObject -> bloque.optString("text", bloque.optString("content", ""))
                        is String -> bloque
                        else -> ""
                    }
                    if (texto.isNotBlank()) {
                        if (partes.isNotEmpty()) partes.append("\n")
                        partes.append(texto)
                    }
                }
                partes.toString()
            }
            is JSONObject -> contenido.optString("text", contenido.optString("content", ""))
            null -> ""
            else -> contenido.toString()
        }.ifBlank { "(sin respuesta)" }
    }

    /**
     * Parche cliente: quita anotaciones internas de control que a veces el
     * modelo "maestro" deja pegadas al final del texto, tipo
     * "_(maestro: mal · tema espiritual)_". Lo ideal es que fenix_core.py
     * nunca las incluya en el texto visible, pero mientras tanto se limpian
     * aquí para que no lleguen al usuario.
     */
    private fun limpiarTexto(texto: String): String {
        return texto
            .replace(Regex("(?i)_?\\(\\s*maestro\\s*:[^)]*\\)_?"), "")
            .trim()
            .ifBlank { "(sin respuesta)" }
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
        vozSeleccionada: String,
        nombreJardin: String?
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
        datos.put(claveUsuario ?: "")
        datos.put(nombreJardin ?: "")

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
