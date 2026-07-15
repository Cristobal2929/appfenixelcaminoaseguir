package com.fenix.aprendiz

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Fénix Aprendiz / Teshuá — pantalla de inicio.
 *
 * REDISEÑO IMPORTANTE: esta pantalla ya NO carga el Space en un WebView.
 * Antes mostraba directamente la página del Space (con todo el contenido
 * personal de Pedro) a pantalla completa, lo cual no era la intención.
 *
 * Ahora es una pantalla 100% nativa con identidad propia (espiral Teshuá,
 * letras hebreas de fondo, paleta negro/dorado). El único camino hacia
 * adelante es el botón "Hablar con Fénix", que abre ChatActivity: ahí es
 * donde ocurre toda la conversación (texto y voz) hablando con el Space
 * por API, sin que el usuario vea jamás la página web del Space.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val emblema = findViewById<android.view.View>(R.id.emblema)
        respirarEmblema(emblema)

        findViewById<android.widget.Button>(R.id.btnHablarFenix).setOnClickListener {
            abrirChat()
        }

        findViewById<android.widget.TextView>(R.id.btnAjustesHome).setOnClickListener {
            abrirChat(irDirectoAAjustes = true)
        }
    }

    private fun abrirChat(irDirectoAAjustes: Boolean = false) {
        val intent = Intent(this, ChatActivity::class.java)
        if (irDirectoAAjustes) intent.putExtra(ChatActivity.EXTRA_ABRIR_AJUSTES, true)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_up_in, R.anim.fade_out)
    }

    /** Respiración sutil y continua del emblema: nunca es un WebView, pero tampoco está muerto. */
    private fun respirarEmblema(view: android.view.View) {
        val escalaX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.06f).apply {
            duration = 1800
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
        }
        val escalaY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.06f).apply {
            duration = 1800
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
        }
        escalaX.start()
        escalaY.start()
    }
}
