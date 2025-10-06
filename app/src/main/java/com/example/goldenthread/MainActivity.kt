package com.example.goldenthread

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.goldenthread.databinding.ActivityMainBinding
import com.example.goldenthread.util.Favoritemanager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize FavoriteManager
        Favoritemanager.init(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load default fragment (Home)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, HomeFragment())
            .commit()

        // BottomNavigationView listener
        binding.bottomNav.setOnItemSelectedListener { item ->
            val selectedFragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_explore -> ExploreFragment()
                R.id.nav_thread -> ThreadFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> ExploreFragment()
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, selectedFragment)
                .commit()

            true
        }
    }

    // Helper to hide/show BottomNavigationView
    fun setBottomNavVisibility(isVisible: Boolean) {
        binding.bottomNav.visibility = if (isVisible) View.VISIBLE else View.GONE
    }
}
