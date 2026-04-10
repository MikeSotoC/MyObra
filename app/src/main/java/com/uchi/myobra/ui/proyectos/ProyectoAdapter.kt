package com.uchi.myobra.ui.proyectos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.uchi.myobra.data.db.Proyecto
import com.uchi.myobra.databinding.ItemProyectoCardBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProyectoAdapter(
    private val onOpen:   (Proyecto) -> Unit,
    private val onEdit:   (Proyecto) -> Unit,
    private val onDelete: (Proyecto) -> Unit
) : ListAdapter<Proyecto, ProyectoAdapter.VH>(DIFF) {

    private val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE"))

    inner class VH(val b: ItemProyectoCardBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(p: Proyecto) {
            b.tvNombreProyecto.text   = p.nombre
            b.tvPropietario.text      = p.propietario.ifBlank { "Sin propietario" }
            b.tvUbicacion.text        = p.ubicacion.ifBlank { "Sin ubicación" }
            b.tvFechaModificacion.text= "Mod. ${dateFmt.format(Date(p.fechaModificacion))}"

            b.root.setOnClickListener    { onOpen(p) }
            b.btnEditarProyecto.setOnClickListener  { onEdit(p) }
            b.btnEliminarProyecto.setOnClickListener{ onDelete(p) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemProyectoCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Proyecto>() {
            override fun areItemsTheSame(a: Proyecto, b: Proyecto) = a.id == b.id
            override fun areContentsTheSame(a: Proyecto, b: Proyecto) = a == b
        }
    }
}
