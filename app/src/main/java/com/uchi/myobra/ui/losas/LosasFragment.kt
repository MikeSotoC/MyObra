package com.uchi.myobra.ui.losas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.google.android.material.snackbar.Snackbar
import com.uchi.myobra.R
import com.uchi.myobra.data.CapecoDatos
import com.uchi.myobra.data.FilaAcero
import com.uchi.myobra.data.PreciosRepository
import com.uchi.myobra.data.ResultadoCalculo
import com.uchi.myobra.databinding.FragmentLosasBinding
import com.uchi.myobra.ui.base.AceroMetradoHelper
import com.uchi.myobra.ui.base.BaseSeccionFragment
import com.uchi.myobra.ui.base.CapecoBottomSheet

class LosasFragment : BaseSeccionFragment() {

    private var _binding: FragmentLosasBinding? = null
    private val binding get() = _binding!!
    private lateinit var aceroHelper: AceroMetradoHelper

    private var filasAcero: List<FilaAcero> = emptyList()
    private var aceroTotalKg    = 0.0
    private var aceroTotalCosto = 0.0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLosasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = PreciosRepository.getInstance(requireContext())

        val alturaOpts = CapecoDatos.losasAligeradas.map { "h = ${it.altura} cm" }
        binding.dropAlturaLosa.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, alturaOpts))
        binding.dropAlturaLosa.setText(alturaOpts[1], false) // h=20cm default

        val ladrilloOpts = listOf("Ladrillo hueco", "Ladrillo casetón")
        binding.dropTipoLadrillo.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, ladrilloOpts))
        binding.dropTipoLadrillo.setText(ladrilloOpts[0], false)

        aceroHelper = AceroMetradoHelper(
            context   = requireContext(),
            container = view.findViewById(R.id.containerFilasAcero),
            repo      = repo
        ) { filas, kg, costo ->
            filasAcero      = filas
            aceroTotalKg    = kg
            aceroTotalCosto = costo
        }
        // Losas: viguetas típicas Ø3/8" y temperatura Ø1/4"
        aceroHelper.addFila(diam = CapecoDatos.barrasAcero[2].diam) // Ø3/8"
        aceroHelper.addFila(diam = CapecoDatos.barrasAcero[0].diam) // Ø6mm (temperatura)

        view.findViewById<View>(R.id.btnAgregarBarra).setOnClickListener { aceroHelper.addFila() }
        view.findViewById<View>(R.id.btnVerTablaAcero).setOnClickListener {
            CapecoBottomSheet.newInstance("acero").show(childFragmentManager, "capeco_acero")
        }
        binding.btnTablaCapeco.setOnClickListener {
            CapecoBottomSheet.newInstance("losas").show(childFragmentManager, "capeco_losas")
        }
        binding.btnCalcular.setOnClickListener { calcular() }
        binding.btnLimpiar.setOnClickListener  { limpiar() }

        obraViewModel.losas.observe(viewLifecycleOwner) { r ->
            if (r != null && r.isValid) { mostrarResultadosCard(requireView(), r); mostrarPresupuestoCard(requireView(), r) }
        }
    }

    private fun calcular() {
        val area = binding.etArea.text.toString().toDoubleOrNull() ?: run { showError(); return }

        if (aceroHelper.isEmpty() || aceroTotalKg == 0.0) {
            showError("Ingresa al menos una barra de acero (viguetas del plano)")
            return
        }

        val altStr = binding.dropAlturaLosa.text.toString()
        val altCm  = altStr.filter { it.isDigit() }.toIntOrNull() ?: 20
        val coef   = CapecoDatos.losasAligeradas.firstOrNull { it.altura == altCm }
            ?: CapecoDatos.losasAligeradas[1]

        val esCaseton = binding.dropTipoLadrillo.text.toString().contains("casetón")
        val p = repo.getPrecios()

        // Concreto por m²
        val cem    = area * coef.cemento
        val are    = area * coef.arena
        val pie    = area * coef.piedra
        val agu    = area * coef.agua
        val ladInt = (area * coef.ladrilloHueco).toInt()

        val precioLad = if (esCaseton) p.ladrilloCaseton else p.ladrilloHueco
        val cCem = cem * p.cementoBolsa
        val cAre = are * p.arenaM3
        val cPie = pie * p.piedraM3
        val cAgu = agu * p.aguaM3
        val cLad = ladInt * precioLad

        val vol  = area * (altCm / 100.0)
        val cMO  = vol * (CapecoDatos.MO_CONCRETO_OPERARIO_HH_M3 * p.moOperario +
                          CapecoDatos.MO_CONCRETO_OFICIAL_HH_M3  * p.moOficial  +
                          CapecoDatos.MO_CONCRETO_PEON_HH_M3     * p.moPeon)
        val totMat = cCem + cAre + cPie + cAgu + cLad + aceroTotalCosto

        val r = ResultadoCalculo(
            volumenM3 = area, cemento = cem, arena = are, piedra = pie, agua = agu,
            filasAcero = filasAcero, aceroTotalKg = aceroTotalKg, ladrillo = ladInt,
            costoCemento = cCem, costoArena = cAre, costoPiedra = cPie, costoAgua = cAgu,
            costoAcero = aceroTotalCosto, costoLadrillo = cLad,
            costoManoObra = cMO, totalMateriales = totMat, totalObra = totMat + cMO,
            isValid = true,
            descripcion = "Losa h=${altCm}cm, ${if (esCaseton) "casetón" else "hueco"} (${fmt.format(area)} m²)"
        )
        obraViewModel.guardarSeccion("losas", r)
        mostrarResultadosCard(requireView(), r)
        mostrarPresupuestoCard(requireView(), r)
    }

    private fun limpiar() {
        binding.etArea.text?.clear()
        aceroHelper.clear()
        aceroHelper.addFila(diam = CapecoDatos.barrasAcero[2].diam)
        aceroHelper.addFila(diam = CapecoDatos.barrasAcero[0].diam)
        aceroTotalKg = 0.0; aceroTotalCosto = 0.0; filasAcero = emptyList()
        val empty = ResultadoCalculo()
        obraViewModel.guardarSeccion("losas", empty)
        mostrarResultadosCard(requireView(), empty)
        mostrarPresupuestoCard(requireView(), empty)
    }

    private fun showError(msg: String = "Ingrese todos los datos requeridos") =
        Snackbar.make(requireView(), msg, Snackbar.LENGTH_LONG).show()

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
