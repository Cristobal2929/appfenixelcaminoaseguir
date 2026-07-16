package com.fenix.aprendiz

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Pantalla central del Jardín — paso 1 del mapa acordado con Cristóbal.
 *
 * IdentidadActivity ya no lleva directo al Templo de Gratitud: primero pasa
 * por aquí. Esto permite ir sumando Altar de la Luz / Río de Conversación /
 * Círculo de Historias más adelante sin tener que reestructurar nada de lo
 * que ya funciona (Identidad y Templo quedan intactos).
 *
 * Por ahora solo el Templo está activo. Los demás son botones "próximamente"
 * — existen para que la persona vea que están en camino, pero no navegan
 * a ningún sitio todavía.
 */
class JardinActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOMBRE = "extra_nombre_jardin"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jardin)

        val nombre = intent.getStringExtra(EXTRA_NOMBRE)
            ?: Prefs.leerNombreJardin(this)
            ?: run {
                Toast.makeText(this, "Entra primero al Jardín.", Toast.LENGTH_LONG).show()
                finish()
                return
            }

        findViewById<TextView>(R.id.tvNombreJardin).text =
            getString(R.string.jardin_hub_bienvenida, nombre)

        findViewById<Button>(R.id.btnJardinTemplo).setOnClickListener {
            startActivity(Intent(this, GratitudActivity::class.java).putExtra(GratitudActivity.EXTRA_NOMBRE, nombre))
        }

        findViewById<Button>(R.id.btnJardinAltar).setOnClickListener {
            startActivity(Intent(this, AltarActivity::class.java).putExtra(AltarActivity.EXTRA_NOMBRE, nombre))
        }

        findViewById<Button>(R.id.btnJardinRio).setOnClickListener {
            startActivity(Intent(this, RioConversacionActivity::class.java).putExtra(RioConversacionActivity.EXTRA_NOMBRE, nombre))
        }

        val proximamente = { Toast.makeText(this, getString(R.string.jardin_proximamente), Toast.LENGTH_SHORT).show() }
        findViewById<Button>(R.id.btnJardinCirculo).setOnClickListener { proximamente() }
    }
}
