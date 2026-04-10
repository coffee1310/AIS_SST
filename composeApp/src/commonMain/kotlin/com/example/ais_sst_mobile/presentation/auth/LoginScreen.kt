package com.example.ais_sst_mobile.presentation.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import com.example.ais_sst_mobile.presentation.components.AppBackground
import com.example.ais_sst_mobile.presentation.components.CustomTextField
import org.jetbrains.compose.resources.painterResource
import ais_sst_mobile.composeapp.generated.resources.Res
import ais_sst_mobile.composeapp.generated.resources.logo_auth
import cafe.adriel.voyager.navigator.currentOrThrow

class LoginScreen : Screen {
    @Composable
    override fun Content() {
        val screenModel = getScreenModel<LoginScreenModel>()
        val state by screenModel.state.collectAsState()
        val focusManager = LocalFocusManager.current

        var login by rememberSaveable { mutableStateOf("") }
        var password by rememberSaveable { mutableStateOf("") }
        var isPasswordVisible by rememberSaveable { mutableStateOf(false) }

        val passwordRegex = remember { Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])[a-zA-Z0-9]{8,}\$") }

        val isLoginError = login.isNotEmpty() && login.length != 6

        val isPasswordError = password.isNotEmpty() && !passwordRegex.matches(password)

        val navigator = cafe.adriel.voyager.navigator.LocalNavigator.currentOrThrow

        LaunchedEffect(state) {
            when (state) {
                is LoginScreenModel.State.Success -> {
                    // TODO: Здесь будет переход на главный экран приложения!
                    // navigator.replaceAll(MainScreen())

                    val user = (state as LoginScreenModel.State.Success).user
                    println("АВТОРИЗАЦИЯ УСПЕШНА! Привет, ${user.name} ${user.surname}. Токен: ${user.token}")
                }
                else -> {}
            }
        }

        AppBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(80.dp))

                Image(
                    painter = painterResource(Res.drawable.logo_auth),
                    contentDescription = "Логотип",
                    modifier = Modifier.height(110.dp)
                )

                Spacer(modifier = Modifier.height(100.dp))

                CustomTextField(
                    value = login,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() }) {
                            login = newValue
                            screenModel.resetState()
                        }
                    },
                    placeholder = "Номер студбилета",
                    isError = isLoginError,
                    errorMessage = if (isLoginError) "Студбилет должен состоять из 6 цифр" else null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    suffix = {
                        Text("@edu.fa.ru", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                CustomTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        screenModel.resetState()
                    },
                    placeholder = "Пароль",
                    isError = isPasswordError,
                    errorMessage = if (isPasswordError) "От 8 символов: A-Z, a-z и цифры" else null,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            screenModel.login(login, password)
                        }
                    ),
                    trailingIcon = {
                        val image = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(imageVector = image, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                )

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    Text(
                        text = "Забыли пароль?",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier
                            .padding(top = 12.dp, start = 15.dp)
                            .clickable { /* TODO: Страница восстановления */ }
                    )
                }

                Spacer(modifier = Modifier.height(90.dp))

                Button(
                    onClick = { screenModel.login(login, password) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(0.3.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    if (state is LoginScreenModel.State.Loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = "Войти",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                if (state is LoginScreenModel.State.Error) {
                    Text(
                        text = (state as LoginScreenModel.State.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Нет аккаунта? ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = "Зарегистрироваться",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.clickable { /* TODO: На регистрацию */ }
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}