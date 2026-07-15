package com.fenix.aprendiz

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Mensaje(val texto: String, val esUsuario: Boolean)

class ChatAdapter(private val mensajes: MutableList<Mensaje>) :
    RecyclerView.Adapter<ChatAdapter.VH>() {

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
    }

    override fun getItemCount() = mensajes.size

    fun agregar(m: Mensaje) {
        mensajes.add(m)
        notifyItemInserted(mensajes.size - 1)
    }
}
