package com.fenix.aprendiz

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cliente del Space "Descifrador de Código Teshuá" (FastAPI, JSON plano —
 * NO Gradio). Dos rutas del main.py:
 *
 *   POST /api/descifrar   { "nombre", "fecha", "fondo":"" } -> { "respuesta": "<texto místico>" }
 *   POST /api/video_alma  { "nombre", "fecha", "fondo":"" } -> { "archivo": "<xxx.mp4>" }  (tarda ~1 min)
 *      El vídeo se sirve en  {BASE}/ver_videos/<archivo>
 *
 * `fecha` va en formato ISO "YYYY-MM-DD".
 * Todo corre en un hilo de fondo; los callbacks vuelven en ese hilo (usa
 * runOnUiThread en la Activity para tocar la UI).
 */
object DescifradorApiClient {

    private const val BASE = "https://cristobal299-desifrador-de-codigo-teshua.hf.space"

    /** Texto místico personalizado. Rápido (segundos). */
    fun descifrar(
        nombreCompleto: String,
        fechaIso: String,
        onOk: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val cuerpo = JSONObject()
                    .put("nombre", nombreCompleto)
                    .put("fecha", fechaIso)
                    .put("fondo", "")
                val resp = postJson("$BASE/api/descifrar", cuerpo.toString(), lecturaMs = 45000)
                val texto = JSONObject(resp).optString("respuesta", "").trim()
                if (texto.isEmpty()) onError("respuesta vacía") else onOk(texto)
            } catch (e: Exception) {
                onError(e.message ?: "error de conexión")
            }
        }.start()
    }

    /** Vídeo del alma. Lento (~1 min): timeout de lectura amplio. Devuelve la URL completa. */
    fun videoAlma(
        nombreCompleto: String,
        fechaIso: String,
        onOk: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val cuerpo = JSONObject()
                    .put("nombre", nombreCompleto)
                    .put("fecha", fechaIso)
                    .put("fondo", "")
                val resp = postJson("$BASE/api/video_alma", cuerpo.toString(), lecturaMs = 150000)
                val json = JSONObject(resp)
                val archivo = json.optString("archivo", "")
                val error = json.optString("error", "")
                when {
                    archivo.isNotEmpty() -> onOk("$BASE/ver_videos/$archivo")
                    error.isNotEmpty() -> onError(error)
                    else -> onError("sin archivo")
                }
            } catch (e: Exception) {
                onError(e.message ?: "error de conexión")
            }
        }.start()
    }

    private fun postJson(urlStr: String, body: String, lecturaMs: Int): String {
        val con = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 15000
            readTimeout = lecturaMs
        }
        con.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)); it.flush() }
        if (con.responseCode !in 200..299) {
            con.disconnect()
            throw RuntimeException("El Descifrador respondió ${con.responseCode}")
        }
        val texto = con.inputStream.bufferedReader().use { it.readText() }
        con.disconnect()
        return texto
    }
}
