package com.example.livegeoguessr.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.livegeoguessr.R
import com.example.livegeoguessr.ui.testing.TestTags

@Composable
fun AuthScreen(
    onSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(
                if (uiState.isLoginMode) {
                    TestTags.LOGIN_SCREEN
                } else {
                    TestTags.REGISTER_SCREEN
                }
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (uiState.isLoginMode) {
                stringResource(R.string.login)
            } else {
                stringResource(R.string.register)
            },
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = {
                Text(stringResource(R.string.email))
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(
                    if (uiState.isLoginMode) {
                        TestTags.LOGIN_EMAIL
                    } else {
                        TestTags.REGISTER_EMAIL
                    }
                ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = {
                Text(stringResource(R.string.password))
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(
                    if (uiState.isLoginMode) {
                        TestTags.LOGIN_PASSWORD
                    } else {
                        TestTags.REGISTER_PASSWORD
                    }
                ),
            singleLine = true,
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            trailingIcon = {
                val image = if (passwordVisible) {
                    Icons.Filled.Visibility
                } else {
                    Icons.Filled.VisibilityOff
                }

                IconButton(
                    onClick = {
                        passwordVisible = !passwordVisible
                    }
                ) {
                    Icon(
                        imageVector = image,
                        contentDescription = null
                    )
                }
            }
        )

        if (uiState.errorResId != null) {
            Text(
                text = stringResource(uiState.errorResId!!),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .testTag(TestTags.AUTH_ERROR)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.authenticate(email, password)
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(
                    if (uiState.isLoginMode) {
                        TestTags.LOGIN_BUTTON
                    } else {
                        TestTags.REGISTER_BUTTON
                    }
                ),
            enabled = email.isNotEmpty() &&
                    password.length >= 6 &&
                    !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    if (uiState.isLoginMode) {
                        stringResource(R.string.login)
                    } else {
                        stringResource(R.string.register)
                    }
                )
            }
        }

        TextButton(
            onClick = {
                viewModel.toggleMode()
            },
            modifier = Modifier
                .padding(top = 8.dp)
                .testTag(
                    if (uiState.isLoginMode) {
                        TestTags.REGISTER_LINK
                    } else {
                        TestTags.LOGIN_LINK
                    }
                )
        ) {
            Text(
                if (uiState.isLoginMode) {
                    stringResource(R.string.dont_have_account)
                } else {
                    stringResource(R.string.already_have_account)
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp)
        )

        OutlinedButton(
            onClick = {
                viewModel.loginWithGoogle(context)
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.GOOGLE_LOGIN_BUTTON),
            enabled = !uiState.isLoading
        ) {
            Text(stringResource(R.string.login_with_google))
        }
    }
}