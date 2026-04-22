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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import com.example.ais_sst_mobile.presentation.components.AppBackground
import com.example.ais_sst_mobile.presentation.components.CustomTextField
import com.example.ais_sst_mobile.presentation.components.CustomButton
import org.jetbrains.compose.resources.painterResource
import ais_sst_mobile.composeapp.generated.resources.Res
import ais_sst_mobile.composeapp.generated.resources.logo_auth
import androidx.compose.ui.text.style.TextAlign
import cafe.adriel.voyager.navigator.currentOrThrow

class LoginScreen : Screen {
    @Composable
    override fun Content() {
        val screenModel = getScreenModel<LoginScreenModel>()
        val state by screenModel.state.collectAsState()

        val showCaptchaDialog by screenModel.showCaptchaDialog.collectAsState()
        val currentCaptcha by screenModel.currentCaptcha.collectAsState()
        val captchaError by screenModel.captchaError.collectAsState()

        val focusManager = LocalFocusManager.current

        var login by rememberSaveable { mutableStateOf("") }
        var selectedDomain by rememberSaveable { mutableStateOf("@edu.fa.ru") }
        var isDomainMenuExpanded by remember { mutableStateOf(false) }

        var password by rememberSaveable { mutableStateOf("") }
        var isPasswordVisible by rememberSaveable { mutableStateOf(false) }

        val passwordRegex = remember { Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}\$") }

        val isLoginError = selectedDomain == "@edu.fa.ru" && login.isNotEmpty() && login.length != 6
        val isPasswordError = password.isNotEmpty() && !passwordRegex.matches(password)

        val navigator = cafe.adriel.voyager.navigator.LocalNavigator.currentOrThrow

        LaunchedEffect(state) {
            when (state) {
                is LoginScreenModel.State.Success -> {
                    val user = (state as LoginScreenModel.State.Success).user
                    println("АВТОРИЗАЦИЯ УСПЕШНА! Привет, ${user.name} ${user.surname}. Токен: ${user.token}")
                }
                else -> {}
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
                    .imePadding()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

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
                    value = login,
                    onValueChange = { newValue ->
                        if (selectedDomain == "@edu.fa.ru") {
                            if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
                                login = newValue
                                screenModel.resetState()
                            }
                        } else {
                            login = newValue
                            screenModel.resetState()
                        }
                    },
                    placeholder = if (selectedDomain == "@edu.fa.ru") "Номер студбилета" else "Логин",
                    isError = isLoginError,
                    errorMessage = if (isLoginError) "Студбилет должен состоять из 6 цифр" else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    trailingIcon = {
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { isDomainMenuExpanded = true }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedDomain,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Icon(
                                    imageVector = if (isDomainMenuExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Выбрать домен",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(start = 2.dp).size(20.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = isDomainMenuExpanded,
                                onDismissRequest = { isDomainMenuExpanded = false },
                                containerColor = MaterialTheme.colorScheme.background
                            ) {
                                listOf("@edu.fa.ru", "@fa.ru").forEach { domain ->
                                    DropdownMenuItem(
                                        text = { Text(domain, style = MaterialTheme.typography.labelMedium) },
                                        onClick = {
                                            selectedDomain = domain
                                            isDomainMenuExpanded = false
                                            screenModel.resetState()

                                            if (domain == "@edu.fa.ru" && !login.all { it.isDigit() }) {
                                                login = login.filter { it.isDigit() }.take(6)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                CustomTextField(
                    modifier = contentModifier,
                    value = password,
                    onValueChange = {
                        password = it
                        screenModel.resetState()
                    },
                    placeholder = "Пароль",
                    isError = isPasswordError,
                    errorMessage = if (isPasswordError) "От 8 символов: A-Z, a-z, цифры, спецсимволы" else null,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            screenModel.login(login, selectedDomain, password)
                        }
                    ),
                    trailingIcon = {
                        val image = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
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
                    isLoading = state is LoginScreenModel.State.Loading,
                    onClick = {
                        focusManager.clearFocus()
                        screenModel.login(login, selectedDomain, password)
                    },
                    modifier = contentModifier
                )

                if (state is LoginScreenModel.State.Error) {
                    Text(
                        text = (state as LoginScreenModel.State.Error).message,
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
                        modifier = Modifier.clickable { navigator.push(RegisterScreen()) }
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
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

    Dialog(
        onDismissRequest = {  },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
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