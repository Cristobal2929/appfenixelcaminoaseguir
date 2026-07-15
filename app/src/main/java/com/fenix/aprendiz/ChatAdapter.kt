package com.fenix.aprendiz

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Mensaje(val texto: String, val esUsuario: Boolean)

class ChatAdapter(private val mensajes: MutableList<Mensaje>) :
    RecyclerView.Adapter<ChatAdapter.VH>() {

    // Evita re-animar burbujas ya mostradas al hacer scroll hacia arriba/abajo.
    private var ultimaPosicionAnimada = -1

    class VH(val root: android.view.View) : RecyclerView.ViewHolder(root) {
        val tv: TextView = root.findViewById(R.id.tvMensaje)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_mensaje, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = mensajes[position]
        holder.tv.text = m.texto
        val root = holder.root as android.widget.LinearLayout
        if (m.esUsuario) {
            root.gravity = android.view.Gravity.END
            holder.tv.setBackgroundResource(R.drawable.bg_burbuja_usuario)
        } else {
            root.gravity = android.view.Gravity.START
            holder.tv.setBackgroundResource(R.drawable.bg_burbuja_fenix)
        }

        if (position > ultimaPosicionAnimada) {
            val anim = AnimationUtils.loadAnimation(holder.root.context, R.anim.burbuja_entrada)
            holder.root.startAnimation(anim)
            ultimaPosicionAnimada = position
        } else {
            holder.root.clearAnimation()
        }
    }

    override fun getItemCount() = mensajes.size

    fun obtenerTodos(): List<Mensaje> = mensajes

    fun agregar(m: Mensaje) {
        mensajes.add(m)
        notifyItemInserted(mensajes.size - 1)
    }

    /** Carga mensajes ya guardados (al abrir el chat) sin re-animar burbujas viejas. */
    fun cargarGuardados(guardados: List<Mensaje>) {
        mensajes.clear()
        mensajes.addAll(guardados)
        ultimaPosicionAnimada = mensajes.size - 1
        notifyDataSetChanged()
    }

    /** Vacía el chat (usado por "Reiniciar conversación"). */
    fun limpiar() {
        mensajes.clear()
        ultimaPosicionAnimada = -1
        notifyDataSetChanged()
    }
}
