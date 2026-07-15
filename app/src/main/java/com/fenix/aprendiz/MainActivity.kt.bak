package com.fenix.aprendiz

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**
 * Fénix Aprendiz — la app.
 *
 * No lleva ningún cerebro ni base de datos dentro: es una ventana (WebView) que
 * abre tu Space de Hugging Face. TODO son llamadas al Space, y el Space
 * responde —chat, la voz de Teshuá con su eco, la memoria en Supabase—, tal cual
 * lo diseñamos. El usuario baja una app ligera; tú mantienes el cerebro en tu
 * lado.
 *
 * Si algún día cambias el nombre del Space, solo tienes que cambiar la línea
 * URL_ESPACIO de abajo y recompilar.
 */
class MainActivity : AppCompatActivity() {

    // Dirección pública de tu Space. Formato de Hugging Face:
    // https://<usuario>-<nombre-del-space>.hf.space  (todo en minúsculas)
    private val URL_ESPACIO = "https://cristobal299-fenix-aprendiz.hf.space"

    private lateinit var web: WebView

    // Para poder subir PDF desde la pestaña "Memoria Documental" del Space.
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

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        web = WebView(this)
        setContentView(web)

        web.settings.apply {
            javaScriptEnabled = true          // Gradio necesita JavaScript
            domStorageEnabled = true          // ...y almacenamiento local
            // Deja que la voz de Teshuá suene sola al llegar la respuesta, sin
            // que el usuario tenga que tocar la pantalla antes.
            mediaPlaybackRequiresUserGesture = false
        }

        // La navegación se queda dentro de la app (no salta al navegador).
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean = false
        }

        web.webChromeClient = object : WebChromeClient() {
            // Abre el selector de archivos cuando el Space pide subir un PDF.
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

            // Concede permisos que pida la web (por si más adelante el Space usa
            // micrófono para el chat de voz en directo).
            override fun onPermissionRequest(request: PermissionRequest) {
                request.grant(request.resources)
            }
        }

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState)
        } else {
            web.loadUrl(URL_ESPACIO)
        }
    }

    // El botón "atrás" navega dentro del Space antes de cerrar la app.
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }
}
