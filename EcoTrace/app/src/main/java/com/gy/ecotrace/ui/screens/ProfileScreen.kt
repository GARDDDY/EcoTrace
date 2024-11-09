package com.gy.ecotrace.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gy.ecotrace.sections.profile.AuthViewModel
import com.gy.ecotrace.ui.common.BottomNavigationBar
import com.gy.ecotrace.utils.Screen

@Composable
fun ProfileScreen(viewModel: AuthViewModel = hiltViewModel()) {
    val authStatus by viewModel.isAuthenticated.collectAsState()

    when (authStatus) {
        false -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LinearProgressIndicator()
            }
        }
        null -> {
            AuthEntryScreen()
        }
        true -> {
            UserProfileScreen(viewModel)
        }
    }
}

@Composable
fun UserProfileScreen(x0: AuthViewModel) {
    TODO("Not yet implemented")
}

@Composable
fun AuthEntryScreen(onLoginClick: () -> Unit = {}, onRegisterClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Вы не вошли в аккаунт")
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onLoginClick) {
            Text("Войти")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onRegisterClick) {
            Text("Регистрация")
        }
    }
}