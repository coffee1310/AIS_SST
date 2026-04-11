package com.example.ais_sst_mobile.presentation.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.ais_sst_mobile.presentation.components.*
import com.example.ais_sst_mobile.presentation.components.utils.DateTransformation
import com.example.ais_sst_mobile.presentation.components.utils.PhoneTransformation
import com.example.ais_sst_mobile.presentation.components.utils.PrefixTransformation
import com.preat.peekaboo.image.picker.SelectionMode
import com.preat.peekaboo.image.picker.rememberImagePickerLauncher
import com.preat.peekaboo.image.picker.toImageBitmap
import kotlinx.datetime.toLocalDateTime

class RegisterScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val focusManager = LocalFocusManager.current

        var surname by rememberSaveable { mutableStateOf("") }
        var name by rememberSaveable { mutableStateOf("") }
        var patronymic by rememberSaveable { mutableStateOf("") }
        var birthDate by rememberSaveable { mutableStateOf("") }
        var socialStatus by rememberSaveable { mutableStateOf("") }
        var gender by rememberSaveable { mutableStateOf("") }
        var course by rememberSaveable { mutableStateOf("") }
        var specialty by rememberSaveable { mutableStateOf("") }
        var groupNum by rememberSaveable { mutableStateOf("") }
        var corpEmail by rememberSaveable { mutableStateOf("") }
        var corpDomain by rememberSaveable { mutableStateOf("@edu.fa.ru") }
        var addEmail by rememberSaveable { mutableStateOf("") }
        var phone by rememberSaveable { mutableStateOf("") }
        var vkLink by rememberSaveable { mutableStateOf("") }
        var password by rememberSaveable { mutableStateOf("") }
        var confirmPassword by rememberSaveable { mutableStateOf("") }

        var isPassVisible by rememberSaveable { mutableStateOf(false) }
        var isConfirmPassVisible by rememberSaveable { mutableStateOf(false) }
        var isAgreedPD by rememberSaveable { mutableStateOf(false) }
        val isAgreedNewsletter = true
        var isPhoneFocused by remember { mutableStateOf(false) }
        var isVkFocused by remember { mutableStateOf(false) }
        var showDatePicker by remember { mutableStateOf(false) }
        val datePickerState = rememberDatePickerState()
        var expandedCorpDomain by remember { mutableStateOf(false) }
        var expandedStatus by remember { mutableStateOf(false) }
        var expandedGender by remember { mutableStateOf(false) }
        var expandedSpecialty by remember { mutableStateOf(false) }
        var generalError by remember { mutableStateOf<String?>(null) }

        val scope = rememberCoroutineScope()
        var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
        val singleImagePicker = rememberImagePickerLauncher(
            selectionMode = SelectionMode.Single,
            scope = scope,
            onResult = { byteArrays -> selectedImageBytes = byteArrays.firstOrNull() }
        )

        val nameRegex = remember { Regex("^[А-ЯЁ][а-яё]*$") }
        val emailRegex = remember { Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z]+$") }
        val vkRegex = remember { Regex("^[a-zA-Z0-9_.-]+$") }
        val passwordRegex = remember { Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])[a-zA-Z0-9]{8,}$") }

        val isSurnameError = surname.isNotEmpty() && !nameRegex.matches(surname.trim())
        val isNameError = name.isNotEmpty() && !nameRegex.matches(name.trim())
        val isPatronymicError = patronymic.isNotEmpty() && !nameRegex.matches(patronymic.trim())

        val isBirthDateError = remember(birthDate) {
            if (birthDate.isEmpty()) false
            else if (birthDate.length != 8) true
            else {
                try {
                    val d = birthDate.substring(0, 2).toInt()
                    val m = birthDate.substring(2, 4).toInt()
                    val y = birthDate.substring(4, 8).toInt()
                    val inputDate = kotlinx.datetime.LocalDate(year = y, monthNumber = m, dayOfMonth = d)

                    val currentMillis = kotlin.time.Clock.System.now().toEpochMilliseconds()
                    val todayInstant = kotlinx.datetime.Instant.fromEpochMilliseconds(currentMillis)
                    val today = todayInstant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date

                    inputDate > today || y < 1900
                } catch (e: Exception) { true }
            }
        }

        val isCourseError = course.isNotEmpty() && !course.all { it.isDigit() }
        val isGroupNumError = groupNum.isNotEmpty() && !groupNum.all { it.isDigit() }
        val isCorpEmailError = corpEmail.isNotEmpty() && (corpEmail.length != 6 || !corpEmail.all { it.isDigit() })
        val isAddEmailError = addEmail.isNotEmpty() && !emailRegex.matches(addEmail.trim())
        val isPhoneError = phone.isNotEmpty() && phone.length != 10
        val isVkLinkError = vkLink.isNotEmpty() && !vkRegex.matches(vkLink.trim())
        val isPasswordError = password.isNotEmpty() && !passwordRegex.matches(password)
        val isConfirmPasswordError = confirmPassword.isNotEmpty() && password != confirmPassword

        val isRegisterEnabled = isAgreedPD && isAgreedNewsletter

        AppBackground {
            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            showDatePicker = false
                            datePickerState.selectedDateMillis?.let { millis ->
                                val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(millis)
                                val localDate = instant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                                val day = localDate.dayOfMonth.toString().padStart(2, '0')
                                val month = localDate.monthNumber.toString().padStart(2, '0')
                                val year = localDate.year.toString()
                                birthDate = "$day$month$year"
                            }
                        }) { Text("ОК", color = MaterialTheme.colorScheme.secondary) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Отмена", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(60.dp))

                CustomTextField(
                    value = surname,
                    onValueChange = { surname = it; generalError = null },
                    placeholder = "* Фамилия",
                    isError = isSurnameError,
                    errorMessage = if (isSurnameError) "Только кириллица, с заглавной буквы" else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
                Spacer(modifier = Modifier.height(16.dp))

                CustomTextField(
                    value = name,
                    onValueChange = { name = it; generalError = null },
                    placeholder = "* Имя",
                    isError = isNameError,
                    errorMessage = if (isNameError) "Только кириллица, с заглавной буквы" else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
                Spacer(modifier = Modifier.height(16.dp))

                CustomTextField(
                    value = patronymic,
                    onValueChange = { patronymic = it; generalError = null },
                    placeholder = "* Отчество",
                    isError = isPatronymicError,
                    errorMessage = if (isPatronymicError) "Только кириллица, с заглавной буквы" else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
                Spacer(modifier = Modifier.height(16.dp))

                CustomTextField(
                    value = birthDate,
                    onValueChange = {
                        if (it.length <= 8 && it.all { char -> char.isDigit() }) {
                            birthDate = it
                            generalError = null
                        }
                    },
                    placeholder = "* Дата рождения",
                    isError = isBirthDateError,
                    errorMessage = if (isBirthDateError) "Некорректная дата (ДД.ММ.ГГГГ)" else null,
                    visualTransformation = DateTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.clearFocus() }),
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, "Выбрать дату", tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))

                val statuses = listOf("Студент", "Сирота", "Инвалид", "Многодетный", "Иное")
                ExposedDropdownMenuBox(expanded = expandedStatus, onExpandedChange = { expandedStatus = !expandedStatus }) {
                    CustomTextField(
                        value = socialStatus, onValueChange = {}, readOnly = true, placeholder = "* Социальный статус",
                        trailingIcon = {
                            val icon = if (expandedStatus) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
                            Icon(icon, null, tint = MaterialTheme.colorScheme.secondary)
                        },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expandedStatus, onDismissRequest = { expandedStatus = false }) {
                        statuses.forEach { selection ->
                            DropdownMenuItem(
                                text = { Text(selection, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface) },
                                onClick = { socialStatus = selection; expandedStatus = false; generalError = null }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                val genders = listOf("Мужской", "Женский")
                ExposedDropdownMenuBox(expanded = expandedGender, onExpandedChange = { expandedGender = !expandedGender }) {
                    CustomTextField(
                        value = gender, onValueChange = {}, readOnly = true, placeholder = "* Пол",
                        trailingIcon = {
                            val icon = if (expandedGender) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
                            Icon(icon, null, tint = MaterialTheme.colorScheme.secondary)
                        },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expandedGender, onDismissRequest = { expandedGender = false }) {
                        genders.forEach { selection ->
                            DropdownMenuItem(
                                text = { Text(selection, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface) },
                                onClick = { gender = selection; expandedGender = false; generalError = null }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                CustomTextField(
                    value = course,
                    onValueChange = {
                        if (it.length <= 1 && it.all { c -> c in '1'..'4' }) { course = it; generalError = null }
                    },
                    placeholder = "* Номер курса",
                    isError = isCourseError,
                    errorMessage = if (isCourseError) "Только цифры от 1 до 4" else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.clearFocus() })
                )
                Spacer(modifier = Modifier.height(16.dp))

                val specialties = listOf("ПИ", "ИБ", "ИБАС", "ПМИ")
                ExposedDropdownMenuBox(expanded = expandedSpecialty, onExpandedChange = { expandedSpecialty = !expandedSpecialty }) {
                    CustomTextField(
                        value = specialty, onValueChange = {}, readOnly = true, placeholder = "* Специальность",
                        trailingIcon = {
                            val icon = if (expandedSpecialty) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
                            Icon(icon, null, tint = MaterialTheme.colorScheme.secondary)
                        },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expandedSpecialty, onDismissRequest = { expandedSpecialty = false }) {
                        specialties.forEach { selection ->
                            DropdownMenuItem(
                                text = { Text(selection, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface) },
                                onClick = { specialty = selection; expandedSpecialty = false; generalError = null }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                CustomTextField(
                    value = groupNum,
                    onValueChange = {
                        if (it.length <= 6 && it.all { c -> c.isDigit() }) { groupNum = it; generalError = null }
                    },
                    placeholder = "* Номер группы",
                    isError = isGroupNumError,
                    errorMessage = if (isGroupNumError) "Только цифры" else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
                Spacer(modifier = Modifier.height(16.dp))

                CustomTextField(
                    value = corpEmail,
                    onValueChange = {
                        if (it.length <= 6 && it.all { c -> c.isDigit() }) { corpEmail = it; generalError = null }
                    },
                    placeholder = "* Корпоративная почта",
                    isError = isCorpEmailError,
                    errorMessage = if (isCorpEmailError) "Только 6 цифр, выбор домена справа" else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    suffix = {
                        Box {
                            Row(
                                modifier = Modifier.clickable { expandedCorpDomain = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(corpDomain, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyLarge)
                                Icon(
                                    if (expandedCorpDomain) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    "Выбрать домен", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(start = 2.dp).size(20.dp)
                                )
                            }
                            DropdownMenu(expanded = expandedCorpDomain, onDismissRequest = { expandedCorpDomain = false }, containerColor = MaterialTheme.colorScheme.background) {
                                listOf("@edu.fa.ru", "@fa.ru").forEach { domain ->
                                    DropdownMenuItem(
                                        text = { Text(domain, style = MaterialTheme.typography.labelMedium) },
                                        onClick = { corpDomain = domain; expandedCorpDomain = false; generalError = null }
                                    )
                                }
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))

                CustomTextField(
                    value = addEmail,
                    onValueChange = { addEmail = it; generalError = null },
                    placeholder = "  Дополнительная почта",
                    isError = isAddEmailError,
                    errorMessage = if (isAddEmailError) "Некорректный формат почты" else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
                Spacer(modifier = Modifier.height(16.dp))

                CustomTextField(
                    value = phone,
                    onValueChange = {
                        if (it.length <= 10 && it.all { c -> c.isDigit() }) { phone = it; generalError = null }
                    },
                    placeholder = "* Номер телефона",
                    isError = isPhoneError,
                    errorMessage = if (isPhoneError) "Введите 10 цифр (без +7)" else null,
                    visualTransformation = PhoneTransformation(isPhoneFocused),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier.onFocusChanged { isPhoneFocused = it.isFocused }
                )
                Spacer(modifier = Modifier.height(16.dp))

                CustomTextField(
                    value = vkLink,
                    onValueChange = { vkLink = it; generalError = null },
                    placeholder = "* Ссылка на ВКонтакте",
                    isError = isVkLinkError,
                    errorMessage = if (isVkLinkError) "Только латиница и цифры, без пробелов и @" else null,
                    visualTransformation = PrefixTransformation("https://vk.com/", isVkFocused),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier.onFocusChanged { isVkFocused = it.isFocused }
                )
                Spacer(modifier = Modifier.height(16.dp))

                CustomTextField(
                    value = password,
                    onValueChange = { password = it; generalError = null },
                    placeholder = "* Пароль",
                    isError = isPasswordError,
                    errorMessage = if (isPasswordError) "От 8 символов: A-Z, a-z и цифры" else null,
                    visualTransformation = if (isPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    trailingIcon = {
                        val icon = if (isPassVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { isPassVisible = !isPassVisible }) {
                            Icon(icon, null, tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))

                CustomTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; generalError = null },
                    placeholder = "* Повторите пароль",
                    isError = isConfirmPasswordError,
                    errorMessage = if (isConfirmPasswordError) "Пароли не совпадают" else null,
                    visualTransformation = if (isConfirmPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    trailingIcon = {
                        val icon = if (isConfirmPassVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { isConfirmPassVisible = !isConfirmPassVisible }) {
                            Icon(icon, null, tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.fillMaxWidth().clickable { singleImagePicker.launch() }) {
                    CustomTextField(
                        value = if (selectedImageBytes != null) "Фотография загружена" else "",
                        onValueChange = {}, readOnly = true, placeholder = "* Официальная фотография",
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            val icon = if (selectedImageBytes != null) Icons.Default.CheckCircle else Icons.Default.AddAPhoto
                            Icon(icon, "Загрузить фото", tint = MaterialTheme.colorScheme.secondary)
                        }
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { singleImagePicker.launch() })
                }

                selectedImageBytes?.let { bytes ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Image(
                        bitmap = bytes.toImageBitmap(),
                        contentDescription = "Превью профиля",
                        modifier = Modifier.size(100.dp).clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.secondary, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Text(
                    text = "Загрузка своей фотографии является обязательным условием! Просьба загружать официальное фото в анфас, где видно ваше лицо",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Thin),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                )

                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { isAgreedPD = !isAgreedPD }
                    ) {
                        Checkbox(
                            checked = isAgreedPD, onCheckedChange = { isAgreedPD = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
                        )
                        Text(
                            text = "Я даю согласие на обработку данных",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Light),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(
                            checked = isAgreedNewsletter, onCheckedChange = { },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.secondary,
                                disabledCheckedColor = MaterialTheme.colorScheme.secondary
                            )
                        )
                        Text(
                            text = "Я даю согласие на получение рассылки",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Light),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(25.dp))

                Button(
                    onClick = {
                        val hasEmptyFields = surname.isBlank() || name.isBlank() || birthDate.length != 8 ||
                                socialStatus.isBlank() || gender.isBlank() || course.isBlank() || specialty.isBlank() ||
                                groupNum.isBlank() || corpEmail.isBlank() || phone.length != 10 ||
                                vkLink.isBlank() || password.isBlank() || confirmPassword.isBlank() || selectedImageBytes == null

                        val hasValidationErrors = isSurnameError || isNameError || isPatronymicError || isBirthDateError ||
                                isCourseError || isGroupNumError || isCorpEmailError || isAddEmailError ||
                                isPhoneError || isVkLinkError || isPasswordError || isConfirmPasswordError

                        if (hasEmptyFields || hasValidationErrors) {
                            generalError = "Корректно заполните все обязательные поля и загрузите фото"
                        } else {
                            generalError = null
                            // TODO: Отправка на сервер
                        }
                    },
                    enabled = isRegisterEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    ),
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(0.3.dp, if (isRegisterEnabled) MaterialTheme.colorScheme.outline else Color.Transparent),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Зарегистрироваться", style = MaterialTheme.typography.titleLarge, color = if (isRegisterEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f))
                }

                generalError?.let { msg ->
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp).fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Уже есть аккаунт? ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = "Войти",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.clickable { navigator.pop() }
                    )
                }

                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}