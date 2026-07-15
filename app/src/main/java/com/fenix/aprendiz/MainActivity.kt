package com.fenix.aprendiz

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs

/**
 * Fénix Aprendiz / Teshuá — la app.
 *
 * Sigue sin llevar ningún cerebro ni base de datos dentro: el WebView abre tu
 * Space de Hugging Face completo, con voz e IA. Encima flota el botón Teshuá:
 *  - toque corto -> abre el chat en vivo nativo (ChatActivity), solo texto.
 *  - arrastrarlo hasta la zona roja inferior -> cierra la app.
 *
 * Si algún día cambias el nombre del Space, cambia URL_ESPACIO y recompila.
 */
class MainActivity : AppCompatActivity() {

    private val URL_ESPACIO = "https://cristobal299-fenix-aprendiz.hf.space"

    private lateinit var web: WebView
    private lateinit var fab: View
    private lateinit var closeZone: View

    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private val selectorArchivo = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val cb = fileCallback ?: return@registerForActivityResult
        cb.onReceiveValue(
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        )
        fileCallback = null
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        web = findViewById(R.id.webView)
        fab = findViewById(R.id.fabTeshua)
        closeZone = findViewById(R.id.closeZone)

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
        }

        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean = false
        }

        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                params: FileChooserParams
            ): Boolean {
                fileCallback = filePathCallback
                return try {
                    selectorArchivo.launch(params.createIntent())
                    true
                } catch (e: Exception) {
                    fileCallback = null
                    false
                }
            }

            override fun onPermissionRequest(request: PermissionRequest) {
                request.grant(request.resources)
            }
        }

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState)
        } else {
            web.loadUrl(URL_ESPACIO)
        }

        configurarBotonFlotante()
    }

    /** Botón Teshuá: semi-transparente en reposo, arrastrable, tap corto abre el chat. */
    private fun configurarBotonFlotante() {
        var dX = 0f
        var dY = 0f
        var startX = 0f
        var startY = 0f
        var arrastrando = false
        val umbralClick = 12f

        fab.alpha = 0.35f

        fab.setOnTouchListener { v, event ->
            val parent = v.parent as ViewGroup
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = v.x - event.rawX
                    dY = v.y - event.rawY
                    startX = event.rawX
                    startY = event.rawY
                    arrastrando = false
                    v.animate().alpha(1f).setDuration(120).start()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val movX = abs(event.rawX - startX)
                    val movY = abs(event.rawY - startY)
                    if (movX > umbralClick || movY > umbralClick) {
                        arrastrando = true
                        closeZone.visibility = View.VISIBLE
                        var nuevaX = event.rawX + dX
                        var nuevaY = event.rawY + dY
                        nuevaX = nuevaX.coerceIn(0f, (parent.width - v.width).toFloat())
                        nuevaY = nuevaY.coerceIn(0f, (parent.height - v.height).toFloat())
                        v.x = nuevaX
                        v.y = nuevaY

                        if (estaSobreZonaCierre(v)) {
                            closeZone.scaleX = 1.25f
                            closeZone.scaleY = 1.25f
                        } else {
                            closeZone.scaleX = 1f
                            closeZone.scaleY = 1f
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    closeZone.visibility = View.GONE
                    if (!arrastrando) {
                        // Toque corto -> abrir chat en vivo
                        startActivity(Intent(this, ChatActivity::class.java))
                        v.animate().alpha(0.35f).setDuration(200).start()
                    } else if (estaSobreZonaCierre(v)) {
                        finishAndRemoveTask()
                    } else {
                        v.animate().alpha(0.35f).setDuration(400).start()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun estaSobreZonaCierre(fabView: View): Boolean {
        val fabCentroX = fabView.x + fabView.width / 2
        val fabCentroY = fabView.y + fabView.height / 2
        val czX = closeZone.x + closeZone.width / 2
        val czY = closeZone.y + closeZone.height / 2
        val distancia = Math.hypot((fabCentroX - czX).toDouble(), (fabCentroY - czY).toDouble())
        return distancia < closeZone.width
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }
}
