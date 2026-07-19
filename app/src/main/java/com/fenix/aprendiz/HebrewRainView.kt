package com.fenix.aprendiz

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.random.Random

/**
 * Fondo decorativo del chat: letras hebreas cayendo lentamente, muy tenues
 * (semi-transparentes) en el color del borde dorado #9A7125, para que
 * decoren sin molestar la lectura de los mensajes.
 */
class HebrewRainView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    // Alef, Bet, Guimel, Dálet, Hei, Vav, Zayin, Jet, Tet, Yud, Kaf, Lámed,
    // Mem, Nun, Sámej, Ayin, Pei, Tsadi, Qof, Resh, Shin, Tav.
    private val letras = "אבגדהוזחטיכלמנסעפצקרשת".map { it.toString() }

    private val colorBase = Color.parseColor("#9A7125")

    private data class Gota(
        var x: Float,
        var y: Float,
        val velocidad: Float,
        val tamano: Float,
        val alfa: Int,
        val letra: String
    )

    private val gotas = mutableListOf<Gota>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorBase
    }

    private var animador: ValueAnimator? = null
    private var inicializado = false

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0 && !inicializado) {
            generarGotas(w, h)
            inicializado = true
            arrancarAnimacion()
        }
    }

    private fun generarGotas(w: Int, h: Int) {
        val densidad = resources.displayMetrics.density
        val cantidad = (w / (34 * densidad)).toInt().coerceIn(6, 14)
        gotas.clear()
        repeat(cantidad) {
            gotas.add(
                Gota(
                    x = Random.nextFloat() * w,
                    y = Random.nextFloat() * h - h,
                    velocidad = (18 + Random.nextFloat() * 24) * densidad / 30f,
                    tamano = (16 + Random.nextFloat() * 12) * densidad,
                    // Muy tenue: entre ~10% y ~22% de opacidad, para no molestar la vista.
                    alfa = (26 + Random.nextInt(30)),
                    letra = letras.random()
                )
            )
        }
    }

    private fun arrancarAnimacion() {
        animador?.cancel()
        animador = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 16
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                val h = height
                if (h == 0) return@addUpdateListener
                for (gota in gotas) {
                    gota.y += gota.velocidad
                    if (gota.y > h) {
                        gota.y = -gota.tamano
                        gota.x = Random.nextFloat() * width
                    }
                }
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        for (gota in gotas) {
            paint.textSize = gota.tamano
            paint.alpha = gota.alfa
            canvas.drawText(gota.letra, gota.x, gota.y, paint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animador?.cancel()
    }
}
