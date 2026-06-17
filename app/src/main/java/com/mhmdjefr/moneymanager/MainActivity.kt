package com.mhmdjefr.moneymanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mhmdjefr.moneymanager.ui.dashboard.DashboardScreen
import com.mhmdjefr.moneymanager.ui.dashboard.DashboardViewModel
import com.mhmdjefr.moneymanager.ui.dashboard.DashboardViewModelFactory
import com.mhmdjefr.moneymanager.ui.settings.SettingsScreen
import com.mhmdjefr.moneymanager.ui.settings.CategorySettingsScreen
import com.mhmdjefr.moneymanager.ui.statistic.StatisticScreen
import com.mhmdjefr.moneymanager.ui.theme.*
import com.mhmdjefr.moneymanager.ui.transaction.AddTransactionScreen
import com.mhmdjefr.moneymanager.ui.transaction.AddTransactionViewModel
import com.mhmdjefr.moneymanager.ui.transaction.AddTransactionViewModelFactory
import com.mhmdjefr.moneymanager.ui.wallet.WalletScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as MoneyApplication
        val dashboardViewModel: DashboardViewModel by viewModels { DashboardViewModelFactory(app.repository) }
        val addTransactionViewModel: AddTransactionViewModel by viewModels { AddTransactionViewModelFactory(app.repository) }

        setContent {
            MoneyManagerTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route?.substringBefore("?")

                val mainRoutes = listOf("dashboard", "statistic", "wallet", "settings")

                Scaffold(
                    bottomBar = {
                        if (currentRoute in mainRoutes) {
                            NavigationBar(
                                containerColor = CardWhite,
                                tonalElevation = 8.dp
                            ) {
                                NavigationBarItem(
                                    selected = currentRoute == "dashboard",
                                    onClick = { navController.navigate("dashboard") },
                                    icon = { Icon(imageVector = if (currentRoute == "dashboard") Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "Home") },
                                    label = { Text("Home", fontSize = 10.sp) },
                                    colors = NavigationBarItemDefaults.colors(selectedIconColor = SoftBlue, selectedTextColor = SoftBlue, unselectedIconColor = TextSecondary, unselectedTextColor = TextSecondary, indicatorColor = SoftBlue.copy(alpha = 0.1f))
                                )

                                NavigationBarItem(
                                    selected = currentRoute == "statistic",
                                    onClick = { navController.navigate("statistic") },
                                    icon = { Icon(imageVector = if (currentRoute == "statistic") Icons.Filled.PieChart else Icons.Outlined.PieChart, contentDescription = "Statistics") },
                                    label = { Text("Stats", fontSize = 10.sp) },
                                    colors = NavigationBarItemDefaults.colors(selectedIconColor = SoftBlue, selectedTextColor = SoftBlue, unselectedIconColor = TextSecondary, unselectedTextColor = TextSecondary, indicatorColor = SoftBlue.copy(alpha = 0.1f))
                                )

                                NavigationBarItem(
                                    selected = false,
                                    onClick = { navController.navigate("add_transaction?-1") },
                                    icon = {
                                        Box(modifier = Modifier.size(44.dp).background(SoftBlue, CircleShape), contentAlignment = Alignment.Center) {
                                            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Transaction", tint = Color.White, modifier = Modifier.size(24.dp))
                                        }
                                    },
                                    label = { Text("Add", fontSize = 10.sp) },
                                    colors = NavigationBarItemDefaults.colors(unselectedTextColor = TextSecondary)
                                )

                                NavigationBarItem(
                                    selected = currentRoute == "wallet",
                                    onClick = { navController.navigate("wallet") },
                                    icon = { Icon(imageVector = if (currentRoute == "wallet") Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet, contentDescription = "Wallet") },
                                    label = { Text("Wallet", fontSize = 10.sp) },
                                    colors = NavigationBarItemDefaults.colors(selectedIconColor = SoftBlue, selectedTextColor = SoftBlue, unselectedIconColor = TextSecondary, unselectedTextColor = TextSecondary, indicatorColor = SoftBlue.copy(alpha = 0.1f))
                                )

                                NavigationBarItem(
                                    selected = currentRoute == "settings",
                                    onClick = { navController.navigate("settings") },
                                    icon = { Icon(imageVector = if (currentRoute == "settings") Icons.Filled.Settings else Icons.Outlined.Settings, contentDescription = "Settings") },
                                    label = { Text("Settings", fontSize = 10.sp) },
                                    colors = NavigationBarItemDefaults.colors(selectedIconColor = SoftBlue, selectedTextColor = SoftBlue, unselectedIconColor = TextSecondary, unselectedTextColor = TextSecondary, indicatorColor = SoftBlue.copy(alpha = 0.1f))
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "dashboard",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("dashboard") { DashboardScreen(viewModel = dashboardViewModel, onNavigateToEdit = { id -> navController.navigate("add_transaction?$id") }) }
                        composable(route = "add_transaction?{id}", arguments = listOf(navArgument("id") { defaultValue = -1; type = NavType.IntType })) { backStackEntry ->
                            val id = backStackEntry.arguments?.getInt("id") ?: -1
                            AddTransactionScreen(viewModel = addTransactionViewModel, transactionId = id, onBackClick = { navController.popBackStack() })
                        }
                        composable("wallet") { WalletScreen(viewModel = dashboardViewModel) }
                        composable("statistic") { StatisticScreen(viewModel = dashboardViewModel) }
                        composable("settings") {
                            SettingsScreen(onNavigateToCategories = { navController.navigate("manage_categories") })
                        }
                        composable("manage_categories") {
                            CategorySettingsScreen(viewModel = dashboardViewModel, onBackClick = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}