package com.example.ais_sst_mobile.presentation.events.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ais_sst_mobile.navigation.CreateEventComponent
import com.example.ais_sst_mobile.presentation.components.AppBackground
import com.example.ais_sst_mobile.presentation.components.CustomBackButton
import com.example.ais_sst_mobile.presentation.components.CustomButton
import com.example.ais_sst_mobile.presentation.components.CustomTextField
import com.example.ais_sst_mobile.presentation.components.clearFocusOnScroll
import com.example.ais_sst_mobile.presentation.components.clearFocusOnTap
import com.example.ais_sst_mobile.presentation.components.utils.DateTransformation
import com.preat.peekaboo.image.picker.ResizeOptions
import com.preat.peekaboo.image.picker.SelectionMode
import com.preat.peekaboo.image.picker.rememberImagePickerLauncher
import com.preat.peekaboo.image.picker.toImageBitmap
import io.ktor.util.decodeBase64Bytes
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping

// --- Вспомогательные классы и функции ---

// Моковая модель пользователя (заменишь на свою из домена)
data class MockUser(val id: Int, val fullName: String, val surname: String, val name: String, val photo: String? = null)

data class RoleUiModel(
    val name: String = "",
    val tasks: String = "",
    val deadlineDate: String = "",
    val deadlineTime: String = "",
    val peopleCount: String = "",
    val points: String = "",
    val reserveCount: String = "",
    val sector: String = "",
    val isOrganizer: Boolean = false,
    val isGlobalSelected: Boolean = false,
    val isExpanded: Boolean = true
)

// Визуальная трансформация для времени (HH:mm)
class TimeTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 4) text.text.substring(0..3) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 1) out += ":"
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 1) return offset
                if (offset <= 4) return offset + 1
                return 5
            }
            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 2) return offset
                if (offset <= 5) return offset - 1
                return 4
            }
        }
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

