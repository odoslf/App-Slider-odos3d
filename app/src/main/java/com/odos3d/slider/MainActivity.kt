package com.odos3d.slider

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import com.odos3d.slider.databinding.ActivityMainBinding
import com.odos3d.slider.ui.components.StatusChipView

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    val statusChip: StatusChipView by lazy {
        findViewById(R.id.chStatus)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        val navController = findNavController(R.id.nav_host)
        bindBottomNavigation(navController)
    }

    private fun bindBottomNavigation(navController: NavController) {
        val destinations = listOf(
            R.id.nav_basico,
            R.id.nav_avanzado,
            R.id.nav_manual,
            R.id.nav_scenes,
            R.id.galleryFragment,
            R.id.camaraFragment,
            R.id.nav_timelapse,
            R.id.nav_ajustes
        )

        binding.bottomNav.setOnItemReselectedListener { }
        binding.bottomNav.setOnItemSelectedListener { item ->
            val targetIndex = destinations.indexOf(item.itemId)
            if (targetIndex == -1) {
                return@setOnItemSelectedListener false
            }

            val currentId = navController.currentDestination?.id
            if (currentId == item.itemId) {
                return@setOnItemSelectedListener true
            }

            val currentIndex = destinations.indexOf(currentId)
            val options = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(navController.graph.startDestinationId, false)

            if (currentIndex != -1 && targetIndex < currentIndex) {
                options.setEnterAnim(R.anim.slide_in_left)
                    .setExitAnim(R.anim.slide_out_right)
                    .setPopEnterAnim(R.anim.slide_in_right)
                    .setPopExitAnim(R.anim.slide_out_left)
            } else {
                options.setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
            }

            navController.navigate(item.itemId, null, options.build())
            true
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id in destinations) {
                binding.bottomNav.menu.findItem(destination.id)?.isChecked = true
            }
        }
    }
}
