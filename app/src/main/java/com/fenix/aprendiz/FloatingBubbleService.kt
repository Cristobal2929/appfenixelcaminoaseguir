package com.fenix.aprendiz

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Fase C real: la burbuja flotante de Fénix/Teshuá.
 *
 * Dibuja un círculo dorado (el mismo estilo del emblema de Home) encima de
 * cualquier otra app usando WindowManager. Se puede arrastrar por la
 * pantalla; si se suelta cerca de la "zona de cierre" (abajo, en rojo),
 * la burbuja desaparece y el servicio se detiene. Un toque simple (sin
 * arrastre) abre ChatActivity directamente, para hablar con Fénix sin
 * salir de la app en la que estabas.
 */
class FloatingBubbleService : Service() {

    private lateinit var windowManager: WindowManager

    private var burbujaView: View? = null
    private var burbujaParams: WindowManager.LayoutParams? = null

    private var zonaCierreView: View? = null
    private var zonaCierreParams: WindowManager.LayoutParams? = null

    private var xInicial = 0
    private var yInicial = 0
    private var xToqueInicial = 0f
    private var yToqueInicial = 0f
    private var seMovio = false

    companion object {
        private const val UMBRAL_CLICK = 12
        private const val RADIO_CIERRE = 160
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        crearZonaCierre()
        crearBurbuja()
    }

    private fun tipoOverlay(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    private fun crearBurbuja() {
        val tamano = (58 * resources.displayMetrics.density).toInt()

        val frame = FrameLayout(this).apply {
            setBackgroundResource(R.drawable.bg_fab_teshua)
        }
        val icono = ImageView(this).apply {
            setImageResource(R.drawable.ic_teshua_spiral)
            val padding = (12 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }
        frame.addView(
            icono,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )
        burbujaView = frame

        val params = WindowManager.LayoutParams(
            tamano, tamano,
            tipoOverlay(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = resources.displayMetrics.widthPixels - tamano - (16 * resources.displayMetrics.density).toInt()
        params.y = (120 * resources.displayMetrics.density).toInt()
        burbujaParams = params

        frame.setOnTouchListener { _, event -> onToqueBurbuja(event) }

        windowManager.addView(frame, params)
    }

    private fun crearZonaCierre() {
        val tamano = (70 * resources.displayMetrics.density).toInt()
        val view = ImageView(this).apply {
            setBackgroundResource(R.drawable.bg_close_zone)
            setImageResource(R.drawable.ic_close)
            val padding = (18 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            visibility = View.GONE
        }
        zonaCierreView = view

        val params = WindowManager.LayoutParams(
            tamano, tamano,
            tipoOverlay(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        params.y = (48 * resources.displayMetrics.density).toInt()
        zonaCierreParams = params

        windowManager.addView(view, params)
    }

    private fun onToqueBurbuja(event: MotionEvent): Boolean {
        val params = burbujaParams ?: return false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                xInicial = params.x
                yInicial = params.y
                xToqueInicial = event.rawX
                yToqueInicial = event.rawY
                seMovio = false
                zonaCierreView?.visibility = View.VISIBLE
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - xToqueInicial).toInt()
                val dy = (event.rawY - yToqueInicial).toInt()
                if (abs(dx) > UMBRAL_CLICK || abs(dy) > UMBRAL_CLICK) seMovio = true

                params.x = xInicial + dx
                params.y = yInicial + dy
                windowManager.updateViewLayout(burbujaView, params)
                return true
            }
            MotionEvent.ACTION_UP -> {
                zonaCierreView?.visibility = View.GONE

                if (!seMovio) {
                    abrirChat()
                } else if (estaSobreZonaCierre(params)) {
                    detenerBurbuja()
                }
                return true
            }
        }
        return false
    }

    private fun estaSobreZonaCierre(params: WindowManager.LayoutParams): Boolean {
        val metrics = resources.displayMetrics
        val cierreParams = zonaCierreParams ?: return false
        val burbujaCentroX = params.x + (burbujaView?.width ?: 0) / 2
        val burbujaCentroY = params.y + (burbujaView?.height ?: 0) / 2
        val cierreCentroX = metrics.widthPixels / 2
        val cierreCentroY = metrics.heightPixels - cierreParams.y - (zonaCierreView?.height ?: 0) / 2
        val distancia = hypot(
            (burbujaCentroX - cierreCentroX).toDouble(),
            (burbujaCentroY - cierreCentroY).toDouble()
        )
        return distancia < RADIO_CIERRE * resources.displayMetrics.density
    }

    private fun abrirChat() {
        val intent = Intent(this, ChatActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun detenerBurbuja() {
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        burbujaView?.let { runCatching { windowManager.removeView(it) } }
        zonaCierreView?.let { runCatching { windowManager.removeView(it) } }
    }
}
