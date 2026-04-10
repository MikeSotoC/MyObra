package com.uchi.myobra.ui.falsopiso

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.google.android.material.snackbar.Snackbar
import com.uchi.myobra.data.CapecoDatos
import com.uchi.myobra.data.PreciosRepository
import com.uchi.myobra.data.ResultadoCalculo
import com.uchi.myobra.databinding.FragmentFalsopisoBinding
import com.uchi.myobra.ui.base.BaseSeccionFragment
import com.uchi.myobra.ui.base.CapecoBottomSheet

class FalsopisoFragment : BaseSeccionFragment() {

    private var _binding: FragmentFalsopisoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFalsopisoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = PreciosRepository.getInstance(requireContext())

        val opts = CapecoDatos.concretoFalsoPiso.map { it.proporcion }
        binding.dropProporcion.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, opts))
        binding.dropProporcion.setText(opts[0], false)

        binding.btnTablaCapeco.setOnClickListener {
            CapecoBottomSheet.newInstance("falsopiso").show(childFragmentManager, "capeco_fp")
        }
        binding.btnCalcular.setOnClickListener { calcular() }
        binding.btnLimpiar.setOnClickListener  { limpiar() }

        obraViewModel.falsopiso.observe(viewLifecycleOwner) { r ->
            if (r != null && r.isValid) { mostrarResultadosCard(requireView(), r); mostrarPresupuestoCard(requireView(), r) }
        }
    }

    private fun calcular() {
        val area    = binding.etArea.text.toString().toDoubleOrNull()    ?: run { showError(); return }
        val espesor = binding.etEspesor.text.toString().toDoubleOrNull() ?: run { showError(); return }

        val propStr = binding.dropProporcion.text.toString()
        val coef = CapecoDatos.concretoFalsoPiso.firstOrNull { it.proporcion == propStr }
            ?: CapecoDatos.concretoFalsoPiso[0]
        val p   = repo.getPrecios()
        val vol = area * (espesor / 100.0)

        val cem  = vol * coef.cemento;  val hor = vol * coef.hormigon;  val agu = vol * coef.agua
        val cCem = cem * p.cementoBolsa
        val cHor = hor * p.hormigonM3
        val cAgu = agu * p.aguaM3
        val cMO  = vol * (CapecoDatos.MO_CONCRETO_OPERARIO_HH_M3 * p.moOperario +
                          CapecoDatos.MO_CONCRETO_OFICIAL_HH_M3  * p.moOficial  +
                          CapecoDatos.MO_CONCRETO_PEON_HH_M3     * p.moPeon)
        val totMat = cCem + cHor + cAgu

        val r = ResultadoCalculo(
            volumenM3 = vol, cemento = cem, hormigon = hor, agua = agu,
            costoCemento = cCem, costoArena = cHor, costoAgua = cAgu,
            costoManoObra = cMO, totalMateriales = totMat, totalObra = totMat + cMO,
            isValid = true,
            descripcion = "Falso Piso ${coef.proporcion}, e=${espesor}cm (${fmt.format(area)} m²)"
        )
        obraViewModel.guardarSeccion("falsopiso", r)
        mostrarResultadosCard(requireView(), r)
        mostrarPresupuestoCard(requireView(), r)
    }

    private fun limpiar() {
        binding.etArea.text?.clear()
        binding.etEspesor.setText("10")
        val empty = ResultadoCalculo()
        obraViewModel.guardarSeccion("falsopiso", empty)
        mostrarResultadosCard(requireView(), empty)
        mostrarPresupuestoCard(requireView(), empty)
    }

    private fun showError() =
        Snackbar.make(requireView(), "Ingrese todos los datos requeridos", Snackbar.LENGTH_SHORT).show()

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
