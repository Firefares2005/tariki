package com.example.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.utils.LanguageUtils
import com.example.core.config.SessionManager
import com.example.presentation.auth.PhoneNumberScreen
import com.example.presentation.driver.DriverHomeScreen
import com.example.presentation.passenger.PassengerHomeScreen
import com.example.presentation.profile.ProfileScreen
import com.example.presentation.setup.DriverCarSetupScreen
import com.example.presentation.setup.ProfileSetupScreen

@Composable
fun TarikiNavGraph() {
    val context = LocalContext.current
    val navController = rememberNavController()
    
    // Track runtime language code state initialized from cached preferences
    var currentLanguageCode by remember { 
        mutableStateOf(LanguageUtils.getSavedLanguage(context)) 
    }

    // Effect to apply language changes dynamically
    LaunchedEffect(currentLanguageCode) {
        LanguageUtils.setLocale(context, currentLanguageCode)
    }

    // Start destination detection - navigate directly to home if logged in
    var startDestination by remember { mutableStateOf("phone_number") }
    LaunchedEffect(Unit) {
        val userSession = SessionManager.getUser()
        if (userSession != null) {
            val (_, userType, phone) = userSession
            startDestination = "home/$phone/$userType"
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("phone_number") {
            PhoneNumberScreen(
                currentLanguageCode = currentLanguageCode,
                onLanguageSelected = { nextLang ->
                    currentLanguageCode = nextLang
                },
                onNavigateToProfile = { phone, userType ->
                    navController.navigate("profile_setup/$phone/$userType")
                },
                onNavigateToHome = { phone, userType ->
                    navController.navigate("home/$phone/$userType") {
                        popUpTo("phone_number") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "profile_setup/{phone}/{userType}",
            arguments = listOf(
                navArgument("phone") { type = NavType.StringType },
                navArgument("userType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            val userType = backStackEntry.arguments?.getString("userType") ?: "passenger"
            ProfileSetupScreen(
                phone = phone,
                userType = userType,
                onNavigateToHome = { p, u ->
                    navController.navigate("home/$p/$u") {
                        popUpTo("phone_number") { inclusive = true }
                    }
                },
                onNavigateToCarSetup = { p ->
                    navController.navigate("car_setup/$p")
                }
            )
        }

        composable(
            route = "car_setup/{phone}",
            arguments = listOf(navArgument("phone") { type = NavType.StringType })
        ) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            DriverCarSetupScreen(
                phone = phone,
                onNavigateToHome = { p, u ->
                    navController.navigate("home/$p/$u") {
                        popUpTo("phone_number") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "home/{phone}/{userType}",
            arguments = listOf(
                navArgument("phone") { type = NavType.StringType },
                navArgument("userType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            val userType = backStackEntry.arguments?.getString("userType") ?: "passenger"

            if (userType == "driver") {
                DriverHomeScreen(
                    phone = phone,
                    onLogout = {
                        navController.navigate("phone_number") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToProfile = {
                        navController.navigate("profile_view")
                    }
                )
            } else {
                PassengerHomeScreen(
                    phone = phone,
                    onLogout = {
                        navController.navigate("phone_number") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToProfile = {
                        navController.navigate("profile_view")
                    }
                )
            }
        }

        composable("profile_view") {
            ProfileScreen(
                onBack = {
                    navController.navigateUp()
                }
            )
        }
    }
}
