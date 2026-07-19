package com.fenix.aprendiz

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cliente para el endpoint "fenix_chat_movil" del Space — Río de
 * Conversación. Mismo protocolo "call API" de Gradio que AltarApiClient
 * y GratitudApiClient, pero este endpoint recibe 5 parámetros y devuelve 4:
 *
 *   entrada:  mensaje, historial, voz_seleccionada, clave_usuario, nombre_jardin
 *   salida:   historial_out, txt_out, audio_out, historial_view_out
 *
 * Para cargar el historial sin enviar nada (al entrar a la pantalla), se
 * llama con mensaje="" — responder_movil detecta el mensaje vacío y
 * devuelve el historial sin tocar nada más.
 */
object RioApiClient {

    private const val SPACE_BASE = "https://cristobal299-fenix-aprendiz.hf.space"
    private const val SUFIJO_MAESTRO_MARCA = "\n\n_(maestro:"

    data class Mensaje(val role: String, val content: String)

    /** Envía un mensaje (o "" para solo cargar el historial actual). */
    fun enviar(
        mensaje: String,
        historialActual: List<Mensaje>,
        nombreJardin: String,
        onResultado: (historialActualizado: List<Mensaje>, historialParaEnviar: List<Mensaje>) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val historialJson = JSONArray()
                for (m in historialActual) {
                    historialJson.put(JSONObject().put("role", m.role).put("content", m.content))
                }

                val datos = JSONArray()
                    .put(mensaje)
                    .put(historialJson)
                    .put("") // voz_seleccionada: sin voz por ahora
                    .put("") // clave_usuario: pool compartido
                    .put(nombreJardin)

                val eventId = iniciarLlamada("fenix_chat_movil", datos)
                val resultado = leerResultado("fenix_chat_movil", eventId)
                if (resultado == null) {
                    onError("El Space no respondió a tiempo.")
                    return@Thread
                }

                // salida[0] = historial_out (para seguir la conversación),
                // salida[3] = historial_view_out (el río completo ya guardado en Supabase)
                val historialParaEnviar = parsearHistorial(resultado.opt(0))
                val historialVista = parsearHistorial(resultado.opt(3))

                onResultado(historialVista, historialParaEnviar)
            } catch (e: Exception) {
                onError(e.message ?: "Error de conexión con el Space")
            }
        }.start()
    }

    private fun parsearHistorial(valor: Any?): List<Mensaje> {
        val arr = when (valor) {
            is JSONArray -> valor
            is String -> try { JSONArray(valor) } catch (e: Exception) { JSONArray() }
            else -> JSONArray()
        }
        val lista = mutableListOf<Mensaje>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val role = item.optString("role", "assistant")
            var contenido = item.optString("content", "")
            if (role == "assistant") {
                contenido = contenido.substringBefore(SUFIJO_MAESTRO_MARCA).trim()
            }
            lista.add(Mensaje(role, contenido))
        }
        return lista
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
        con.readTimeout = 45000 // el ciclo completo (Cerebras + maestro + voz) tarda más que Altar/Gratitud

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
