package com.uchi.myobra

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.uchi.myobra.data.ObraViewModel
import com.uchi.myobra.databinding.ActivityMainBinding
import com.uchi.myobra.util.ThemeManager

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val obraVm: ObraViewModel by viewModels {
        androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView    = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        // All top-level destinations (no back arrow)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_proyectos,
                R.id.nav_zapatas,   R.id.nav_cimientos,    R.id.nav_sobrecimiento,
                R.id.nav_muros,     R.id.nav_falsopiso,    R.id.nav_columnas,
                R.id.nav_vigas,     R.id.nav_losas,
                R.id.nav_presupuesto, R.id.nav_precios
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Show current project name as toolbar subtitle
        obraVm.proyectoActivo.observe(this) { proyecto ->
            supportActionBar?.subtitle = proyecto?.nombre ?: ""
        }

        // Update theme icon when destination changes
        navController.addOnDestinationChangedListener { _, _, _ ->
            invalidateOptionsMenu()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        val isDark = ThemeManager.isDark(this)
        menu.findItem(R.id.action_toggle_theme)?.title =
            if (isDark) "Tema claro ☀" else "Tema oscuro 🌙"
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_toggle_theme) {
            ThemeManager.toggle(this)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