// Функция для склонения слова "роль"
fun getRolePlural(count: Int): String {
    val mod10 = count % 10
    val mod100 = count % 100
    return when {
        mod100 in 11..19 -> "$count ролей"
        mod10 == 1 -> "$count роль"
        mod10 in 2..4 -> "$count роли"
        else -> "$count ролей"
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateEventScreen(component: CreateEventComponent) {
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var venue by remember { mutableStateOf("") }

    var isPublic by remember { mutableStateOf(true) }
    var isFree by remember { mutableStateOf(false) }
    var isDraft by remember { mutableStateOf(false) }

    var roles by remember { mutableStateOf(listOf(RoleUiModel())) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    var activeRoleDateIndex by remember { mutableStateOf<Int?>(null) }
    var activeRoleTimeIndex by remember { mutableStateOf<Int?>(null) }

    // Состояния для фото
    var eventPhotoBase64 by remember { mutableStateOf<String?>(null) }
    val imagePicker = rememberImagePickerLauncher(
        selectionMode = SelectionMode.Single,
        scope = coroutineScope,
        resizeOptions = ResizeOptions(
            width = 800,
            height = 800,
            compressionQuality = 0.8
        ),
        onResult = { byteArrays ->
            byteArrays.firstOrNull()?.let { bytes ->
                val maxSizeInBytes = 2 * 1024 * 1024
                if (bytes.size > maxSizeInBytes) {
                    // TODO: Показать ошибку (screenModel.showError)
                } else {
                    //eventPhotoBase64 = io.ktor.util.encodeBase64(bytes)
                }
            }
        }
    )

    // Состояния для организаторов (TODO: перенести в ScreenModel)
    var organizerSearchQuery by remember { mutableStateOf("") }
    var selectedOrganizers by remember { mutableStateOf(listOf<MockUser>()) }
    // Моковые данные для поиска
    val allActivists = listOf(
        MockUser(1, "Иванов Иван Иванович", "Иванов", "Иван"),
        MockUser(2, "Петров Петр Петрович", "Петров", "Петр")
    )
    val filteredActivists = remember(organizerSearchQuery) {
        if (organizerSearchQuery.isBlank()) emptyList()
        else allActivists.filter { it.fullName.contains(organizerSearchQuery, ignoreCase = true) }
    }


    val glassBackgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)
    val tealAccent = Color(0xFF00BFA5)

    if (showDatePicker || activeRoleDateIndex != null) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
                activeRoleDateIndex = null
            },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val instant = Instant.fromEpochMilliseconds(millis)
                        val localDate = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                        val day = localDate.dayOfMonth.toString().padStart(2, '0')
                        val month = localDate.monthNumber.toString().padStart(2, '0')
                        val year = localDate.year.toString()
                        val formattedDate = "$day$month$year"

                        if (showDatePicker) {
                            date = formattedDate
                        } else if (activeRoleDateIndex != null) {
                            val rolesMut = roles.toMutableList()
                            rolesMut[activeRoleDateIndex!!] = rolesMut[activeRoleDateIndex!!].copy(deadlineDate = formattedDate)
                            roles = rolesMut
                        }
                    }
                    showDatePicker = false
                    activeRoleDateIndex = null
                }) { Text("ОК", color = MaterialTheme.colorScheme.secondary) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    activeRoleDateIndex = null
                }) { Text("Отмена", color = MaterialTheme.colorScheme.onSurface) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showStartTimePicker || showEndTimePicker || activeRoleTimeIndex != null) {
        val timePickerState = rememberTimePickerState()
        AlertDialog(
            onDismissRequest = {
                showStartTimePicker = false
                showEndTimePicker = false
                activeRoleTimeIndex = null
            },
            confirmButton = {
                TextButton(onClick = {
                    val formattedTime = "${timePickerState.hour.toString().padStart(2, '0')}${timePickerState.minute.toString().padStart(2, '0')}"

                    if (showStartTimePicker) startTime = formattedTime
                    else if (showEndTimePicker) endTime = formattedTime
                    else if (activeRoleTimeIndex != null) {
                        val rolesMut = roles.toMutableList()
                        rolesMut[activeRoleTimeIndex!!] = rolesMut[activeRoleTimeIndex!!].copy(deadlineTime = formattedTime)
                        roles = rolesMut
                    }

                    showStartTimePicker = false
                    showEndTimePicker = false
                    activeRoleTimeIndex = null
                }) { Text("ОК", color = MaterialTheme.colorScheme.secondary) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showStartTimePicker = false
                    showEndTimePicker = false
                    activeRoleTimeIndex = null
                }) { Text("Отмена", color = MaterialTheme.colorScheme.onSurface) }
            },
            text = {
                // Возвращаем циферблат
                TimePicker(state = timePickerState)
            }
        )
    }

    AppBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clearFocusOnTap(focusManager)
                    //.clearFocusOnScroll(focusManager)
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                        .padding(horizontal = 16.dp)
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CustomBackButton(onClick = component.onGoBack)
                    Text(
                        text = "Создание мероприятия",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(40.dp))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Основная информация",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    CustomTextField(
                        value = title,
                        onValueChange = { if (it.length <= 128) title = it },
                        placeholder = "* Название мероприятия",
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = {
                            Text("  Описание мероприятия", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5,
                        maxLines = 5,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = glassBackgroundColor,
                            unfocusedContainerColor = glassBackgroundColor,
                            focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = MaterialTheme.shapes.medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CustomTextField(
                        value = date,
                        onValueChange = { if (it.length <= 8 && it.all { char -> char.isDigit() }) date = it },
                        placeholder = "* Дата (ДД.ММ.ГГГГ)",
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Outlined.CalendarToday, "Выбрать дату", tint = tealAccent, modifier = Modifier.size(20.dp))
                            }
                        },
                        visualTransformation = DateTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CustomTextField(
                            value = startTime,
                            onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) startTime = it },
                            placeholder = "* Время начала",
                            modifier = Modifier.weight(1f),
                            trailingIcon = {
                                IconButton(onClick = { showStartTimePicker = true }) {
                                    Icon(Icons.Outlined.Schedule, null, tint = tealAccent, modifier = Modifier.size(20.dp))
                                }
                            },
                            visualTransformation = TimeTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        )

                        CustomTextField(
                            value = endTime,
                            onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) endTime = it },
                            placeholder = "* Время конца",
                            modifier = Modifier.weight(1f),
                            trailingIcon = {
                                IconButton(onClick = { showEndTimePicker = true }) {
                                    Icon(Icons.Outlined.Schedule, null, tint = tealAccent, modifier = Modifier.size(20.dp))
                                }
                            },
                            visualTransformation = TimeTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    CustomTextField(
                        value = venue,
                        onValueChange = { if (it.length <= 255) venue = it },
                        placeholder = "* Место проведения",
                        trailingIcon = { Icon(Icons.Outlined.LocationOn, null, tint = tealAccent, modifier = Modifier.size(20.dp)) },
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // === ОРГАНИЗАТОРЫ ===
                    Text(
                        text = "Организаторы",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp, top = 12.dp)
                    )

                    if (selectedOrganizers.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectedOrganizers.forEach { organizer ->
                                InputChip(
                                    selected = false,
                                    onClick = { selectedOrganizers = selectedOrganizers.filter { it.id != organizer.id } },
                                    label = {
                                        Text(
                                            text = organizer.surname + " " + organizer.name,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Удалить",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.surfaceTint
                                        )
                                    },
                                    colors = InputChipDefaults.inputChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.4f),
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceTint)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        CustomTextField(
                            value = organizerSearchQuery,
                            onValueChange = { organizerSearchQuery = it },
                            placeholder = "  Начните вводить ФИО организатора...",
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                        )

                        AnimatedVisibility(visible = filteredActivists.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                shape = MaterialTheme.shapes.extraLarge,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.4f)),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 240.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    filteredActivists.forEachIndexed { index, activist ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (!selectedOrganizers.any { it.id == activist.id }) {
                                                        selectedOrganizers = selectedOrganizers + activist
                                                    }
                                                    organizerSearchQuery = "" // Очищаем поиск после выбора
                                                    focusManager.clearFocus()
                                                }
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val avatarBitmap = remember(activist.photo) {
                                                try {
                                                    activist.photo?.let { rawString ->
                                                        var textToDecode = rawString.trim()
                                                        if (textToDecode.startsWith("ZGF0Y")) {
                                                            val decodedText = textToDecode.decodeBase64Bytes().decodeToString()
                                                            if (decodedText.startsWith("data:image")) textToDecode = decodedText
                                                        }
                                                        if (textToDecode.contains("base64,")) {
                                                            textToDecode = textToDecode.substringAfter("base64,").trim()
                                                        }
                                                        val bytes = textToDecode.decodeBase64Bytes()
                                                        if (bytes.isNotEmpty()) bytes.toImageBitmap() else null
                                                    }
                                                } catch (e: Throwable) {
                                                    e.printStackTrace()
                                                    null
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (avatarBitmap != null) {
                                                    Image(
                                                        bitmap = avatarBitmap,
                                                        contentDescription = "Аватар",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Icon(
                                                        Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Text(
                                                text = activist.fullName,
                                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        if (index < filteredActivists.size - 1) {
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                                modifier = Modifier.padding(horizontal = 12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val hasOrganizerRole = roles.any { it.isOrganizer }
                    if (!hasOrganizerRole) {
                        Text(
                            text = "+ Создать заявку на организатора",
                            color = tealAccent,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .clickable {
                                    roles = roles + RoleUiModel(name = "Организатор", isOrganizer = true, isGlobalSelected = true)
                                }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Медиа и доступ",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // === ФОТО ===
                    val strokeColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(glassBackgroundColor)
                            .drawBehind {
                                if (eventPhotoBase64 == null) {
                                    drawRoundRect(
                                        color = strokeColor,
                                        style = Stroke(
                                            width = 3f,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                                        ),
                                        cornerRadius = CornerRadius(12.dp.toPx())
                                    )
                                }
                            }
                            .clickable {
                                imagePicker.launch()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (eventPhotoBase64 != null) {
                            val bitmap = remember(eventPhotoBase64) {
                                try {
                                    eventPhotoBase64!!.decodeBase64Bytes().toImageBitmap()
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = "Фото мероприятия",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Добавить",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Добавить",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = glassBackgroundColor),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            ToggleRow("Публичное событие", "Доступно всем активистам", Icons.Outlined.Public, tealAccent, isPublic) { isPublic = it }
                            ToggleRow("Свободное событие", "Регистрация без отбора", Icons.Outlined.LockOpen, tealAccent, isFree) { isFree = it }
                            ToggleRow("Черновик", "Доступно только членам правления", Icons.Outlined.Edit, tealAccent, isDraft) { isDraft = it }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                        Text("Роли и задачи", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text(getRolePlural(roles.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }

                    roles.forEachIndexed { index, role ->
                        RoleCard(
                            role = role,
                            tealAccent = tealAccent,
                            onRoleChange = { updatedRole -> roles = roles.toMutableList().apply { this[index] = updatedRole } },
                            onRemove = { roles = roles.filterIndexed { i, _ -> i != index } },
                            onDateClick = { activeRoleDateIndex = index },
                            onTimeClick = { activeRoleTimeIndex = index }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    val dashColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .drawBehind {
                                drawRoundRect(
                                    color = dashColor,
                                    style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)),
                                    cornerRadius = CornerRadius(12.dp.toPx())
                                )
                            }
                            .clickable { roles = roles + RoleUiModel() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AddCircleOutline, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Добавить роль", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    CustomButton(text = "Создать мероприятие", onClick = { /* TODO: Submit */ }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
fun ToggleRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = iconTint,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleCard(
    role: RoleUiModel,
    tealAccent: Color,
    onRoleChange: (RoleUiModel) -> Unit,
    onRemove: () -> Unit,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit
) {
    val glassBg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)
    val globalRoles = listOf("Фотограф", "Дизайнер", "СММ", "Куратор", "Медиа", "Волонтёр", "Копирайтер")
    var expandedRoleSearch by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                if (role.isOrganizer || role.isGlobalSelected) {
                    Text(
                        text = role.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (!role.isOrganizer) {
                        IconButton(onClick = { onRoleChange(role.copy(isGlobalSelected = false, name = "")) }) {
                            Icon(Icons.Default.Edit, "Изменить роль", tint = tealAccent, modifier = Modifier.size(20.dp))
                        }
                    }
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expandedRoleSearch,
                        onExpandedChange = { expandedRoleSearch = !expandedRoleSearch },
                        modifier = Modifier.weight(1f)
                    ) {
                        CustomTextField(
                            value = role.name,
                            onValueChange = { onRoleChange(role.copy(name = it)) },
                            placeholder = "* Название роли",
                            modifier = Modifier.menuAnchor()
                        )
                        val filteredRoles = globalRoles.filter { it.contains(role.name, ignoreCase = true) }
                        if (filteredRoles.isNotEmpty() && expandedRoleSearch) {
                            ExposedDropdownMenu(expanded = expandedRoleSearch, onDismissRequest = { expandedRoleSearch = false }) {
                                filteredRoles.forEach { globalRoleName ->
                                    DropdownMenuItem(
                                        text = { Text(globalRoleName, style = MaterialTheme.typography.labelMedium) },
                                        onClick = {
                                            onRoleChange(role.copy(name = globalRoleName, isGlobalSelected = true))
                                            expandedRoleSearch = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = { onRoleChange(role.copy(isExpanded = !role.isExpanded)) }, modifier = Modifier.size(24.dp)) {
                    Icon(if (role.isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = tealAccent)
                }
            }

            AnimatedVisibility(visible = role.isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    OutlinedTextField(
                        value = role.tasks,
                        onValueChange = { onRoleChange(role.copy(tasks = it)) },
                        placeholder = { Text("* Задачи", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 4,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = glassBg,
                            unfocusedContainerColor = glassBg,
                            focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = MaterialTheme.shapes.medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CustomTextField(
                            value = role.deadlineDate,
                            onValueChange = { if (it.length <= 8 && it.all { char -> char.isDigit() }) onRoleChange(role.copy(deadlineDate = it)) },
                            placeholder = "* Дедлайн (дата)",
                            modifier = Modifier.weight(1f),
                            trailingIcon = {
                                IconButton(onClick = onDateClick) {
                                    Icon(Icons.Outlined.CalendarToday, "Выбрать дату", tint = tealAccent, modifier = Modifier.size(20.dp))
                                }
                            },
                            visualTransformation = DateTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        CustomTextField(
                            value = role.deadlineTime,
                            onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) onRoleChange(role.copy(deadlineTime = it)) },
                            placeholder = "* Дедлайн (время)",
                            modifier = Modifier.weight(1f),
                            trailingIcon = {
                                IconButton(onClick = onTimeClick) {
                                    Icon(Icons.Outlined.Schedule, "Выбрать время", tint = tealAccent, modifier = Modifier.size(20.dp))
                                }
                            },
                            visualTransformation = TimeTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CustomTextField(
                            value = role.peopleCount,
                            onValueChange = { onRoleChange(role.copy(peopleCount = it)) },
                            placeholder = "* Количество людей",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        CustomTextField(
                            value = role.points,
                            onValueChange = { onRoleChange(role.copy(points = it)) },
                            placeholder = "* Количество баллов",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!role.isOrganizer) {
                        Spacer(modifier = Modifier.height(12.dp))
                        CustomTextField(
                            value = role.reserveCount,
                            onValueChange = { onRoleChange(role.copy(reserveCount = it)) },
                            placeholder = "* Количество людей в резерв",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    CustomTextField(
                        value = role.sector,
                        onValueChange = { },
                        readOnly = true,
                        placeholder = "  Ответственный сектор",
                        //trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}