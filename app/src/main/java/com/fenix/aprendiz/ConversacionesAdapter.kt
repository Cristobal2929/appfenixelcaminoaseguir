package com.fenix.aprendiz

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lista del historial (cajón izquierdo): cada fila es una conversación
 * guardada. Un toque la abre; una pulsación larga ofrece borrarla.
 */
class ConversacionesAdapter(
    private val items: MutableList<Prefs.ResumenConversacion>,
    private val alAbrir: (Prefs.ResumenConversacion) -> Unit,
    private val alBorrar: (Prefs.ResumenConversacion) -> Unit
) : RecyclerView.Adapter<ConversacionesAdapter.VH>() {

    private val formatoFecha = SimpleDateFormat("d MMM · HH:mm", Locale("es", "ES"))

    class VH(root: View) : RecyclerView.ViewHolder(root) {
        val titulo: TextView = root.findViewById(R.id.tvConvTitulo)
        val fecha: TextView = root.findViewById(R.id.tvConvFecha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversacion, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        holder.titulo.text = if (c.titulo.isBlank()) "Conversación" else c.titulo
        holder.fecha.text = formatoFecha.format(Date(c.ts))
        holder.itemView.setOnClickListener { alAbrir(c) }
        holder.itemView.setOnLongClickListener { alBorrar(c); true }
    }

    override fun getItemCount() = items.size

    fun refrescar(nuevos: List<Prefs.ResumenConversacion>) {
        items.clear()
        items.addAll(nuevos)
        notifyDataSetChanged()
    }
}
