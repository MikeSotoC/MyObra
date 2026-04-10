package com.uchi.myobra.ui.proyectos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.uchi.myobra.R
import com.uchi.myobra.data.ObraViewModel
import com.uchi.myobra.data.db.Proyecto
import com.uchi.myobra.databinding.FragmentProyectosBinding
import com.uchi.myobra.util.ThemeManager

class ProyectosFragment : Fragment() {

    private var _b: FragmentProyectosBinding? = null
    private val b  get() = _b!!
    private val vm: ObraViewModel by activityViewModels {
        androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(
            requireActivity().application
        )
    }
    private lateinit var adapter: ProyectoAdapter

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentProyectosBinding.inflate(inflater, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ProyectoAdapter(
            onOpen   = ::abrirProyecto,
            onEdit   = { p -> NewProyectoSheet.newInstance(p).show(childFragmentManager, "edit") },
            onDelete = ::confirmarEliminar
        )
        b.rvProyectos.adapter = adapter

        vm.todosLosProyectos.observe(viewLifecycleOwner) { lista ->
            adapter.submitList(lista)
            val hay = lista.isNotEmpty()
            b.rvProyectos.visibility  = if (hay) View.VISIBLE else View.GONE
            b.layoutEmpty.visibility  = if (hay) View.GONE   else View.VISIBLE
            b.statsBar.visibility     = if (hay) View.VISIBLE else View.GONE
            val n = lista.size
            b.tvConteoProyectos.text  = if (n == 1) "1 proyecto" else "$n proyectos"
        }

        b.fabNuevoProyecto.setOnClickListener { showNewSheet() }
        b.btnCrearPrimero.setOnClickListener  { showNewSheet() }

        b.btnToggleTema.setOnClickListener {
            ThemeManager.toggle(requireContext())
        }
    }

    private fun abrirProyecto(proyecto: Proyecto) {
        vm.abrirProyecto(proyecto)
        findNavController().navigate(R.id.nav_zapatas)
    }

    private fun showNewSheet() =
        NewProyectoSheet.newInstance(null).show(childFragmentManager, "new_proyecto")

    private fun confirmarEliminar(proyecto: Proyecto) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar \"${proyecto.nombre}\"")
            .setMessage("Se eliminarán todos los cálculos del proyecto. Esta acción no se puede deshacer.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ -> vm.eliminarProyecto(proyecto) }
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
