package com.gastrosan.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.gastrosan.R
import com.gastrosan.databinding.ActivityMenuBinding


class MenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuBinding
    private lateinit var toolbar: Toolbar
    private var userEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        toolbar = binding.toolbar
        setSupportActionBar(toolbar)

        // Obtener el correo electrónico del Intent
        userEmail = intent.getStringExtra("email")
        println("Email en Menu: $userEmail")

        val navController = findNavController(R.id.nav_host_fragment_activity_menu)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_camera, R.id.navigation_dashboard
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)

        toolbar.setNavigationOnClickListener {
            Toast.makeText(this, "Perfil", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, ProfileActivity::class.java).apply {
                // Pasar el correo electrónico a ProfileActivity
                putExtra("email", userEmail)
            })
        }
    }
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_menu)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_nav_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.profile -> {
                // Abre la actividad de perfil aquí
                Toast.makeText(this, "Perfil", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, ProfileActivity::class.java).apply {
                    // Pasar el correo electrónico a ProfileActivity
                    putExtra("email", userEmail)
                })
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}