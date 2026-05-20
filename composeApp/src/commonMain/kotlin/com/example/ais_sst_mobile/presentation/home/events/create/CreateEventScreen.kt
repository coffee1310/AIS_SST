package com.example.ais_sst_mobile.presentation.events.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ais_sst_mobile.data.network.dto.RoleDto
import com.example.ais_sst_mobile.navigation.CreateEventComponent
import com.example.ais_sst_mobile.presentation.components.AppBackground
import com.example.ais_sst_mobile.presentation.components.CustomBackButton
import com.example.ais_sst_mobile.presentation.components.CustomButton
import com.example.ais_sst_mobile.presentation.components.CustomSnackbar
import com.example.ais_sst_mobile.presentation.components.CustomTextField
import com.example.ais_sst_mobile.presentation.components.utils.DateTransformation
import com.preat.peekaboo.image.picker.ResizeOptions
import com.preat.peekaboo.image.picker.SelectionMode
import com.preat.peekaboo.image.picker.rememberImagePickerLauncher
import com.preat.peekaboo.image.picker.toImageBitmap
import io.ktor.util.decodeBase64Bytes
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.getKoin

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
    val isExpanded: Boolean = true,
    val globalRoleId: Int? = null
)

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
    val koin = getKoin()
    val screenModel = remember { koin.get<CreateEventScreenModel>() }

    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
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

    val eventPhotoBase64 by screenModel.eventPhotoBase64.collectAsState()
    val searchQuery by screenModel.searchQuery.collectAsState()
    val filteredActivists by screenModel.filteredActivists.collectAsState()
    val selectedOrganizers by screenModel.selectedOrganizers.collectAsState()
    val globalRoles by screenModel.globalRoles.collectAsState()
    val isLoading by screenModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        screenModel.effect.collect { effect ->
            when (effect) {
                is CreateEventEffect.NavigateBack -> component.onGoBack()
                is CreateEventEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

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
                    screenModel.showError("Фото слишком большое! Выберите другое")
                } else {
                    screenModel.updateEventPhoto(bytes)
                }
            }
        }
    )

    val glassBackgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)

    if (showDatePicker || activeRoleDateIndex != null) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
                activeRoleDateIndex = null
            },
            confirmButton = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = {
                        showDatePicker = false
                        activeRoleDateIndex = null
                    }) {
                        Text("Отмена", color = MaterialTheme.colorScheme.onSurface)
                    }

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
                    }) {
                        Text("ОК", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            },
            dismissButton = null
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
            text = { TimePicker(state = timePickerState) }
        )
    }

    AppBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { focusManager.clearFocus() })
                    }
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
                                Icon(Icons.Outlined.CalendarToday, "Выбрать дату", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
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
                                    Icon(Icons.Outlined.Schedule, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
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
                                    Icon(Icons.Outlined.Schedule, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
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
                        trailingIcon = { Icon(Icons.Outlined.LocationOn, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp)) },
                    )

                    Spacer(modifier = Modifier.height(12.dp))

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
                                    onClick = { screenModel.removeOrganizer(organizer) },
                                    label = {
                                        Text(
                                            text = organizer.surname + " " + organizer.name,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(Icons.Default.Close, "Удалить", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.surfaceTint)
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
                            value = searchQuery,
                            onValueChange = { screenModel.updateSearchQuery(it) },
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
                                                    screenModel.addOrganizer(activist)
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
                                                    Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Text(
                                                text = "${activist.surname} ${activist.name}",
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
                            color = MaterialTheme.colorScheme.secondary,
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
                                Icon(Icons.Default.AddPhotoAlternate, "Добавить", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Добавить", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
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
                            ToggleRow("Публичное событие", "Доступно всем активистам", Icons.Outlined.Public, MaterialTheme.colorScheme.secondary, isPublic) { isPublic = it }
                            ToggleRow("Свободное событие", "Регистрация без отбора", Icons.Outlined.LockOpen, MaterialTheme.colorScheme.secondary, isFree) { isFree = it }
                            ToggleRow("Черновик", "Доступно только членам правления", Icons.Outlined.Edit, MaterialTheme.colorScheme.secondary, isDraft) { isDraft = it }
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
                            globalRolesList = globalRoles,
                            tealAccent = MaterialTheme.colorScheme.secondary,
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

                    CustomButton(
                        text = if (isLoading) "Создание..." else "Создать мероприятие",
                        onClick = {
                            if (!isLoading) {
                                screenModel.createEvent(
                                    title = title,
                                    description = description,
                                    date = date,
                                    startTime = startTime,
                                    endTime = endTime,
                                    venue = venue,
                                    isPublic = isPublic,
                                    isDraft = isDraft,
                                    roles = roles
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) { data ->
                CustomSnackbar(data)
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
    globalRolesList: List<RoleDto>,
    tealAccent: Color,
    onRoleChange: (RoleUiModel) -> Unit,
    onRemove: () -> Unit,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit
) {
    val glassBg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)
    var selectedRoleInfo by remember { mutableStateOf<RoleDto?>(null) }

    selectedRoleInfo?.let { info ->
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { selectedRoleInfo = null },
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
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
                        text = info.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Сектор: ${info.sectorTitle ?: "Не указан"}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = info.description?.takeIf { it.isNotBlank() } ?: "Описание отсутствует",
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(24.dp))

                    OutlinedButton(
                        onClick = { selectedRoleInfo = null },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Text("Закрыть", style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp))
                    }
                }
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
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
                        IconButton(onClick = { onRoleChange(role.copy(isGlobalSelected = false, name = "", sector = "")) }) {
                            Icon(Icons.Default.Edit, "Изменить роль", tint = tealAccent, modifier = Modifier.size(20.dp))
                        }
                    }
                } else {
                    CustomTextField(
                        value = role.name,
                        onValueChange = { onRoleChange(role.copy(name = it)) },
                        placeholder = "* Название роли",
                        modifier = Modifier.weight(1f)
                    )
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

            if (!role.isOrganizer && !role.isGlobalSelected) {
                val filteredRoles = globalRolesList
                    .filter { it.title.contains(role.name, ignoreCase = true) }
                    .sortedBy { it.title }

                AnimatedVisibility(visible = filteredRoles.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = if (role.isExpanded) 8.dp else 0.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.4f)),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            filteredRoles.forEachIndexed { index, globalRole ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onRoleChange(
                                                role.copy(
                                                    name = globalRole.title,
                                                    isGlobalSelected = true,
                                                    sector = globalRole.sectorTitle ?: "Нет сектора",
                                                    globalRoleId = globalRole.id
                                                )
                                            )
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = globalRole.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )

                                    IconButton(
                                        onClick = { selectedRoleInfo = globalRole },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Outlined.Info, "Информация", tint = tealAccent, modifier = Modifier.size(20.dp))
                                    }
                                }

                                if (index < filteredRoles.size - 1) {
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

            AnimatedVisibility(visible = role.isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    OutlinedTextField(
                        value = role.tasks,
                        onValueChange = { onRoleChange(role.copy(tasks = it)) },
                        placeholder = { Text("  Задачи", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
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
                            placeholder = "* Кол-во людей",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        CustomTextField(
                            value = role.points,
                            onValueChange = { onRoleChange(role.copy(points = it)) },
                            placeholder = "* Кол-во баллов",
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

                        Spacer(modifier = Modifier.height(12.dp))

                        CustomTextField(
                            value = role.sector,
                            onValueChange = { },
                            readOnly = true,
                            placeholder = "  Ответственный сектор",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}