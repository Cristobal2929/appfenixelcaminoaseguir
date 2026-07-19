package com.fenix.aprendiz

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Guarda la clave personal de Cerebras SOLO en este dispositivo
 * (SharedPreferences local). Nunca se envía a ningún sitio salvo, si el
 * usuario la puso, al propio Space como parámetro `clave_usuario` de la
 * función `responder`, tal como ya soporta fenix_core.py.
 *
 * También guarda aquí, SOLO en este dispositivo, el historial de la
 * conversación en curso (tanto el formato "Gradio" que se manda al Space,
 * como los mensajes ya pintados en pantalla) para que el chat sobreviva a
 * cerrar la app, y la última "lección"/categoría detectada para mostrarla
 * en el subtítulo.
 */
object Prefs {
    private const val FICHERO = "teshua_prefs"
    private const val CLAVE_CEREBRAS = "clave_cerebras"
    private const val CLAVE_HISTORIAL_GRADIO = "historial_gradio"
    private const val CLAVE_MENSAJES_UI = "mensajes_ui"
    private const val CLAVE_ULTIMA_LECCION = "ultima_leccion"
    private const val CLAVE_JARDIN_NOMBRE = "jardin_nombre"
    private const val CLAVE_CASA_NUMERO = "casa_numero"
    private const val CLAVE_CASA_SEXO = "casa_sexo"
    private const val CLAVE_ULTIMA_MARCA_CIRCULO = "ultima_marca_circulo"

    /** Marca (fecha/autor+texto) de la última historia del Círculo ya vista, para notificar solo las nuevas. */
    fun guardarUltimaMarcaCirculo(ctx: Context, marca: String) {
        ctx.getSharedPreferences(FICHERO, Context.MODE_PRIVATE)
            .edit().putString(CLAVE_ULTIMA_MARCA_CIRCULO, marca).apply()
    }

    fun leerUltimaMarcaCirculo(ctx: Context): String? =
        ctx.getSharedPreferences(FICHERO, Context.MODE_PRIVATE)
            .getString(CLAVE_ULTIMA_MARCA_CIRCULO, null)

    fun guardarClaveCerebras(ctx: Context, clave: String) {
        ctx.getSharedPreferences(FICHERO, Context.MODE_PRIVATE)
            .edit().putString(CLAVE_CEREBRAS, clave).apply()
    }

    fun leerClaveCerebras(ctx: Context): String? {
        val v = ctx.getSharedPreferences(FICHERO, Context.MODE_PRIVATE)
            .getString(CLAVE_CEREBRAS, null)
        return if (v.isNullOrBlank()) null else v
    }

    fun olvidarClaveCerebras(ctx: Context) {
        ctx.getSharedPreferences(FICHERO, Context.MODE_PRIVATE)
            .edit().remove(CLAVE_CEREBRAS).apply()
    }

    /** Historial en formato Gradio (el que se manda de vuelta al Space en cada turno). */
    fun guardarHistorialGradio(ctx: Context, historial: JSONArray) {
        ctx.getSharedPreferences(FICHERO, Context.MODE_PRIVATE)
            .edit().putString(CLAVE_HISTORIAL_GRADIO, historial.toString()).apply()
    }

    fun leerHistorialGradio(ctx: Context): JSONArray {
        val v = ctx.getSharedPreferences(FICHERO, Context.MODE_PRIVATE)
            .getString(CLAVE_HISTORIAL_GRADIO, null) ?: return JSONArray()
        return try { JSONArray(v) } catch (e: Exception) { JSONArray() }
    }

    /** Mensajes ya pintados en el RecyclerView (burbujas usuario/Fénix). */
    fun guardarMensajesUI(ctx: Context, mensajes: List<Mensaje>) {
        val arr = JSONArray()
        for (m in mensajes) {
            arr.put(JSONObject().put("texto", m.texto).put("esUsuario", m.esUsuario))
        }
        ctx.getSharedPreferences(FICHERO, Context.MODE_PRIVATE)
            .edit().putString(CLAVE_MENSAJES_UI, arr.toString()).apply()
    }

