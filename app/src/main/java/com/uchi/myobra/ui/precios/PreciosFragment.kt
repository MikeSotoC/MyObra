package com.uchi.myobra.ui.precios

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.snackbar.Snackbar
import com.uchi.myobra.data.MaterialPrecios
import com.uchi.myobra.data.PreciosRepository
import com.uchi.myobra.databinding.FragmentPreciosBinding
import com.uchi.myobra.ui.base.BaseSeccionFragment
import java.util.Locale

class PreciosFragment : BaseSeccionFragment() {

    private var _b: FragmentPreciosBinding? = null
    private val b get() = _b!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentPreciosBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = PreciosRepository.getInstance(requireContext())
        load()
        b.btnGuardar.setOnClickListener   { save() }
        b.btnRestaurar.setOnClickListener { reset() }
    }

    private fun load() {
        val p = repo.getPrecios()
        fun Double.f() = String.format(Locale.US, "%.2f", this)
        // Materiales generales
        b.etCemento.setText(p.cementoBolsa.f())
        b.etArena.setText(p.arenaM3.f())
        b.etPiedra.setText(p.piedraM3.f())
        b.etAgua.setText(p.aguaM3.f())
        b.etHormigon.setText(p.hormigonM3.f())
        b.etPiedraGrande.setText(p.piedraGrandM3.f())
        b.etLadrilloKK.setText(p.ladrilloKK.f())
        b.etLadrilloHueco.setText(p.ladrilloHueco.f())
        // Acero por diámetro
        b.etAcero6mm.setText(p.acero6mm.f())
        b.etAcero8mm.setText(p.acero8mm.f())
        b.etAcero38.setText(p.acero3_8.f())
        b.etAcero12.setText(p.acero1_2.f())
        b.etAcero58.setText(p.acero5_8.f())
        b.etAcero34.setText(p.acero3_4.f())
        b.etAcero1.setText(p.acero1.f())
        // MO
        b.etMoOperario.setText(p.moOperario.f())
        b.etMoOficial.setText(p.moOficial.f())
        b.etMoPeon.setText(p.moPeon.f())
    }

    private fun save() {
        fun String.d() = toDoubleOrNull() ?: 0.0
        val new = MaterialPrecios(
            cementoBolsa    = b.etCemento.text.toString().d(),
            arenaM3         = b.etArena.text.toString().d(),
            piedraM3        = b.etPiedra.text.toString().d(),
            aguaM3          = b.etAgua.text.toString().d(),
            hormigonM3      = b.etHormigon.text.toString().d(),
            piedraGrandM3   = b.etPiedraGrande.text.toString().d(),
            ladrilloKK      = b.etLadrilloKK.text.toString().d(),
            ladrilloHueco   = b.etLadrilloHueco.text.toString().d(),
            ladrilloCaseton = repo.getPrecios().ladrilloCaseton,
            moOperario      = b.etMoOperario.text.toString().d(),
            moOficial       = b.etMoOficial.text.toString().d(),
            moPeon          = b.etMoPeon.text.toString().d(),
            acero6mm        = b.etAcero6mm.text.toString().d(),
            acero8mm        = b.etAcero8mm.text.toString().d(),
            acero3_8        = b.etAcero38.text.toString().d(),
            acero1_2        = b.etAcero12.text.toString().d(),
            acero5_8        = b.etAcero58.text.toString().d(),
            acero3_4        = b.etAcero34.text.toString().d(),
            acero1          = b.etAcero1.text.toString().d(),
        )
        repo.savePrecios(new)
        Snackbar.make(requireView(), "✓ Precios guardados correctamente", Snackbar.LENGTH_SHORT).show()
    }

    private fun reset() {
        repo.resetDefault()
        load()
        Snackbar.make(requireView(), "Precios restaurados a valores por defecto", Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
