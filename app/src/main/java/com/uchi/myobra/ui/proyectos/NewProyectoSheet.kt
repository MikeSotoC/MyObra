package com.uchi.myobra.ui.proyectos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.uchi.myobra.data.ObraViewModel
import com.uchi.myobra.data.db.Proyecto
import com.uchi.myobra.databinding.BottomSheetNewProyectoBinding

class NewProyectoSheet : BottomSheetDialogFragment() {

    private var _b: BottomSheetNewProyectoBinding? = null
    private val b get() = _b!!
    private val vm: ObraViewModel by activityViewModels {
        androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(
            requireActivity().application
        )
    }

    private var proyectoExistente: Proyecto? = null

    companion object {
        private const val ARG_ID          = "id"
        private const val ARG_NOMBRE      = "nombre"
        private const val ARG_PROPIETARIO = "propietario"
        private const val ARG_UBICACION   = "ubicacion"
        private const val ARG_DESCRIPCION = "descripcion"

        fun newInstance(p: Proyecto?) = NewProyectoSheet().apply {
            if (p != null) {
                arguments = bundleOf(
                    ARG_ID          to p.id,
                    ARG_NOMBRE      to p.nombre,
                    ARG_PROPIETARIO to p.propietario,
                    ARG_UBICACION   to p.ubicacion,
                    ARG_DESCRIPCION to p.descripcion
                )
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = BottomSheetNewProyectoBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val id = arguments?.getLong(ARG_ID, 0L) ?: 0L
        if (id > 0L) {
            proyectoExistente = Proyecto(
                id          = id,
                nombre      = arguments?.getString(ARG_NOMBRE, "") ?: "",
                propietario = arguments?.getString(ARG_PROPIETARIO, "") ?: "",
                ubicacion   = arguments?.getString(ARG_UBICACION, "") ?: "",
                descripcion = arguments?.getString(ARG_DESCRIPCION, "") ?: ""
            )
            // Pre-fill for editing
            b.etNombre.setText(proyectoExistente!!.nombre)
            b.etPropietario.setText(proyectoExistente!!.propietario)
            b.etUbicacion.setText(proyectoExistente!!.ubicacion)
            b.etDescripcion.setText(proyectoExistente!!.descripcion)
            b.tvTituloSheet.text = "Editar proyecto"
            b.btnGuardar.text    = "Guardar cambios"
        } else {
            b.tvTituloSheet.text = "Nuevo proyecto"
            b.btnGuardar.text    = "Crear proyecto"
        }

        b.btnGuardar.setOnClickListener { guardar() }
        b.btnCancelar.setOnClickListener { dismiss() }
    }

    private fun guardar() {
        val nombre      = b.etNombre.text.toString().trim()
        val propietario = b.etPropietario.text.toString().trim()
        val ubicacion   = b.etUbicacion.text.toString().trim()
        val descripcion = b.etDescripcion.text.toString().trim()

        if (nombre.isEmpty()) {
            b.tilNombre.error = "El nombre es obligatorio"
            return
        }
        b.tilNombre.error = null

        val existente = proyectoExistente
        if (existente != null) {
            vm.actualizarProyecto(existente.copy(
                nombre      = nombre,
                propietario = propietario,
                ubicacion   = ubicacion,
                descripcion = descripcion
            ))
        } else {
            vm.crearProyecto(nombre, propietario, ubicacion, descripcion)
        }
        dismiss()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
