package com.fenix.aprendiz

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Todo lo relacionado a "que suene y se vea" cuando pasa algo:
 *  - Un mensaje nuevo de Orígenes llega mientras la app está en 2º plano.
 *  - Una historia nueva aparece en el Círculo.
 *  - La primera vez que a la persona se le asigna su casa: una pequeña
 *    melodía de bienvenida (no requiere ningún archivo de audio: se genera
 *    con ToneGenerator, así no añadimos assets pesados al APK).
 */
object NotificationHelper {

    const val CANAL_MENSAJES = "fenix_mensajes"
    const val CANAL_CIRCULO = "fenix_circulo"

    private const val ID_NOTIF_MENSAJE = 1001
    private const val ID_NOTIF_CIRCULO = 1002

    fun crearCanales(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val sonido = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val atributosAudio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val canalMensajes = NotificationChannel(
            CANAL_MENSAJES,
            context.getString(R.string.notif_canal_mensajes),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notif_canal_mensajes_desc)
            setSound(sonido, atributosAudio)
            enableVibration(true)
        }

        val canalCirculo = NotificationChannel(
            CANAL_CIRCULO,
            context.getString(R.string.notif_canal_circulo),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notif_canal_circulo_desc)
            setSound(sonido, atributosAudio)
            enableVibration(true)
        }

        manager.createNotificationChannel(canalMensajes)
        manager.createNotificationChannel(canalCirculo)
    }

    /** Notifica que Orígenes respondió (chat en 2º plano): ícono + sonido. */
    fun notificarRespuestaChat(context: Context, texto: String) {
        crearCanales(context)
        val intent = Intent(context, ChatActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pending = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(context, CANAL_MENSAJES)
            .setSmallIcon(R.drawable.ic_teshua_spiral)
            .setContentTitle(context.getString(R.string.notif_respuesta_titulo))
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(context).notify(ID_NOTIF_MENSAJE, notif)
    }

    /** Notifica una historia nueva en el Círculo: ícono + sonido. */
    fun notificarHistoriaCirculo(context: Context, autor: String, texto: String) {
        crearCanales(context)
        val intent = Intent(context, CirculoActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pending = PendingIntent.getActivity(
            context, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(context, CANAL_CIRCULO)
            .setSmallIcon(R.drawable.ic_teshua_spiral)
            .setContentTitle(context.getString(R.string.notif_circulo_titulo))
            .setContentText("$autor: $texto")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$autor: $texto"))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        NotificationManagerCompat.from(context).notify(ID_NOTIF_CIRCULO, notif)
    }

    /**
     * Melodía de "bienvenida a casa": una secuencia ascendente de tonos,
     * suave, tocada una sola vez cuando a la persona se le asigna su casa
     * por primera vez. No necesita ningún archivo de audio.
     */
    fun tocarMelodiaBienvenida() {
        Thread {
            try {
                val tono = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
                val secuencia = intArrayOf(
                    ToneGenerator.TONE_PROP_BEEP2,
                    ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,
                    ToneGenerator.TONE_PROP_ACK,
                    ToneGenerator.TONE_CDMA_CONFIRM
                )
                for (t in secuencia) {
                    tono.startTone(t, 220)
                    Thread.sleep(260)
                }
                Thread.sleep(300)
                tono.release()
            } catch (e: Exception) {
                // Si el dispositivo no puede tocar el tono, no interrumpimos la revelación.
            }
        }.start()
    }
}
