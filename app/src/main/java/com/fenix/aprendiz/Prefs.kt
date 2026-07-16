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
}
