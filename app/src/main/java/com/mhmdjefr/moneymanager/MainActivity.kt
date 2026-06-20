package com.mhmdjefr.moneymanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import com.mhmdjefr.moneymanager.ui.onboarding.OnboardingTooltipWithArrow
import com.mhmdjefr.moneymanager.ui.onboarding.rememberOnboardingState
import com.mhmdjefr.moneymanager.ui.settings.SettingsScreen
import com.mhmdjefr.moneymanager.ui.settings.CategorySettingsScreen
import com.mhmdjefr.moneymanager.ui.settings.*
import com.mhmdjefr.moneymanager.ui.statistic.StatsScreen
import com.mhmdjefr.moneymanager.ui.theme.*
import com.mhmdjefr.moneymanager.ui.splash.SplashScreen
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
        val manageCategoriesViewModel: ManageCategoriesViewModel by viewModels {
            ManageCategoriesViewModelFactory((application as MoneyApplication).repository)
        }

        setContent {
            MoneyManagerTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route?.substringBefore("?")

                val mainRoutes = listOf("dashboard", "statistic", "wallet", "settings")

                // Onboarding: Add-transaction button tooltip, shown once on first
                // landing on the dashboard.
                val addButtonTooltip = rememberOnboardingState("add_transaction_button")

                Box(modifier = Modifier.fillMaxSize()) {
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
                                    onClick = {
                                        if (addButtonTooltip.isVisible) addButtonTooltip.dismiss()
                                        navController.navigate("add_transaction/-1")
                                    },
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
                        startDestination = "splash",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("splash") {
                            SplashScreen(navController = navController)
                        }

                        composable("dashboard") {
                            DashboardScreen(
                                viewModel = dashboardViewModel,
                                onNavigateToEdit = { txId ->
                                    navController.navigate("add_transaction/$txId")
                                }
                            )
                        }

                        composable(
                            route = "add_transaction/{id}",
                            arguments = listOf(navArgument("id") {
                                type = NavType.IntType
                                defaultValue = -1
                            })
                        ) { backStackEntry ->
                            val txId = backStackEntry.arguments?.getInt("id") ?: -1
                            AddTransactionScreen(
                                viewModel = addTransactionViewModel,
                                transactionId = txId,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                        composable("manage_categories") {
                            ManageCategoriesScreen(
                                viewModel = manageCategoriesViewModel,
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        composable("wallet") { WalletScreen(viewModel = dashboardViewModel) }
                        composable("statistic") {
                            StatsScreen(viewModel = dashboardViewModel)
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = dashboardViewModel,
                                onNavigate = { route -> navController.navigate(route) }
                            )
                        }
                        composable("profile") { ProfileScreen(onBackClick = { navController.popBackStack() }) }
                        composable("privacy") { PrivacyScreen(onBackClick = { navController.popBackStack() }) }
                        composable("about") { AboutScreen(onBackClick = { navController.popBackStack() }) }
                    }
                }

                // Overlay tooltip Add-transaction, mengambang independen di atas
                // bottom nav sehingga tidak mempengaruhi tinggi NavigationBarItem.
                if (currentRoute == "dashboard") {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 84.dp)
                    ) {
                        OnboardingTooltipWithArrow(
                            visible = addButtonTooltip.isVisible,
                            message = "Tap here to add a new transaction",
                            onDismiss = { addButtonTooltip.dismiss() }
                        )
                    }
                }
                }
            }
        }
    }
}
