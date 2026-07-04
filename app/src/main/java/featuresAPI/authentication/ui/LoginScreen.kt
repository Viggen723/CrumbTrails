package com.example.routetracker.featuresAPI.auth.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import featuresAPI.authentication.data.Resource
import featuresAPI.authentication.viewModel.AuthenticationViewModel

@Composable
fun LoginScreen(
    viewModel: AuthenticationViewModel,
    onLoginSuccessRoute: () -> Unit
) {
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    var isSignUpMode by remember { mutableStateOf(false) }

    val authState by viewModel.loginState.collectAsState()

    LaunchedEffect(key1 = authState) {
        if (authState is Resource.Success) {
            onLoginSuccessRoute()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "CrumbTrails",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = if (isSignUpMode) "Create Account" else "Sign In",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = emailInput,
            onValueChange = { emailInput = it },
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = passwordInput,
            onValueChange = { passwordInput = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (authState is Resource.Loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (isSignUpMode) {
                        viewModel.performSignUp(emailInput, passwordInput)
                    } else {
                        viewModel.performLogin(emailInput, passwordInput)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(if (isSignUpMode) "Sign Up" else "Login")
            }
        }

        if (authState is Resource.Error) {
            val message = "Invalid login!"
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (isSignUpMode) "Already have an account? Log In" else "Don't have an account? Sign Up",
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable {
                isSignUpMode = !isSignUpMode
                viewModel.clearErrorState() // Remove any old errors when switching views
            }
        )
    }
}