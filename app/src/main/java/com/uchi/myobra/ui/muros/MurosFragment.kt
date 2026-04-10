package com.uchi.myobra.ui.muros

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.google.android.material.snackbar.Snackbar
import com.uchi.myobra.data.CapecoDatos
import com.uchi.myobra.data.PreciosRepository
import com.uchi.myobra.data.ResultadoCalculo
import com.uchi.myobra.databinding.FragmentMurosBinding
import com.uchi.myobra.ui.base.BaseSeccionFragment
import com.uchi.myobra.ui.base.CapecoBottomSheet

class MurosFragment : BaseSeccionFragment() {

    private var _binding: FragmentMurosBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMurosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = PreciosRepository.getInstance(requireContext())

        val tipoOpts = CapecoDatos.murosLadrillos.map { it.tipo }.distinct()
        binding.dropTipoMuro.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tipoOpts))
        binding.dropTipoMuro.setText(tipoOpts[0], false)

        val aparejoOpts = listOf("Soga", "Cabeza")
        binding.dropAparejo.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, aparejoOpts))
        binding.dropAparejo.setText(aparejoOpts[0], false)

        binding.btnTablaCapeco.setOnClickListener {
            CapecoBottomSheet.newInstance("muros").show(childFragmentManager, "capeco_muros")
        }
        binding.btnCalcular.setOnClickListener { calcular() }
        binding.btnLimpiar.setOnClickListener  { limpiar() }

        obraViewModel.muros.observe(viewLifecycleOwner) { r ->
            if (r != null && r.isValid) { mostrarResultadosCard(requireView(), r); mostrarPresupuestoCard(requireView(), r) }
        }
    }

    private fun calcular() {
        val nPanos = binding.etNumPanos.text.toString().toDoubleOrNull() ?: run { showError(); return }
        val largo  = binding.etLargo.text.toString().toDoubleOrNull()   ?: run { showError(); return }
        val alto   = binding.etAlto.text.toString().toDoubleOrNull()    ?: run { showError(); return }

        val tipo    = binding.dropTipoMuro.text.toString()
        val aparejo = binding.dropAparejo.text.toString()
        val coef = CapecoDatos.murosLadrillos.firstOrNull { it.tipo == tipo && it.aparejo == aparejo }
            ?: CapecoDatos.murosLadrillos[0]
        val p    = repo.getPrecios()
        val area = nPanos * largo * alto

        val lad  = (area * coef.ladrillos).toInt()
        val cem  = area * coef.cemento
        val are  = area * coef.arena
        val cLad = lad * p.ladrilloKK
        val cCem = cem * p.cementoBolsa
        val cAre = are * p.arenaM3
        val cMO  = area * (CapecoDatos.MO_MURO_OPERARIO_HH_M2 * p.moOperario +
                           CapecoDatos.MO_MURO_PEON_HH_M2 * p.moPeon)
        val totMat = cLad + cCem + cAre

        val r = ResultadoCalculo(
            volumenM3 = area, cemento = cem, arena = are, ladrillo = lad,
            costoCemento = cCem, costoArena = cAre, costoLadrillo = cLad,
            costoManoObra = cMO, totalMateriales = totMat, totalObra = totMat + cMO,
            isValid = true,
            descripcion = "Muros ${coef.aparejo} — ${coef.tipo.take(22)} (${fmt.format(area)} m²)"
        )
        obraViewModel.guardarSeccion("muros", r)
        mostrarResultadosCard(requireView(), r)
        mostrarPresupuestoCard(requireView(), r)
    }

    private fun limpiar() {
        binding.etNumPanos.setText("1")
        binding.etLargo.text?.clear()
        binding.etAlto.text?.clear()
        val empty = ResultadoCalculo()
        obraViewModel.guardarSeccion("muros", empty)
        mostrarResultadosCard(requireView(), empty)
        mostrarPresupuestoCard(requireView(), empty)
    }

    private fun showError() =
        Snackbar.make(requireView(), "Ingrese todos los datos requeridos", Snackbar.LENGTH_SHORT).show()

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
