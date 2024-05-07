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
import com.google.firebase.auth.FirebaseAuth


class MenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuBinding
    private lateinit var toolbar: Toolbar
    private var userEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.getBooleanExtra("navigateToCameraFragment", false)) {
            val navController = findNavController(R.id.nav_host_fragment_activity_menu)
            navController.navigate(R.id.navigation_camera)
        }

        toolbar = binding.toolbar
        setSupportActionBar(toolbar)

        // Obtener el correo electrónico del Intent
        //userEmail = intent.getStringExtra("email")
        val currentUser = FirebaseAuth.getInstance().currentUser
        userEmail = currentUser?.email
        println("User en Menu: $currentUser")
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

        binding.navView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    navController.popBackStack(R.id.navigation_home, false)
                    true
                }
                R.id.navigation_camera -> {
                    if (navController.currentDestination?.id != R.id.navigation_camera) {
                        navController.navigate(R.id.navigation_camera)
                    }
                    true
                }
                R.id.navigation_dashboard -> {
                    if (navController.currentDestination?.id != R.id.navigation_dashboard) {
                        navController.navigate(R.id.navigation_dashboard)
                    }
                    true
                }
                else -> false
            }
        }


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
    override fun onBackPressed() {
        val navController = findNavController(R.id.nav_host_fragment_activity_menu)
        if (navController.currentDestination?.id == R.id.navigation_home) {
            super.onBackPressed()
        } else {
            navController.popBackStack(R.id.navigation_home, false)
        }
    }


}