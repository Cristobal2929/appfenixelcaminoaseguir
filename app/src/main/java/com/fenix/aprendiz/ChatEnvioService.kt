package com.fenix.aprendiz

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import org.json.JSONArray

/**
 * FIX pedido: "que cuando está escribiendo, si sale de la app no se
 * cuelgue y siga en 2º plano". Antes, el envío corría en un Thread
 * colgado de ChatActivity: si Android mataba el proceso al salir de la
 * app (memoria baja, mucho tiempo fuera, etc.) la respuesta se perdía y
 * al volver el chat quedaba "colgado" en está escribiendo para siempre.
 *
 * Ahora el envío corre dentro de un Service en primer plano (con su
 * propia notificación, obligatoria desde Android O), que Android no mata
 * mientras dura. Así el mensaje SIEMPRE llega, se guarda en Prefs y, si
 * la persona ya no está mirando el chat, se avisa con notificación +
 * sonido. Si sigue en el chat, ChatActivity escucha el mismo resultado
 * por broadcast y actualiza la pantalla al instante.
 */
class ChatEnvioService : Service() {

    companion object {
        const val ACTION_RESULTADO = "com.fenix.aprendiz.ACTION_CHAT_RESULTADO"
        const val EXTRA_OK = "ok"
        const val EXTRA_RESPUESTA = "respuesta"
        const val EXTRA_AUDIO_URL = "audio_url"
        const val EXTRA_CATEGORIA = "categoria"
        const val EXTRA_ERROR = "error"

        const val EXTRA_MENSAJE = "mensaje"
        const val EXTRA_HISTORIAL_GRADIO = "historial_gradio"
        const val EXTRA_CLAVE_USUARIO = "clave_usuario"
        const val EXTRA_NOMBRE_JARDIN = "nombre_jardin"

        private const val ID_NOTIF_ESCRIBIENDO = 2001

        @Volatile
        var chatVisible = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        iniciarComoPrimerPlano()

        val mensaje = intent.getStringExtra(EXTRA_MENSAJE) ?: run { stopSelf(startId); return START_NOT_STICKY }
        val historialJson = intent.getStringExtra(EXTRA_HISTORIAL_GRADIO) ?: "[]"
        val historial = try { JSONArray(historialJson) } catch (e: Exception) { JSONArray() }
        val claveUsuario = intent.getStringExtra(EXTRA_CLAVE_USUARIO)
        val nombreJardin = intent.getStringExtra(EXTRA_NOMBRE_JARDIN)

        FenixApiClient.enviarMensaje(
            mensaje = mensaje,
            historialGradio = historial,
            claveUsuario = claveUsuario,
            nombreJardin = nombreJardin,
            onResultado = { respuesta, historialActualizado, audioUrl, categoria ->
                persistirRespuesta(respuesta, historialActualizado, categoria)
                emitirResultado(
                    ok = true,
                    respuesta = respuesta,
                    audioUrl = audioUrl,
                    categoria = categoria,
                    error = null
                )
                if (!chatVisible) {
                    NotificationHelper.notificarRespuestaChat(applicationContext, respuesta)
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
            },
            onError = { error ->
                emitirResultado(ok = false, respuesta = null, audioUrl = null, categoria = null, error = error)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
            }
        )

        return START_NOT_STICKY
    }

    /** Guarda la respuesta en Prefs aunque la Activity ya no exista (app en 2º plano o cerrada). */
    private fun persistirRespuesta(respuesta: String, historialActualizado: JSONArray, categoria: String?) {
        val mensajesUI = Prefs.leerMensajesUI(applicationContext)
        mensajesUI.add(Mensaje(respuesta, esUsuario = false))
        Prefs.guardarMensajesUI(applicationContext, mensajesUI)
        Prefs.guardarHistorialGradio(applicationContext, historialActualizado)
        Prefs.guardarUltimaLeccion(applicationContext, categoria)
        Prefs.archivarConversacionActual(applicationContext, mensajesUI, historialActualizado, categoria)
    }

    private fun emitirResultado(
        ok: Boolean, respuesta: String?, audioUrl: String?, categoria: String?, error: String?
    ) {
        val intent = Intent(ACTION_RESULTADO).apply {
            setPackage(applicationContext.packageName)
            putExtra(EXTRA_OK, ok)
            putExtra(EXTRA_RESPUESTA, respuesta)
            putExtra(EXTRA_AUDIO_URL, audioUrl)
            putExtra(EXTRA_CATEGORIA, categoria)
            putExtra(EXTRA_ERROR, error)
        }
        sendBroadcast(intent)
    }

    private fun iniciarComoPrimerPlano() {
        NotificationHelper.crearCanales(applicationContext)
        val intent = Intent(this, ChatActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pending = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif: Notification = NotificationCompat.Builder(this, NotificationHelper.CANAL_MENSAJES)
            .setSmallIcon(R.drawable.ic_teshua_spiral)
            .setContentTitle(getString(R.string.chat_escribiendo))
            .setContentIntent(pending)
            .setSilent(true)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(ID_NOTIF_ESCRIBIENDO, notif, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(ID_NOTIF_ESCRIBIENDO, notif)
        }
    }
}
