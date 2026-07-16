package com.fenix.aprendiz

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Revelación de la casa: muestra la tribu asignada en el centro, el número
 * del alma, el mensaje (por ahora la bendición tradicional de la propia
 * tribu, de Tribu.kt) y las 12 casas, con la del alma resaltada. El botón
 * "Empieza tu camino" entra a la app que ya teníamos (MainActivity).
 */
class RevelacionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NUMERO = "extra_numero_alma"
        const val EXTRA_SEXO = "extra_sexo"
        const val EXTRA_NOMBRE = "extra_nombre_revelacion"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_revelacion)

        val numero = intent.getIntExtra(EXTRA_NUMERO, Prefs.leerCasaNumero(this) ?: 1)
        val sexo = intent.getStringExtra(EXTRA_SEXO) ?: Prefs.leerSexoCasa(this)
        val tribu = Tribus.porNumero(numero) ?: Tribus.TODAS.first()

        findViewById<TextView>(R.id.tvCasaNombre).text = tribu.nombre
        findViewById<TextView>(R.id.tvCasaSimbolo).text = tribu.simbolo
        findViewById<TextView>(R.id.tvBienvenida).text =
            getString(if (sexo == "m") R.string.rev_esta_es_m else R.string.rev_esta_es_h)
        findViewById<TextView>(R.id.tvNumeroAlma).text = numero.toString()
        findViewById<TextView>(R.id.tvMensajeCasa).text = tribu.texto

        construirDoceCasas(numero)

        findViewById<Button>(R.id.btnEmpezarCamino).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }
    }

    private fun construirDoceCasas(numeroAsignado: Int) {
        val grid = findViewById<GridLayout>(R.id.gridCasas)
        val margen = dp(6)
        val padV = dp(12)
        val padH = dp(6)

        for (tribu in Tribus.TODAS) {
            val activa = tribu.numeroEsencia == numeroAsignado
            val colorTribu = colorDe(tribu.colorResName)

            val chip = TextView(this).apply {
                text = if (activa) "★ ${tribu.nombre}" else tribu.nombre
                gravity = Gravity.CENTER
                setPadding(padH, padV, padH, padV)
                textSize = 13f
                setTextColor(
                    getColor(if (activa) R.color.teshua_dorado else R.color.teshua_texto_apagado)
                )
                if (activa) setTypeface(typeface, android.graphics.Typeface.BOLD)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(10).toFloat()
                    setColor(getColor(R.color.teshua_fondo_panel))
                    setStroke(if (activa) dp(3) else dp(1), colorTribu)
                }
            }

            val lp = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(margen, margen, margen, margen)
            }
            grid.addView(chip, lp)
        }
    }

    private fun colorDe(resName: String): Int {
        val id = resources.getIdentifier(resName, "color", packageName)
        return if (id != 0) getColor(id) else getColor(R.color.teshua_dorado)
    }

    private fun dp(valor: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, valor.toFloat(), resources.displayMetrics
        ).toInt()
}
