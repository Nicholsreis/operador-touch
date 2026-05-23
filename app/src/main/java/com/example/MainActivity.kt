package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.OperatorDashboardScreen
import com.example.ui.viewmodel.OperatorViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val operatorViewModel: OperatorViewModel = viewModel()
                NavHost(navController = navController, startDestination = "login") {
                    composable("login") {
                        LoginScreen(
                            viewModel = operatorViewModel,
                            onLoginSuccess = {
                                operatorViewModel.carregarFila()
                                navController.navigate("dashboard") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("dashboard") {
                        OperatorDashboardScreen(
                            viewModel = operatorViewModel,
                            onLogout = {
                                navController.navigate("login") {
                                    popUpTo("dashboard") { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