    fun leerMensajesUI(ctx: Context): MutableList<Mensaje> {
        val lista = mutableListOf<Mensaje>()
        val v = ctx.getSharedPreferences(FICHERO, Context.MODE_PRIVATE)
            .getString(CLAVE_MENSAJES_UI, null) ?: return lista
        try {
            val arr = JSONArray(v)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                lista.add(Mensaje(o.getString("texto"), o.getBoolean("esUsuario")))
            }
        } catch (e: Exception) {
            // Historial corrupto o de una versión vieja: se ignora y se empieza limpio.
        }
        return lista
    }

    fun guardarUltimaLeccion(ctx: Context, leccion: String?) {
        ctx.getSharedPreferences(FICHERO, Context.MODE_PRIVATE)
            .edit().putString(CLAVE_ULTIMA_LECCION, leccion).apply()
    }

    fun leerUltimaLeccion(ctx: Context): String? {
        val v = ctx.getSharedPreferences(FICHERO, Context.MODE_PRIVATE)
            .getString(CLAVE_ULTIMA_LECCION, null)
        return if (v.isNullOrBlank()) null else v
    }

    /** Borra todo rastro de la conversación en curso (usado por "Reiniciar conversación"). */
    fun reiniciarConversacion(ctx: Context) {
        ctx.getSharedPreferences(FICHERO, Context.MODE_PRIVATE)
            .edit()
            .remove(CLAVE_HISTORIAL_GRADIO)
            .remove(CLAVE_MENSAJES_UI)
            .remove(CLAVE_ULTIMA_LECCION)
            .apply()
    }

    /**
     * Identidad ligera del Jardín (Templo de Gratitud, Círculo de Historias,
     * Río de Conversación). Solo se guarda el nombre en este dispositivo: el
     * PIN nunca se almacena localmente, solo se usa una vez para validarse
     * contra el Space (login) o para recuperar el perfil en un celular nuevo.
     */
    fun guardarNombreJardin(ctx: Context, nombre: String) {
        ctx.getSharedPreferences(FICHERO, Context.MODE_PRIVATE)
            .edit().putString(CLAVE_JARDIN_NOMBRE, nombre).apply()
    }

    fun leerNombreJardin(ctx: Context): String? {
        val v = ctx.getSharedPreferences(FICHERO, Context.MODE_PRIVATE)
            .getString(CLAVE_JARDIN_NOMBRE, null)
        return if (v.isNullOrBlank()) null else v
    }

    fun salirDelJardin(ctx: Context) {
        ctx.getSharedPreferences(FICHERO, Context.MODE_PRIVATE)
            .edit().remove(CLAVE_JARDIN_NOMBRE).apply()
    }

    /**
     * Casa (tribu) revelada en el Portal de Teshuá. Se guarda el número del
     * alma (numeroEsencia, 1-9/11/22/33) y el sexo ("h"/"m") para el saludo.
     * Si hay casa guardada, el Portal no se vuelve a mostrar al arrancar.
     */
    fun guardarCasa(ctx: Context, numeroAlma: Int, sexo: String) {
        ctx.getSharedPreferences(FICHERO, Context.MODE_PRIVATE)
            .edit()
            .putInt(CLAVE_CASA_NUMERO, numeroAlma)
            .putString(CLAVE_CASA_SEXO, sexo)
            .apply()
    }

    /** null si aún no ha pasado por el Portal. */
    fun leerCasaNumero(ctx: Context): Int? {
        val p = ctx.getSharedPreferences(FICHERO, Context.MODE_PRIVATE)
        return if (p.contains(CLAVE_CASA_NUMERO)) p.getInt(CLAVE_CASA_NUMERO, -1) else null
    }

    fun leerSexoCasa(ctx: Context): String =
        ctx.getSharedPreferences(FICHERO, Context.MODE_PRIVATE)
            .getString(CLAVE_CASA_SEXO, "h") ?: "h"

    fun olvidarCasa(ctx: Context) {
        ctx.getSharedPreferences(FICHERO, Context.MODE_PRIVATE)
            .edit().remove(CLAVE_CASA_NUMERO).remove(CLAVE_CASA_SEXO).apply()
    }

    // ── Historial de conversaciones (varias, guardadas SOLO en este dispositivo) ──
    private const val CLAVE_CONVERSACIONES = "conversaciones"
    private const val CLAVE_CONV_ACTUAL = "conv_actual_id"

    /** Resumen para la lista del cajón izquierdo: id, título y fecha. */
    data class ResumenConversacion(val id: String, val titulo: String, val ts: Long)

    /** Conversación completa, al abrirla desde el historial. */
    data class ConversacionGuardada(
        val mensajes: MutableList<Mensaje>,
        val historial: JSONArray,
        val leccion: String?
    )

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(FICHERO, Context.MODE_PRIVATE)

    private fun leerArray(ctx: Context, clave: String): JSONArray {
        val v = prefs(ctx).getString(clave, null) ?: return JSONArray()
        return try { JSONArray(v) } catch (e: Exception) { JSONArray() }
    }

    /** Id de la conversación en curso; si no hay ninguna, crea una. */
    fun idConversacionActual(ctx: Context): String {
        val p = prefs(ctx)
        var id = p.getString(CLAVE_CONV_ACTUAL, null)
        if (id.isNullOrBlank()) {
            id = "c" + System.currentTimeMillis()
            p.edit().putString(CLAVE_CONV_ACTUAL, id).apply()
        }
        return id
    }

    /** Arranca una conversación nueva y vacía (rota el id y limpia los slots en curso). */
    fun nuevaConversacion(ctx: Context) {
        prefs(ctx).edit()
            .putString(CLAVE_CONV_ACTUAL, "c" + System.currentTimeMillis())
            .remove(CLAVE_HISTORIAL_GRADIO)
            .remove(CLAVE_MENSAJES_UI)
            .remove(CLAVE_ULTIMA_LECCION)
            .apply()
    }

    /** Lista de conversaciones guardadas, la más reciente primero. */
    fun listarConversaciones(ctx: Context): List<ResumenConversacion> {
        val arr = leerArray(ctx, CLAVE_CONVERSACIONES)
        val out = mutableListOf<ResumenConversacion>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out.add(ResumenConversacion(o.optString("id"), o.optString("titulo"), o.optLong("ts")))
        }
        out.sortByDescending { it.ts }
        return out
    }

    /**
     * Guarda (o actualiza) la conversación en curso dentro de la lista del
     * historial, con su id actual. El título es el primer mensaje del usuario.
     * No archiva conversaciones vacías.
     */
    fun archivarConversacionActual(
        ctx: Context,
        mensajes: List<Mensaje>,
        historial: JSONArray,
        leccion: String?
    ) {
        if (mensajes.isEmpty()) return
        val id = idConversacionActual(ctx)
        val titulo = (mensajes.firstOrNull { it.esUsuario }?.texto ?: "Conversación")
            .replace("\n", " ").trim().take(48).ifBlank { "Conversación" }
        val mensajesArr = JSONArray()
        for (m in mensajes) {
            mensajesArr.put(JSONObject().put("texto", m.texto).put("esUsuario", m.esUsuario))
        }
        val obj = JSONObject()
            .put("id", id)
            .put("titulo", titulo)
            .put("ts", System.currentTimeMillis())
            .put("mensajes", mensajesArr)
            .put("historial", historial.toString())
            .put("leccion", leccion ?: "")

        val arr = leerArray(ctx, CLAVE_CONVERSACIONES)
        var reemplazado = false
        for (i in 0 until arr.length()) {
            if (arr.optJSONObject(i)?.optString("id") == id) {
                arr.put(i, obj); reemplazado = true; break
            }
        }
        if (!reemplazado) arr.put(obj)
        prefs(ctx).edit().putString(CLAVE_CONVERSACIONES, arr.toString()).apply()
    }

    /** Abre una conversación guardada: la vuelca a los slots "en curso" y la deja como actual. */
    fun abrirConversacion(ctx: Context, id: String): ConversacionGuardada? {
        val arr = leerArray(ctx, CLAVE_CONVERSACIONES)
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (o.optString("id") != id) continue
            val ma = o.optJSONArray("mensajes") ?: JSONArray()
            val msgs = mutableListOf<Mensaje>()
            for (j in 0 until ma.length()) {
                val mo = ma.optJSONObject(j) ?: continue
                msgs.add(Mensaje(mo.optString("texto"), mo.optBoolean("esUsuario")))
            }
            val hist = try { JSONArray(o.optString("historial", "[]")) } catch (e: Exception) { JSONArray() }
            val lec = o.optString("leccion", "").ifBlank { null }
            prefs(ctx).edit()
                .putString(CLAVE_CONV_ACTUAL, id)
                .putString(CLAVE_MENSAJES_UI, ma.toString())
                .putString(CLAVE_HISTORIAL_GRADIO, hist.toString())
                .putString(CLAVE_ULTIMA_LECCION, lec)
                .apply()
            return ConversacionGuardada(msgs, hist, lec)
        }
        return null
    }

    /** Borra una conversación del historial. */
    fun borrarConversacion(ctx: Context, id: String) {
        val arr = leerArray(ctx, CLAVE_CONVERSACIONES)
        val nuevo = JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (o.optString("id") != id) nuevo.put(o)
        }
        prefs(ctx).edit().putString(CLAVE_CONVERSACIONES, nuevo.toString()).apply()
    }
}
