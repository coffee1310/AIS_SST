package com.example.ais_sst_mobile.presentation.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ais_sst_mobile.presentation.components.AppBackground
import com.example.ais_sst_mobile.presentation.components.CustomTextField
import com.example.ais_sst_mobile.presentation.components.CustomButton
import org.jetbrains.compose.resources.painterResource
import ais_sst_mobile.composeapp.generated.resources.Res
import ais_sst_mobile.composeapp.generated.resources.logo_auth
import androidx.compose.ui.text.style.TextAlign
import com.example.ais_sst_mobile.navigation.LoginComponent
import com.example.ais_sst_mobile.presentation.components.clearFocusOnScroll
import com.example.ais_sst_mobile.presentation.components.clearFocusOnTap
import org.koin.compose.getKoin

@Composable
fun LoginScreen(component: LoginComponent) {
    val koin = getKoin()
    val screenModel = remember { koin.get<LoginScreenModel>() }

    val uiState by screenModel.uiState.collectAsState()
    val screenState by screenModel.screenState.collectAsState()

    val showCaptchaDialog by screenModel.showCaptchaDialog.collectAsState()
    val currentCaptcha by screenModel.currentCaptcha.collectAsState()
    val captchaError by screenModel.captchaError.collectAsState()

    val focusManager = LocalFocusManager.current

    //val passwordRegex = remember { Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#\\\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}\$") }
    val passwordRegex = remember { Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$") }

    val isLoginError = uiState.domain == "@edu.fa.ru" && uiState.login.isNotEmpty() && uiState.login.length != 6
    val isPasswordError = uiState.password.isNotEmpty() && !passwordRegex.matches(uiState.password)

    LaunchedEffect(Unit) {
        screenModel.effect.collect { effect ->
            if (effect == "SUCCESS") {
                component.onLoginSuccess()
            }
        }
    }

    if (showCaptchaDialog && currentCaptcha != null) {
        CaptchaDialog(
            currentCaptcha = currentCaptcha!!,
            captchaError = captchaError,
            onRefresh = { screenModel.refreshCaptcha() },
            onVerify = { input -> screenModel.verifyCaptcha(input) },
            onClearError = { screenModel.clearCaptchaError() }
        )
    }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clearFocusOnTap(focusManager)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.safeDrawing))

            val contentModifier = Modifier.fillMaxWidth(0.9f)

            Spacer(modifier = Modifier.height(80.dp))

            Image(
                painter = painterResource(Res.drawable.logo_auth),
                contentDescription = "Логотип",
                modifier = Modifier.height(110.dp)
            )

            Spacer(modifier = Modifier.height(100.dp))

            CustomTextField(
                modifier = contentModifier,
                value = uiState.login,
                onValueChange = { screenModel.updateLogin(it) },
                placeholder = if (uiState.domain == "@edu.fa.ru") "Номер студбилета" else "Логин",
                isError = isLoginError,
                errorMessage = if (isLoginError) "Студбилет должен состоять из 6 цифр" else null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (uiState.domain == "@edu.fa.ru") KeyboardType.Number else KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                trailingIcon = {
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { screenModel.toggleDomainMenu(true) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.domain,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Icon(
                                imageVector = if (uiState.isDomainMenuExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Выбрать домен",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(start = 2.dp).size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = uiState.isDomainMenuExpanded,
                            onDismissRequest = { screenModel.toggleDomainMenu(false) },
                            containerColor = MaterialTheme.colorScheme.background
                        ) {
                            listOf("@edu.fa.ru", "@fa.ru").forEach { domain ->
                                DropdownMenuItem(
                                    text = { Text(domain, style = MaterialTheme.typography.labelMedium) },
                                    onClick = { screenModel.selectDomain(domain) }
                                )
                            }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            CustomTextField(
                modifier = contentModifier,
                value = uiState.password,
                onValueChange = { screenModel.updatePassword(it) },
                placeholder = "Пароль",
                isError = isPasswordError,
                errorMessage = if (isPasswordError) "От 8 символов: A-Z, a-z, цифры, спецсимволы" else null,
                visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        screenModel.login()
                    }
                ),
                trailingIcon = {
                    val image = if (uiState.isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { screenModel.togglePasswordVisibility() }) {
                        Icon(imageVector = image, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    }
                }
            )

            Box(modifier = contentModifier, contentAlignment = Alignment.CenterStart) {
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

            CustomButton(
                text = "Войти",
                isLoading = screenState is LoginScreenModel.ScreenState.Loading,
                onClick = {
                    focusManager.clearFocus()
                    screenModel.login()
                },
                modifier = contentModifier
            )

            if (screenState is LoginScreenModel.ScreenState.Error) {
                Text(
                    text = (screenState as LoginScreenModel.ScreenState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = contentModifier.padding(top = 16.dp, bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = contentModifier
            ) {
                Text("Нет аккаунта? ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = "Зарегистрироваться",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.clickable {
                        component.onNavigateToRegister()
                    }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing))
        }
    }
}

@Composable
fun CaptchaDialog(
    currentCaptcha: String,
    captchaError: String?,
    onRefresh: () -> Unit,
    onVerify: (String) -> Unit,
    onClearError: () -> Unit
) {
    var captchaInputLocal by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(captchaError) {
        if (captchaError != null && captchaError != "Введите код") {
            captchaInputLocal = ""
        }
    }

    Dialog(
        onDismissRequest = {  },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(0.3.dp, MaterialTheme.colorScheme.outline),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Подтвердите, что вы не робот",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentCaptcha,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.LineThrough
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, "Обновить", tint = MaterialTheme.colorScheme.secondary)
                    }
                }

                Spacer(Modifier.height(16.dp))

                CustomTextField(
                    value = captchaInputLocal,
                    onValueChange = {
                        captchaInputLocal = it
                        onClearError()
                    },
                    placeholder = "Введите код",
                    isError = captchaError != null,
                    errorMessage = captchaError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onVerify(captchaInputLocal)
                        }
                    )
                )

                Spacer(Modifier.height(24.dp))

                CustomButton(
                    text = "Подтвердить",
                    onClick = {
                        focusManager.clearFocus()
                        onVerify(captchaInputLocal)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}