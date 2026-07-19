package com.fenix.aprendiz

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

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
 *
 * Fase C real: además se puede activar una burbuja flotante (Fénix
 * disponible encima de cualquier otra app) mediante FloatingBubbleService,
 * que requiere el permiso especial "dibujar sobre otras apps".
 */
class MainActivity : AppCompatActivity() {

    private val pedirPermisoNotificaciones = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* si lo niega, simplemente no habrá sonido/ícono en 2º plano */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        NotificationHelper.crearCanales(this)
        pedirPermisoNotificacionesSiHaceFalta()

        val emblema = findViewById<android.view.View>(R.id.emblema)
        respirarEmblema(emblema)
        aplicarTribuDeLaPersona()

        findViewById<android.widget.Button>(R.id.btnHablarFenix).setOnClickListener {
            abrirChat()
        }

        findViewById<android.widget.TextView>(R.id.btnAjustesHome).setOnClickListener {
            abrirChat(irDirectoAAjustes = true)
        }

        findViewById<android.widget.TextView>(R.id.btnBurbujaHome).setOnClickListener {
            activarBurbuja()
        }

        findViewById<android.widget.TextView>(R.id.btnJardinHome).setOnClickListener {
            startActivity(Intent(this, IdentidadActivity::class.java))
            overridePendingTransition(R.anim.slide_up_in, R.anim.fade_out)
        }
    }

    private fun pedirPermisoNotificacionesSiHaceFalta() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val concedido = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!concedido) pedirPermisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun abrirChat(irDirectoAAjustes: Boolean = false) {
        val intent = Intent(this, ChatActivity::class.java)
        if (irDirectoAAjustes) intent.putExtra(ChatActivity.EXTRA_ABRIR_AJUSTES, true)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_up_in, R.anim.fade_out)
    }

    /**
     * Activa la burbuja flotante. Si el permiso de "dibujar sobre otras
     * apps" no está concedido (obligatorio en Android 6+), abre los
     * ajustes del sistema para que el usuario lo conceda una sola vez.
     */
    private fun activarBurbuja() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(
                this,
                "Activa \"Permitir sobre otras apps\" para Orígenes y vuelve a tocar el botón.",
                Toast.LENGTH_LONG
            ).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return
        }

        startService(Intent(this, FloatingBubbleService::class.java))
        Toast.makeText(this, "Orígenes ya flota. Tócalo para hablarle desde cualquier app.", Toast.LENGTH_SHORT).show()
        moveTaskToBack(true)
    }

    /**
     * Sustituye la espiral genérica por el sello de la tribu que el Portal
     * ya reveló para esta persona (guardada en Prefs), y muestra su nombre
     * y un consejo propio de esa casa para el día de hoy.
     *
     * Si por lo que sea aún no hay casa guardada (no debería pasar, porque
     * SplashActivity manda al Portal antes de llegar aquí), se deja tal
     * cual está en el XML: la espiral y el texto de bienvenida genéricos.
     */
    private fun aplicarTribuDeLaPersona() {
        val numeroCasa = Prefs.leerCasaNumero(this) ?: return
        val tribu = Tribus.porNumero(numeroCasa) ?: return

        val idEmblema = resources.getIdentifier(tribu.emblemaResName, "drawable", packageName)
        if (idEmblema != 0) {
            findViewById<android.widget.ImageView>(R.id.ivEmblemaTribu).setImageResource(idEmblema)
        }

        findViewById<android.widget.TextView>(R.id.tvNombreTribuHome).apply {
            text = getString(R.string.home_casa_de, tribu.nombre)
            setTextColor(colorDe(tribu.colorResName))
            visibility = android.view.View.VISIBLE
        }

        findViewById<android.widget.TextView>(R.id.tvBienvenidaHome).text = tribu.consejo
    }

    private fun colorDe(resName: String): Int {
        val id = resources.getIdentifier(resName, "color", packageName)
        return if (id != 0) getColor(id) else getColor(R.color.teshua_dorado)
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
