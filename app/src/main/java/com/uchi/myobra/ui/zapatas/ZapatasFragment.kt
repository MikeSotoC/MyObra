package com.uchi.myobra.ui.zapatas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.google.android.material.snackbar.Snackbar
import com.uchi.myobra.data.CapecoDatos
import com.uchi.myobra.data.FilaAcero
import com.uchi.myobra.data.PreciosRepository
import com.uchi.myobra.data.ResultadoCalculo
import com.uchi.myobra.databinding.FragmentZapatasBinding
import com.uchi.myobra.ui.base.AceroMetradoHelper
import com.uchi.myobra.ui.base.BaseSeccionFragment
import com.uchi.myobra.ui.base.CapecoBottomSheet

class ZapatasFragment : BaseSeccionFragment() {

    private var _binding: FragmentZapatasBinding? = null
    private val binding get() = _binding!!
    private lateinit var aceroHelper: AceroMetradoHelper

    // Estado local del acero
    private var filasAcero: List<FilaAcero> = emptyList()
    private var aceroTotalKg   = 0.0
    private var aceroTotalCosto= 0.0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentZapatasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = PreciosRepository.getInstance(requireContext())

        // f'c dropdown
        val fcOpts = CapecoDatos.concretoNormal.map { "f'c = ${it.fc} kg/cm² (${it.prop})" }
        binding.dropFc.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, fcOpts))
        binding.dropFc.setText(fcOpts[2], false)

        // Acero metrado helper — los IDs ahora están dentro de la card de datos
        aceroHelper = AceroMetradoHelper(
            context      = requireContext(),
            container    = view.findViewById(com.uchi.myobra.R.id.containerFilasAcero),
            repo         = repo
        ) { filas, kg, costo ->
            filasAcero      = filas
            aceroTotalKg    = kg
            aceroTotalCosto = costo
        }
        aceroHelper.addFila()

        view.findViewById<View>(com.uchi.myobra.R.id.btnAgregarBarra)
            .setOnClickListener { aceroHelper.addFila() }
        view.findViewById<View>(com.uchi.myobra.R.id.btnVerTablaAcero)
            .setOnClickListener {
                CapecoBottomSheet.newInstance("acero")
                    .show(childFragmentManager, "capeco_acero")
            }

        binding.btnTablaCapeco.setOnClickListener {
            CapecoBottomSheet.newInstance("normal").show(childFragmentManager, "capeco_normal")
        }

        binding.btnCalcular.setOnClickListener { calcular() }
        binding.btnLimpiar.setOnClickListener  { limpiar() }

        // Observe LiveData — carga desde Room cuando abre proyecto
        obraViewModel.zapatas.observe(viewLifecycleOwner) { r ->
            if (r != null && r.isValid) {
                mostrarResultadosCard(requireView(), r)
                mostrarPresupuestoCard(requireView(), r)
            }
        }
    }

    private fun calcular() {
        val n    = binding.etNumZapatas.text.toString().toDoubleOrNull() ?: run { showError("Ingrese número de zapatas"); return }
        val largo= binding.etLargo.text.toString().toDoubleOrNull() ?: run { showError("Ingrese el largo"); return }
        val ancho= binding.etAncho.text.toString().toDoubleOrNull() ?: run { showError("Ingrese el ancho"); return }
        val alto = binding.etAlto.text.toString().toDoubleOrNull() ?: run { showError("Ingrese el alto/peralte"); return }

        if (aceroHelper.isEmpty() || aceroTotalKg == 0.0) {
            showError("Ingresa al menos una barra de acero del plano estructural")
            return
        }

        val idx  = CapecoDatos.concretoNormal.indexOfFirst { binding.dropFc.text.toString().contains(it.fc.toString()) }.coerceAtLeast(0)
        val coef = CapecoDatos.concretoNormal[idx]
        val p    = repo.getPrecios()
        val vol  = n * largo * ancho * alto

        val cem  = vol * coef.cemento
        val are  = vol * coef.arena
        val pie  = vol * coef.piedra
        val agu  = vol * coef.agua
        val cCem = cem * p.cementoBolsa
        val cAre = are * p.arenaM3
        val cPie = pie * p.piedraM3
        val cAgu = agu * p.aguaM3
        val cMO  = vol * (CapecoDatos.MO_CONCRETO_OPERARIO_HH_M3 * p.moOperario +
                          CapecoDatos.MO_CONCRETO_OFICIAL_HH_M3  * p.moOficial  +
                          CapecoDatos.MO_CONCRETO_PEON_HH_M3     * p.moPeon)
        val totMat = cCem + cAre + cPie + cAgu + aceroTotalCosto

        val resultado = ResultadoCalculo(
            volumenM3       = vol,
            cemento         = cem,
            arena           = are,
            piedra          = pie,
            agua            = agu,
            filasAcero      = filasAcero,
            aceroTotalKg    = aceroTotalKg,
            costoCemento    = cCem,
            costoArena      = cAre,
            costoPiedra     = cPie,
            costoAgua       = cAgu,
            costoAcero      = aceroTotalCosto,
            costoManoObra   = cMO,
            totalMateriales = totMat,
            totalObra       = totMat + cMO,
            isValid         = true,
            descripcion     = "Zapatas (${n.toInt()} und, ${largo}×${ancho}×${alto}m, f'c=${coef.fc})"
        )

        obraViewModel.guardarSeccion("zapatas", resultado)
        mostrarResultadosCard(requireView(), resultado)
        mostrarPresupuestoCard(requireView(), resultado)
    }

    private fun limpiar() {
        binding.etNumZapatas.setText("1")
        binding.etLargo.text?.clear()
        binding.etAncho.text?.clear()
        binding.etAlto.text?.clear()
        aceroHelper.clear()
        aceroHelper.addFila()
        aceroTotalKg    = 0.0
        aceroTotalCosto = 0.0
        filasAcero      = emptyList()
        val empty = ResultadoCalculo()
        obraViewModel.guardarSeccion("zapatas", empty)
        mostrarResultadosCard(requireView(), empty)
        mostrarPresupuestoCard(requireView(), empty)
    }

    private fun showError(msg: String) =
        Snackbar.make(requireView(), msg, Snackbar.LENGTH_LONG).show()

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
