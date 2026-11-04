package com.example.catheavenapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.catheavenapp.databinding.ActivityAdminBinding
import com.google.android.material.appbar.MaterialToolbar

class AdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Every admin screen except the dashboard shows a back arrow to the dashboard
        val navController = (supportFragmentManager
            .findFragmentById(R.id.adminFragmentContainer) as NavHostFragment).navController
        findViewById<MaterialToolbar>(R.id.adminTopBar)
            .setupWithNavController(navController, AppBarConfiguration(setOf(R.id.adminFragment)))
    }
}
