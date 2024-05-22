package com.gastrosan.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
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

        val currentUser = FirebaseAuth.getInstance().currentUser
        userEmail = currentUser?.email
        println("User en Menu: $currentUser")
        println("Email en Menu: $userEmail")

        val navController = findNavController(R.id.nav_host_fragment_activity_menu)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_camera, R.id.navigation_dashboard
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)

        findViewById<ImageView>(R.id.profile_icon).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java).apply {
                putExtra("email", userEmail)
            })
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_menu)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return true // No inflamos el menú ya que no necesitamos `top_nav_menu`
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        val navController = findNavController(R.id.nav_host_fragment_activity_menu)
        if (!navController.navigateUp()) {
            super.onBackPressed()
        }
    }
}
