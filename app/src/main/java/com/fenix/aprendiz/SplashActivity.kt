package com.fenix.aprendiz

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val DURACION_MS = 1400L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            // Primera vez (sin casa revelada) -> Portal de Teshuá.
            // Si ya pasó por el Portal, directo a la app.
            val destino = if (Prefs.leerCasaNumero(this) == null)
                PortalActivity::class.java else MainActivity::class.java
            startActivity(Intent(this, destino))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }, DURACION_MS)
    }
}
