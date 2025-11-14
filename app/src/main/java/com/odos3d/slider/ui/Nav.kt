package com.odos3d.slider.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.odos3d.slider.ble.BluetoothConnector
import com.odos3d.slider.ui.screens.AjustesScreen
import com.odos3d.slider.ui.screens.AvanzadoScreen
import com.odos3d.slider.ui.screens.BluetoothScreen
import com.odos3d.slider.ui.screens.HomeScreen
import com.odos3d.slider.ui.screens.TimelapseScreen

private const val ROUTE_HOME = "home"
private const val ROUTE_ADVANCED = "advanced"
private const val ROUTE_BLUETOOTH = "bluetooth"
private const val ROUTE_TIMELAPSE = "timelapse"
private const val ROUTE_SETTINGS = "settings"

data class SliderDestination(val route: String, val label: String)

private val bottomDestinations = listOf(
    SliderDestination(ROUTE_HOME, "Home"),
    SliderDestination(ROUTE_ADVANCED, "Avanzado"),
    SliderDestination(ROUTE_BLUETOOTH, "Bluetooth"),
    SliderDestination(ROUTE_TIMELAPSE, "Timelapse"),
    SliderDestination(ROUTE_SETTINGS, "Ajustes")
)

@Composable
fun SliderNavHost(
    modifier: Modifier = Modifier,
    startDestination: String = ROUTE_HOME
) {
    val context = LocalContext.current
    val bluetoothConnector = remember { BluetoothConnector(context) }
    val navController = rememberNavController()

    DisposableEffect(Unit) {
        onDispose {
            bluetoothConnector.close()
        }
    }

    Scaffold(
        bottomBar = {
            SliderBottomBar(navController = navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier.padding(innerPadding)
        ) {
            sliderNavGraph(bluetoothConnector = bluetoothConnector)
        }
    }
}

private fun NavGraphBuilder.sliderNavGraph(bluetoothConnector: BluetoothConnector) {
    composable(ROUTE_HOME) {
        HomeScreen(bluetoothConnector = bluetoothConnector)
    }
    composable(ROUTE_ADVANCED) {
        AvanzadoScreen(bluetoothConnector = bluetoothConnector)
    }
    composable(ROUTE_BLUETOOTH) {
        BluetoothScreen(bluetoothConnector = bluetoothConnector)
    }
    composable(ROUTE_TIMELAPSE) {
        TimelapseScreen(bluetoothConnector = bluetoothConnector)
    }
    composable(ROUTE_SETTINGS) {
        AjustesScreen()
    }
}

@Composable
private fun SliderBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        bottomDestinations.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Text(destination.label.first().uppercase())
                },
                label = {
                    Text(destination.label)
                }
            )
        }
    }
}
