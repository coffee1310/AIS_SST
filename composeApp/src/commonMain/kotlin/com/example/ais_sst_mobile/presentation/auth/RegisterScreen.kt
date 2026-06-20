package com.example.ais_sst_mobile.presentation.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.data.network.dto.RegisterRequest
import com.example.ais_sst_mobile.domain.model.Group
import com.example.ais_sst_mobile.domain.model.SocialStatus
import com.example.ais_sst_mobile.domain.model.Speciality
import com.example.ais_sst_mobile.domain.repository.AuthRepository
import com.example.ais_sst_mobile.domain.repository.DictionaryRepository
import com.example.ais_sst_mobile.navigation.RegisterComponent
import com.example.ais_sst_mobile.presentation.components.*
import com.example.ais_sst_mobile.presentation.components.utils.DateTransformation
import com.example.ais_sst_mobile.presentation.components.utils.PhoneTransformation
import com.example.ais_sst_mobile.presentation.components.utils.PrefixTransformation
import com.preat.peekaboo.image.picker.SelectionMode
import com.preat.peekaboo.image.picker.rememberImagePickerLauncher
import com.preat.peekaboo.image.picker.toImageBitmap
import com.preat.peekaboo.image.picker.ResizeOptions
import io.ktor.util.encodeBase64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.getKoin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(component: RegisterComponent) {
    val focusManager = LocalFocusManager.current
    val koin = getKoin()
    val screenModel = remember { koin.get<RegisterScreenModel>() }
    val state by screenModel.state.collectAsState()
    var surname by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var patronymic by rememberSaveable { mutableStateOf("") }
    var birthDate by rememberSaveable { mutableStateOf("") }
    var selectedSocialStatusIds by rememberSaveable { mutableStateOf(setOf<Int>()) }
    val selectedStatusesText = remember(selectedSocialStatusIds, state.socialStatuses) {
        if (selectedSocialStatusIds.isEmpty()) {
            ""
        } else {
            state.socialStatuses
                .filter { it.id in selectedSocialStatusIds }
                .joinToString(", ") { it.title }
        }
    }
    var gender by rememberSaveable { mutableStateOf("") }
    var course by rememberSaveable { mutableStateOf("") }
    var selectedGroupId by rememberSaveable { mutableStateOf<Int?>(null) }
    var groupTitle by rememberSaveable { mutableStateOf("") }
    var expandedGroup by remember { mutableStateOf(false) }
    var specialty by rememberSaveable { mutableStateOf("") }
    var selectedSpecialtyId by rememberSaveable { mutableStateOf<Int?>(null) }
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
    var expandedCourse by remember { mutableStateOf(false) }
    var generalError by remember { mutableStateOf<String?>(null) }
    var imageError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }

    // Новое состояние для предпросмотра картинки перед окончательным сохранением
    var pendingImageBytes by remember { mutableStateOf<ByteArray?>(null) }

    val maxImageSizeBytes = 2 * 1024 * 1024
    val uriHandler = LocalUriHandler.current
    val singleImagePicker = rememberImagePickerLauncher(
        selectionMode = SelectionMode.Single,
        scope = scope,
        resizeOptions = ResizeOptions(
            width = 800,
            height = 800,
            compressionQuality = 0.8
        ),
        onResult = { byteArrays ->
            val bytes = byteArrays.firstOrNull()
            if (bytes != null) {
                if (bytes.size > maxImageSizeBytes) {
                    imageError = "Файл слишком большой. Выберите фото размером до 2 МБ"
                    selectedImageBytes = null
                } else {
                    imageError = null
                    // Передаем в pending для предпросмотра
                    pendingImageBytes = bytes
                }
            }
        }
    )

    val nameRegex = remember { Regex("^[А-ЯЁ][а-яё]*$") }
    val emailRegex = remember { Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z]+$") }
    val vkRegex = remember { Regex("""^[a-zA-Z0-9_.\-/?=&!@#$%]+$""") }
    val passwordRegex = remember { Regex("""^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?])[a-zA-Z0-9!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?]{8,}$""") }
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

    val isCorpEmailError = corpDomain == "@edu.fa.ru" && corpEmail.isNotEmpty() && (corpEmail.length != 6 || !corpEmail.all { it.isDigit() })
    val isAddEmailError = addEmail.isNotEmpty() && !emailRegex.matches(addEmail.trim())
    val isPhoneError = phone.isNotEmpty() && phone.length != 10
    val isVkLinkError = vkLink.isNotEmpty() && !vkRegex.matches(vkLink.trim())
    val isPasswordError = password.isNotEmpty() && !passwordRegex.matches(password)
    val isConfirmPasswordError = confirmPassword.isNotEmpty() && password != confirmPassword

    val isRegisterEnabled = isAgreedPD && isAgreedNewsletter

    LaunchedEffect(state.registerError) {
        if (state.registerError != null) {
            generalError = state.registerError
            screenModel.clearError()
        }
    }

    // Оборачиваем ВСЁ в Box. Это позволит нам нарисовать предпросмотр поверх экрана без использования системного Dialog
    Box(modifier = Modifier.fillMaxSize()) {

        AppBackground {
            if (state.registerSuccess) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clearFocusOnTap(focusManager)
                        .clearFocusOnScroll(focusManager)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Успех",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Ваша заявка успешно отправлена на рассмотрение! Ответ придёт на корпоративную почту",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(40.dp))
                    CustomButton(
                        text = "Вернуться ко входу",
                        onClick = { component.onGoBack() },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    )
                }
            } else {
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
                        .clearFocusOnTap(focusManager)
                        .imePadding()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.windowInsetsTopHeight(WindowInsets.safeDrawing))

                    val contentModifier = Modifier.fillMaxWidth(0.9f)

                    Spacer(modifier = Modifier.height(40.dp))

                    Box(
                        modifier = contentModifier,
                        contentAlignment = Alignment.CenterStart
                    ) {
                        CustomBackButton(onClick = { component.onGoBack() })
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    CustomTextField(
                        modifier = contentModifier,
                        value = surname,
                        onValueChange = { if (it.length <= 64) { surname = it; generalError = null } },                    placeholder = "* Фамилия",
                        isError = isSurnameError,
                        errorMessage = if (isSurnameError) "Только кириллица, с заглавной буквы" else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    CustomTextField(
                        modifier = contentModifier,
                        value = name,
                        onValueChange = { if (it.length <= 64) { name = it; generalError = null } },                    placeholder = "* Имя",
                        isError = isNameError,
                        errorMessage = if (isNameError) "Только кириллица, с заглавной буквы" else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    CustomTextField(
                        modifier = contentModifier,
                        value = patronymic,
                        onValueChange = { if (it.length <= 64) { patronymic = it; generalError = null } },                    placeholder = "* Отчество",
                        isError = isPatronymicError,
                        errorMessage = if (isPatronymicError) "Только кириллица, с заглавной буквы" else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    CustomTextField(
                        modifier = contentModifier,
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

                    ExposedDropdownMenuBox(
                        expanded = expandedStatus,
                        onExpandedChange = { expandedStatus = !expandedStatus }
                    ) {
                        CustomTextField(
                            modifier = Modifier.menuAnchor().fillMaxWidth(0.9f),
                            value = selectedStatusesText,
                            onValueChange = {},
                            readOnly = true,
                            placeholder = "  Социальный статус",
                            trailingIcon = {
                                val icon = if (expandedStatus) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
                                Icon(icon, null, tint = MaterialTheme.colorScheme.secondary)
                            },
                        )

                        ExposedDropdownMenu(
                            expanded = expandedStatus,
                            onDismissRequest = { expandedStatus = false }
                        ) {
                            state.socialStatuses.forEach { item ->
                                val isSelected = selectedSocialStatusIds.contains(item.id)

                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = null,
                                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedSocialStatusIds = if (isSelected) {
                                            selectedSocialStatusIds - item.id
                                        } else {
                                            selectedSocialStatusIds + item.id
                                        }
                                        generalError = null
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    val genders = listOf("Мужчина", "Женщина")
                    ExposedDropdownMenuBox(expanded = expandedGender, onExpandedChange = { expandedGender = !expandedGender }) {
                        CustomTextField(
                            modifier = Modifier.menuAnchor().fillMaxWidth(0.9f),
                            value = gender, onValueChange = {}, readOnly = true, placeholder = "* Пол",
                            trailingIcon = {
                                val icon = if (expandedGender) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
                                Icon(icon, null, tint = MaterialTheme.colorScheme.secondary)
                            }
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

                    val courses = listOf("1", "2", "3", "4")
                    ExposedDropdownMenuBox(expanded = expandedCourse, onExpandedChange = { expandedCourse = !expandedCourse }) {
                        CustomTextField(
                            modifier = Modifier.menuAnchor().fillMaxWidth(0.9f),
                            value = course,
                            onValueChange = {},
                            readOnly = true,
                            placeholder = "* Номер курса",
                            trailingIcon = {
                                val icon = if (expandedCourse) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
                                Icon(icon, null, tint = MaterialTheme.colorScheme.secondary)
                            }
                        )
                        ExposedDropdownMenu(expanded = expandedCourse, onDismissRequest = { expandedCourse = false }) {
                            courses.forEach { selection ->
                                DropdownMenuItem(
                                    text = { Text(selection, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        course = selection
                                        expandedCourse = false
                                        generalError = null
                                        selectedGroupId = null
                                        groupTitle = ""
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    ExposedDropdownMenuBox(expanded = expandedSpecialty, onExpandedChange = { expandedSpecialty = !expandedSpecialty }) {
                        CustomTextField(
                            modifier = Modifier.menuAnchor().fillMaxWidth(0.9f),
                            value = specialty,
                            onValueChange = {},
                            readOnly = true,
                            placeholder = "* Специальность",
                            trailingIcon = {
                                val icon = if (expandedSpecialty) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
                                Icon(icon, null, tint = MaterialTheme.colorScheme.secondary)
                            }
                        )
                        ExposedDropdownMenu(expanded = expandedSpecialty, onDismissRequest = { expandedSpecialty = false }) {
                            state.specialities.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        specialty = item.title
                                        selectedSpecialtyId = item.id
                                        expandedSpecialty = false
                                        generalError = null
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    val availableGroups = remember(course, state.groups) {
                        val courseInt = course.toIntOrNull()
                        if (courseInt != null) {
                            state.groups.filter { it.course == courseInt }
                        } else {
                            emptyList()
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = expandedGroup,
                        onExpandedChange = { if (course.isNotEmpty()) expandedGroup = !expandedGroup }
                    ) {
                        CustomTextField(
                            modifier = Modifier.menuAnchor().fillMaxWidth(0.9f),
                            value = groupTitle,
                            onValueChange = {},
                            readOnly = true,
                            placeholder = if (course.isEmpty()) "Сначала выберите курс" else "* Номер группы",
                            trailingIcon = {
                                val icon = if (expandedGroup) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (course.isEmpty()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else MaterialTheme.colorScheme.secondary
                                )
                            }
                        )
                        ExposedDropdownMenu(expanded = expandedGroup, onDismissRequest = { expandedGroup = false }) {
                            availableGroups.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        groupTitle = item.title
                                        selectedGroupId = item.id
                                        expandedGroup = false
                                        generalError = null
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    CustomTextField(
                        modifier = contentModifier,
                        value = corpEmail,
                        onValueChange = { newValue ->
                            if (newValue.length + corpDomain.length <= 32) {
                                if (corpDomain == "@edu.fa.ru") {
                                    if (newValue.length <= 6 && newValue.all { c -> c.isDigit() }) {
                                        corpEmail = newValue
                                        generalError = null
                                    }
                                } else {
                                    if (!newValue.any { it in 'а'..'я' || it in 'А'..'Я' || it == 'ё' || it == 'Ё' }) {
                                        corpEmail = newValue
                                        generalError = null
                                    }
                                }
                            }
                        },
                        placeholder = "* Корпоративная почта",
                        isError = isCorpEmailError,
                        errorMessage = if (isCorpEmailError) "Студбилет должен состоять из 6 цифр" else null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (corpDomain == "@edu.fa.ru") KeyboardType.Number else KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
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
                                            onClick = {
                                                corpDomain = domain
                                                expandedCorpDomain = false
                                                generalError = null

                                                if (domain == "@edu.fa.ru" && !corpEmail.all { it.isDigit() }) {
                                                    corpEmail = corpEmail.filter { it.isDigit() }.take(6)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    CustomTextField(
                        modifier = contentModifier,
                        value = addEmail,
                        onValueChange = { if (it.length <= 256) { addEmail = it; generalError = null } },                    placeholder = "  Дополнительная почта",
                        isError = isAddEmailError,
                        errorMessage = if (isAddEmailError) "Некорректный формат почты" else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    CustomTextField(
                        modifier = contentModifier.onFocusChanged { isPhoneFocused = it.isFocused },
                        value = phone,
                        onValueChange = {
                            if (it.length <= 10 && it.all { c -> c.isDigit() }) { phone = it; generalError = null }
                        },
                        placeholder = "* Номер телефона",
                        isError = isPhoneError,
                        errorMessage = if (isPhoneError) "Введите 10 цифр (без +7)" else null,
                        visualTransformation = PhoneTransformation(isPhoneFocused),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    CustomTextField(
                        modifier = contentModifier.onFocusChanged { isVkFocused = it.isFocused },
                        value = vkLink,
                        onValueChange = { if (it.length <= 256) { vkLink = it; generalError = null } },                    placeholder = "* Ссылка на ВКонтакте",
                        isError = isVkLinkError,
                        errorMessage = if (isVkLinkError) "Ссылка не должна содержать пробелов и русских букв" else null,
                        visualTransformation = PrefixTransformation("https://vk.ru/", isVkFocused),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    CustomTextField(
                        modifier = contentModifier,
                        value = password,
                        onValueChange = { if (it.length <= 256) { password = it; generalError = null } },                    placeholder = "* Пароль",
                        isError = isPasswordError,
                        errorMessage = if (isPasswordError) "От 8 символов: A-Z, a-z, цифры и спецсимволы" else null,
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
                        modifier = contentModifier,
                        value = confirmPassword,
                        onValueChange = { if (it.length <= 256) { confirmPassword = it; generalError = null } },                    placeholder = "* Повторите пароль",
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

                    Box(modifier = contentModifier.clickable { singleImagePicker.launch() }) {
                        CustomTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = if (selectedImageBytes != null) "Фотография загружена" else "",
                            onValueChange = {}, readOnly = true, placeholder = "* Официальная фотография",
                            trailingIcon = {
                                val icon = if (selectedImageBytes != null) Icons.Default.CheckCircle else Icons.Default.AddAPhoto
                                Icon(icon, "Загрузить фото", tint = MaterialTheme.colorScheme.secondary)
                            }
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { singleImagePicker.launch() })
                    }

                    if (imageError != null) {
                        Text(
                            text = imageError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = contentModifier.padding(top = 16.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    // БЕЗОПАСНАЯ загрузка сохраненного фото
                    val selectedBytes = selectedImageBytes
                    if (selectedBytes != null) {
                        val profileBitmap = remember(selectedBytes) { selectedBytes.toImageBitmap() }
                        Spacer(modifier = Modifier.height(16.dp))
                        Image(
                            bitmap = profileBitmap,
                            contentDescription = "Превью профиля",
                            modifier = Modifier.size(100.dp).clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.secondary, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Text(
                        text = "Внимание: пожалуйста, отнеситесь к выбору снимка ответственно! Если фотография не будет соответствовать правилам ниже, ваша заявка на вступление может быть отклонена. Загрузите подходящее фото сразу, чтобы процесс регистрации прошел быстро и без лишних возвратов.\n"
                                + "\nТребования к снимку:" +
                                "\nФормат: цветная фотография 3х4 (без белого уголка).\nФон: строго белый и однотонный. В кадре не должно быть теней, полос, узоров или посторонних предметов." +
                                "\nПоза: строго анфас. Лицо открыто." +
                                "\nПропорции: лицо 70-80% площади всей фотографии." +
                                "\nОдежда: однотонная, чтобы не сливаться с фоном.",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Thin),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = contentModifier.padding(top = 8.dp, bottom = 24.dp)
                    )

                    val policyAnnotatedString = buildAnnotatedString {
                        append("Я даю согласие на обработку персональных данных в соответствии с ")

                        pushStringAnnotation(tag = "POLICY", annotation = "https://ais-sst.ru/privacy-policy.pdf")
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.secondary,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append("Политикой обработки персональных данных")
                        }
                        pop()
                    }

                    Column(modifier = contentModifier, horizontalAlignment = Alignment.Start) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = isAgreedPD, onCheckedChange = { isAgreedPD = it },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
                            )
                            ClickableText(
                                text = policyAnnotatedString,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Light,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                                ),
                                onClick = { offset ->
                                    policyAnnotatedString.getStringAnnotations(tag = "POLICY", start = offset, end = offset)
                                        .firstOrNull()?.let { annotation ->
                                            uriHandler.openUri(annotation.item)
                                        } ?: run {
                                        isAgreedPD = !isAgreedPD
                                    }
                                }
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { /* ... */ }) {
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

                    CustomButton(
                        modifier = contentModifier,
                        text = "Зарегистрироваться",
                        enabled = isRegisterEnabled,
                        isLoading = state.isLoading,
                        onClick = {
                            val hasEmptyFields = surname.isBlank() || name.isBlank() || birthDate.length != 8 ||
                                    gender.isBlank() || course.isBlank() || selectedSpecialtyId == null ||
                                    selectedGroupId == null || corpEmail.isBlank() || phone.length != 10 ||
                                    vkLink.isBlank() || password.isBlank() || confirmPassword.isBlank() || selectedImageBytes == null

                            val hasValidationErrors = isSurnameError || isNameError || isPatronymicError ||
                                    isBirthDateError || isCorpEmailError || isAddEmailError || isPhoneError ||
                                    isVkLinkError || isPasswordError || isConfirmPasswordError || imageError != null

                            if (hasEmptyFields || hasValidationErrors) {
                                generalError = "Корректно заполните все обязательные поля и загрузите фото"
                            } else {
                                generalError = null
                                screenModel.register(
                                    surname = surname,
                                    name = name,
                                    patronymic = patronymic,
                                    birthDate = birthDate,
                                    socialStatusIds = selectedSocialStatusIds,
                                    gender = gender,
                                    course = course,
                                    specialtyId = selectedSpecialtyId!!,
                                    groupId = selectedGroupId!!,
                                    corpEmail = corpEmail,
                                    corpDomain = corpDomain,
                                    addEmail = addEmail,
                                    phone = phone,
                                    vkLink = vkLink,
                                    pass = password,
                                    photoBytes = selectedImageBytes!!
                                )
                            }
                        }
                    )

                    generalError?.let { msg ->
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = contentModifier.padding(top = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = contentModifier
                    ) {
                        Text("Уже есть аккаунт? ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = "Войти",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.clickable {
                                component.onGoBack()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(60.dp))

                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing))
                }
            }
        }

        // ПОЛНОЭКРАННЫЙ ОВЕРЛЕЙ ПРЕДПРОСМОТРА
        // Так как он находится в конце Box, он будет нарисован поверх всего приложения
        // и идеально зайдет под челку и нижнюю панель навигации.
        val previewBytes = pendingImageBytes
        if (previewBytes != null) {
            val previewBitmap = remember(previewBytes) { previewBytes.toImageBitmap() }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                // Добавляем отступы из WindowInsets внутрь скролла
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() + 24.dp,
                            bottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding() + 24.dp,
                            start = 24.dp,
                            end = 24.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Предпросмотр фото",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Так ваше фото будет выглядеть в приложении. Лицо должно быть крупным и по центру, занимать 70-80% площади всей фотографии",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(40.dp))

                    Image(
                        bitmap = previewBitmap,
                        contentDescription = "Превью",
                        modifier = Modifier
                            .size(250.dp)
                            .clip(CircleShape)
                            .border(4.dp, MaterialTheme.colorScheme.secondary, CircleShape),
                        contentScale = ContentScale.Crop // Автоматически обрезает из центра
                    )

                    Spacer(modifier = Modifier.height(60.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                pendingImageBytes = null
                                singleImagePicker.launch()
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = CircleShape,
                            border = BorderStroke(0.3.dp, MaterialTheme.colorScheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text(
                                text = "Другое фото",
                                color = MaterialTheme.colorScheme.outline,
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 10.sp)
                            )
                        }
                        CustomButton(
                            text = "Оставить",
                            onClick = {
                                selectedImageBytes = previewBytes
                                pendingImageBytes = null
                            },
                            modifier = Modifier.weight(1f).height(48.dp)
                        )
                    }
                }
            }
        }
    }
}