package com.uchi.myobra.ui.vigas

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
import com.uchi.myobra.databinding.FragmentVigasBinding
import com.uchi.myobra.ui.base.AceroMetradoHelper
import com.uchi.myobra.ui.base.BaseSeccionFragment
import com.uchi.myobra.ui.base.CapecoBottomSheet

class VigasFragment : BaseSeccionFragment() {

    private var _binding: FragmentVigasBinding? = null
    private val binding get() = _binding!!
    private lateinit var aceroHelper: AceroMetradoHelper

    private var filasAcero: List<FilaAcero> = emptyList()
    private var aceroTotalKg    = 0.0
    private var aceroTotalCosto = 0.0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVigasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = PreciosRepository.getInstance(requireContext())

        val fcOpts = CapecoDatos.concretoNormal.map { "f'c = ${it.fc} kg/cm² (${it.prop})" }
        binding.dropFc.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, fcOpts))
        binding.dropFc.setText(fcOpts[2], false)

        aceroHelper = AceroMetradoHelper(
            context      = requireContext(),
            container    = view.findViewById(R.id.containerFilasAcero),
            repo         = repo
        ) { filas, kg, costo -> filasAcero = filas; aceroTotalKg = kg; aceroTotalCosto = costo }
        aceroHelper.addFila()

        view.findViewById<View>(R.id.btnAgregarBarra).setOnClickListener { aceroHelper.addFila() }
        view.findViewById<View>(R.id.btnVerTablaAcero).setOnClickListener {
            CapecoBottomSheet.newInstance("acero").show(childFragmentManager, "capeco_acero")
        }
        binding.btnTablaCapeco.setOnClickListener {
            CapecoBottomSheet.newInstance("normal").show(childFragmentManager, "capeco_normal")
        }
        binding.btnCalcular.setOnClickListener { calcular() }
        binding.btnLimpiar.setOnClickListener  { limpiar() }

        obraViewModel.vigas.observe(viewLifecycleOwner) { r ->
            if (r != null && r.isValid) { mostrarResultadosCard(requireView(), r); mostrarPresupuestoCard(requireView(), r) }
        }
    }

    private fun calcular() {
        val nVigas   = binding.etNumVigas.text.toString().toDoubleOrNull()  ?: run { showError("Ingrese N° de vigas"); return }
        val base     = binding.etBase.text.toString().toDoubleOrNull()       ?: run { showError("Ingrese base"); return }
        val peralte  = binding.etPeralte.text.toString().toDoubleOrNull()    ?: run { showError("Ingrese peralte"); return }
        val longitud = binding.etLongitud.text.toString().toDoubleOrNull()   ?: run { showError("Ingrese longitud"); return }

        if (aceroHelper.isEmpty() || aceroTotalKg == 0.0) {
            showError("Ingresa las barras de acero del plano estructural")
            return
        }

        val idx  = CapecoDatos.concretoNormal.indexOfFirst { binding.dropFc.text.toString().contains(it.fc.toString()) }.coerceAtLeast(0)
        val coef = CapecoDatos.concretoNormal[idx]
        val p    = repo.getPrecios()
        val vol  = nVigas * base * peralte * longitud

        val cem = vol * coef.cemento; val are = vol * coef.arena
        val pie = vol * coef.piedra;  val agu = vol * coef.agua
        val cCem = cem * p.cementoBolsa; val cAre = are * p.arenaM3
        val cPie = pie * p.piedraM3;    val cAgu = agu * p.aguaM3
        val cMO  = vol * (CapecoDatos.MO_CONCRETO_OPERARIO_HH_M3 * p.moOperario +
                          CapecoDatos.MO_CONCRETO_OFICIAL_HH_M3  * p.moOficial  +
                          CapecoDatos.MO_CONCRETO_PEON_HH_M3     * p.moPeon)
        val totMat = cCem + cAre + cPie + cAgu + aceroTotalCosto

        val r = ResultadoCalculo(
            volumenM3 = vol, cemento = cem, arena = are, piedra = pie, agua = agu,
            filasAcero = filasAcero, aceroTotalKg = aceroTotalKg,
            costoCemento = cCem, costoArena = cAre, costoPiedra = cPie, costoAgua = cAgu,
            costoAcero = aceroTotalCosto, costoManoObra = cMO,
            totalMateriales = totMat, totalObra = totMat + cMO,
            isValid = true,
            descripcion = "Vigas (${nVigas.toInt()} und, b=${base}m, h=${peralte}m, L=${longitud}m, f'c=${coef.fc})"
        )
        obraViewModel.guardarSeccion("vigas", r)
        mostrarResultadosCard(requireView(), r); mostrarPresupuestoCard(requireView(), r)
    }

    private fun limpiar() {
        binding.etNumVigas.setText("1")
        binding.etBase.text?.clear(); binding.etPeralte.text?.clear(); binding.etLongitud.text?.clear()
        aceroHelper.clear(); aceroHelper.addFila()
        aceroTotalKg = 0.0; aceroTotalCosto = 0.0; filasAcero = emptyList()
        val empty = ResultadoCalculo()
        obraViewModel.guardarSeccion("vigas", empty)
        mostrarResultadosCard(requireView(), empty); mostrarPresupuestoCard(requireView(), empty)
    }

    private fun showError(msg: String) = Snackbar.make(requireView(), msg, Snackbar.LENGTH_LONG).show()
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
