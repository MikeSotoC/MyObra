package com.uchi.myobra.data

import android.content.Context
import android.content.SharedPreferences

class PreciosRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("myobra_precios", Context.MODE_PRIVATE)

    fun getPrecios(): MaterialPrecios {
        val d = MaterialPrecios()
        return MaterialPrecios(
            cementoBolsa    = prefs.getFloat("cemento",      d.cementoBolsa.toFloat()).toDouble(),
            arenaM3         = prefs.getFloat("arena",        d.arenaM3.toFloat()).toDouble(),
            piedraM3        = prefs.getFloat("piedra",       d.piedraM3.toFloat()).toDouble(),
            aguaM3          = prefs.getFloat("agua",         d.aguaM3.toFloat()).toDouble(),
            hormigonM3      = prefs.getFloat("hormigon",     d.hormigonM3.toFloat()).toDouble(),
            piedraGrandM3   = prefs.getFloat("piedraGrande", d.piedraGrandM3.toFloat()).toDouble(),
            ladrilloKK      = prefs.getFloat("ladrilloKK",   d.ladrilloKK.toFloat()).toDouble(),
            ladrilloHueco   = prefs.getFloat("ladrilloHueco",d.ladrilloHueco.toFloat()).toDouble(),
            ladrilloCaseton = prefs.getFloat("ladrilloCaseton",d.ladrilloCaseton.toFloat()).toDouble(),
            moOperario      = prefs.getFloat("moOperario",   d.moOperario.toFloat()).toDouble(),
            moOficial       = prefs.getFloat("moOficial",    d.moOficial.toFloat()).toDouble(),
            moPeon          = prefs.getFloat("moPeon",       d.moPeon.toFloat()).toDouble(),
            // Acero por diámetro
            acero6mm        = prefs.getFloat("acero6mm",     d.acero6mm.toFloat()).toDouble(),
            acero8mm        = prefs.getFloat("acero8mm",     d.acero8mm.toFloat()).toDouble(),
            acero3_8        = prefs.getFloat("acero3_8",     d.acero3_8.toFloat()).toDouble(),
            acero1_2        = prefs.getFloat("acero1_2",     d.acero1_2.toFloat()).toDouble(),
            acero5_8        = prefs.getFloat("acero5_8",     d.acero5_8.toFloat()).toDouble(),
            acero3_4        = prefs.getFloat("acero3_4",     d.acero3_4.toFloat()).toDouble(),
            acero1          = prefs.getFloat("acero1",       d.acero1.toFloat()).toDouble(),
        )
    }

    fun savePrecios(p: MaterialPrecios) {
        prefs.edit().apply {
            putFloat("cemento",       p.cementoBolsa.toFloat())
            putFloat("arena",         p.arenaM3.toFloat())
            putFloat("piedra",        p.piedraM3.toFloat())
            putFloat("agua",          p.aguaM3.toFloat())
            putFloat("hormigon",      p.hormigonM3.toFloat())
            putFloat("piedraGrande",  p.piedraGrandM3.toFloat())
            putFloat("ladrilloKK",    p.ladrilloKK.toFloat())
            putFloat("ladrilloHueco", p.ladrilloHueco.toFloat())
            putFloat("ladrilloCaseton",p.ladrilloCaseton.toFloat())
            putFloat("moOperario",    p.moOperario.toFloat())
            putFloat("moOficial",     p.moOficial.toFloat())
            putFloat("moPeon",        p.moPeon.toFloat())
            putFloat("acero6mm",      p.acero6mm.toFloat())
            putFloat("acero8mm",      p.acero8mm.toFloat())
            putFloat("acero3_8",      p.acero3_8.toFloat())
            putFloat("acero1_2",      p.acero1_2.toFloat())
            putFloat("acero5_8",      p.acero5_8.toFloat())
            putFloat("acero3_4",      p.acero3_4.toFloat())
            putFloat("acero1",        p.acero1.toFloat())
            apply()
        }
    }

    fun resetDefault() = savePrecios(MaterialPrecios())

    /** Precio S/./kg para un diámetro dado */
    fun getPrecioAcero(diam: String, p: MaterialPrecios = getPrecios()): Double = when {
        diam.contains("6mm")  -> p.acero6mm
        diam.contains("8mm")  -> p.acero8mm
        diam.contains("3/8")  -> p.acero3_8
        diam.contains("1/2")  -> p.acero1_2
        diam.contains("5/8")  -> p.acero5_8
        diam.contains("3/4")  -> p.acero3_4
        diam.contains("1\"")  -> p.acero1
        else                  -> p.acero1_2
    }

    companion object {
        @Volatile private var INSTANCE: PreciosRepository? = null
        fun getInstance(context: Context): PreciosRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreciosRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
