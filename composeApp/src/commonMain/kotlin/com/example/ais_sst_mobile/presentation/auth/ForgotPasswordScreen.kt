package com.example.ais_sst_mobile.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ais_sst_mobile.navigation.ForgotPasswordComponent
import com.example.ais_sst_mobile.presentation.components.AppBackground
import com.example.ais_sst_mobile.presentation.components.CustomBackButton
import com.example.ais_sst_mobile.presentation.components.CustomButton
import com.example.ais_sst_mobile.presentation.components.CustomTextField
import com.example.ais_sst_mobile.presentation.components.clearFocusOnTap
import org.koin.compose.getKoin

@Composable
fun ForgotPasswordScreen(component: ForgotPasswordComponent) {
    val koin = getKoin()
    val screenModel = remember { koin.get<ForgotPasswordScreenModel>() }
    val state by screenModel.uiState.collectAsState()

    val focusManager = LocalFocusManager.current

    var isNewPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var expandedDomain by remember { mutableStateOf(false) }

    // === ПРАВИЛЬНЫЙ REGEX (как в регистрации) ===
    val passwordRegex = remember {
        Regex("""^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?])[a-zA-Z0-9!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?]{8,}$""")
    }

    val isPasswordError = state.newPassword.isNotEmpty() && !passwordRegex.matches(state.newPassword)
    val isConfirmPasswordError = state.confirmPassword.isNotEmpty() && state.newPassword != state.confirmPassword

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clearFocusOnTap(focusManager)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
        ) {
            // ==================== КНОПКА НАЗАД (ВВЕРХУ СЛЕВА) ====================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 8.dp)
            ) {
                CustomBackButton(onClick = { component.onGoBack() })
            }

            // ==================== ОСНОВНОЙ КОНТЕНТ (ПО ЦЕНТРУ) ====================
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "Восстановление пароля",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (state.step == 1)
                            "Введите корпоративную почту"
                        else
                            "Введите код из письма и новый пароль",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    // ==================== ШАГ 1 ====================
                    if (state.step == 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CustomTextField(
                                modifier = Modifier.weight(1f),
                                value = state.email,
                                onValueChange = { newValue ->
                                    if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
                                        screenModel.updateEmail(newValue)
                                    }
                                },
                                placeholder = "Номер студбилета",
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Выбор домена
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { expandedDomain = true }
                                        .padding(horizontal = 12.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(state.domain, color = MaterialTheme.colorScheme.onSurface)
                                    Icon(
                                        imageVector = if (expandedDomain) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                DropdownMenu(
                                    expanded = expandedDomain,
                                    onDismissRequest = { expandedDomain = false },
                                    containerColor = MaterialTheme.colorScheme.background
                                ) {
                                    listOf("@edu.fa.ru", "@fa.ru").forEach { domain ->
                                        DropdownMenuItem(
                                            text = { Text(domain) },
                                            onClick = {
                                                screenModel.updateDomain(domain)
                                                expandedDomain = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        CustomButton(
                            text = "Отправить код",
                            isLoading = state.isLoading,
                            onClick = {
                                focusManager.clearFocus()
                                screenModel.requestCode()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // ==================== ШАГ 2 ====================
                    if (state.step == 2) {
                        CustomTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.code,
                            onValueChange = { screenModel.updateCode(it) },
                            placeholder = "Код из письма",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        CustomTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.newPassword,
                            onValueChange = { screenModel.updateNewPassword(it) },
                            placeholder = "Новый пароль",
                            isError = isPasswordError,
                            errorMessage = if (isPasswordError) "Минимум 8 символов: A-Z, a-z, цифры и спецсимвол" else null,
                            visualTransformation = if (isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                val icon = if (isNewPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                IconButton(onClick = { isNewPasswordVisible = !isNewPasswordVisible }) {
                                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        CustomTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.confirmPassword,
                            onValueChange = { screenModel.updateConfirmPassword(it) },
                            placeholder = "Повторите пароль",
                            isError = isConfirmPasswordError,
                            errorMessage = if (isConfirmPasswordError) "Пароли не совпадают" else null,
                            visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                val icon = if (isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        CustomButton(
                            text = "Сбросить пароль",
                            isLoading = state.isLoading,
                            onClick = {
                                focusManager.clearFocus()
                                screenModel.resetPassword()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    state.error?.let { error ->
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // ==================== ОВЕРЛЕЙ УСПЕХА ====================
        if (state.isSuccess) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(enabled = false) { },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Пароль успешно изменён!",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Теперь вы можете войти с новым паролем",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    CustomButton(
                        text = "Вернуться ко входу",
                        onClick = { component.onPasswordResetSuccess() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}