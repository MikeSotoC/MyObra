package com.uchi.myobra.ui.sobrecimiento

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.google.android.material.snackbar.Snackbar
import com.uchi.myobra.data.CapecoDatos
import com.uchi.myobra.data.PreciosRepository
import com.uchi.myobra.data.ResultadoCalculo
import com.uchi.myobra.databinding.FragmentSobrecimientoBinding
import com.uchi.myobra.ui.base.BaseSeccionFragment
import com.uchi.myobra.ui.base.CapecoBottomSheet

class SobreFragment : BaseSeccionFragment() {

    private var _binding: FragmentSobrecimientoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSobrecimientoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = PreciosRepository.getInstance(requireContext())

        val opts = CapecoDatos.ciclopeoSobrecimiento.map { it.proporcion }
        binding.dropProporcion.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, opts))
        binding.dropProporcion.setText(opts[0], false)

        binding.btnTablaCapeco.setOnClickListener {
            CapecoBottomSheet.newInstance("ciclopeo").show(childFragmentManager, "capeco_sob")
        }
        binding.btnCalcular.setOnClickListener { calcular() }
        binding.btnLimpiar.setOnClickListener  { limpiar() }

        obraViewModel.sobrecimiento.observe(viewLifecycleOwner) { r ->
            if (r != null && r.isValid) { mostrarResultadosCard(requireView(), r); mostrarPresupuestoCard(requireView(), r) }
        }
    }

    private fun calcular() {
        val largo = binding.etLargoTotal.text.toString().toDoubleOrNull() ?: run { showError(); return }
        val ancho = binding.etAncho.text.toString().toDoubleOrNull()      ?: run { showError(); return }
        val alto  = binding.etAlto.text.toString().toDoubleOrNull()       ?: run { showError(); return }

        val propStr = binding.dropProporcion.text.toString()
        val coef = CapecoDatos.ciclopeoSobrecimiento.firstOrNull { it.proporcion == propStr }
            ?: CapecoDatos.ciclopeoSobrecimiento[0]
        val p   = repo.getPrecios()
        val vol = largo * ancho * alto

        val cem = vol * coef.cemento;      val hor = vol * coef.hormigon
        val pig = vol * coef.piedraGrande; val agu = vol * coef.agua
        val cCem = cem * p.cementoBolsa;   val cHor = hor * p.hormigonM3
        val cPiG = pig * p.piedraGrandM3;  val cAgu = agu * p.aguaM3
        val cMO  = vol * (CapecoDatos.MO_CONCRETO_OPERARIO_HH_M3 * p.moOperario +
                          CapecoDatos.MO_CONCRETO_OFICIAL_HH_M3  * p.moOficial  +
                          CapecoDatos.MO_CONCRETO_PEON_HH_M3     * p.moPeon)
        val totMat = cCem + cHor + cPiG + cAgu

        val r = ResultadoCalculo(
            volumenM3 = vol, cemento = cem, hormigon = hor, piedraGrande = pig, agua = agu,
            costoCemento = cCem, costoArena = cHor, costoAgua = cAgu, costoPiedraGrande = cPiG,
            costoManoObra = cMO, totalMateriales = totMat, totalObra = totMat + cMO,
            isValid = true,
            descripcion = "Sobrecimientos (L=${largo}m, A=${ancho}m, h=${alto}m, ${coef.proporcion})"
        )
        obraViewModel.guardarSeccion("sobrecimiento", r)
        mostrarResultadosCard(requireView(), r)
        mostrarPresupuestoCard(requireView(), r)
    }

    private fun limpiar() {
        binding.etLargoTotal.text?.clear()
        binding.etAncho.text?.clear()
        binding.etAlto.text?.clear()
        val empty = ResultadoCalculo()
        obraViewModel.guardarSeccion("sobrecimiento", empty)
        mostrarResultadosCard(requireView(), empty)
        mostrarPresupuestoCard(requireView(), empty)
    }

    private fun showError() =
        Snackbar.make(requireView(), "Ingrese todos los datos requeridos", Snackbar.LENGTH_SHORT).show()

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
