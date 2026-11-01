package com.example.basketballtracker.app.navigation

import com.example.basketballtracker.ui.theme.SportsColors
import android.net.http.SslCertificate.restoreState
import android.net.http.SslCertificate.saveState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        BottomNavItem("Home", "home", Icons.Default.Home),
        BottomNavItem("Games", "history", Icons.Default.CalendarToday),
        BottomNavItem("Players", "players", Icons.Default.Person),
        BottomNavItem("Stats", "stats", Icons.Default.BarChart)
    )

    NavigationBar(
        containerColor = SportsColors.Surface,
        tonalElevation = 0.dp
    ) {
        val currentRoute = navController
            .currentBackStackEntryAsState()
            .value
            ?.destination
            ?.route

        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(item.icon, contentDescription = item.title)
                },
                label = {
                    Text(item.title, fontWeight = if (currentRoute == item.route) FontWeight.Bold else FontWeight.Medium)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = SportsColors.Accent,
                    selectedTextColor = SportsColors.Accent,
                    unselectedIconColor = SportsColors.Muted,
                    unselectedTextColor = SportsColors.Muted,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: ImageVector
)
