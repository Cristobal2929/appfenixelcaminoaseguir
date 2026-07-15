package com.fenix.aprendiz

import android.content.Context

/**
 * Guarda la clave personal de Cerebras SOLO en este dispositivo
 * (SharedPreferences local). Nunca se envía a ningún sitio salvo, si el
 * usuario la puso, al propio Space como parámetro `clave_usuario` de la
 * función `responder`, tal como ya soporta fenix_core.py.
 */
object Prefs {
    private const val FICHERO = "teshua_prefs"
    private const val CLAVE_CEREBRAS = "clave_cerebras"

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
}
