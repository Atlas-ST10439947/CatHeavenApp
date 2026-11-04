package com.example.catheavenapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragmentContainer) as NavHostFragment
        navController = navHostFragment.navController

        // The five tabs are top-level screens; everything else (a cat's profile, Personal
        // Profile, the booking steps) shows a back arrow in the top bar
        val topLevel = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_cats, R.id.nav_menu, R.id.nav_booking, R.id.nav_profile)
        )
        findViewById<MaterialToolbar>(R.id.mainTopBar).setupWithNavController(navController, topLevel)

        // Keeps the highlighted tab in sync with the current screen
        bottomNavigationView.setupWithNavController(navController)

        // Opening a tab always shows that tab's main page, instead of the sub-page it was
        // left on (e.g. Profile opens Settings, not Personal Profile)
        bottomNavigationView.setOnItemSelectedListener { item ->
            openTab(item.itemId)
            true
        }
        // Tapping the tab you're already on goes back to its main page too
        bottomNavigationView.setOnItemReselectedListener { item ->
            navController.popBackStack(item.itemId, false)
        }
    }

    /** Also used by Home's "Rescue Cats" card to open the Residents tab. */
    fun openTab(destinationId: Int) {
        if (navController.currentDestination?.id == destinationId) return
        navController.navigate(destinationId, null, navOptions {
            launchSingleTop = true
            restoreState = false
            popUpTo(navController.graph.findStartDestination().id) { saveState = false }
        })
    }
}
