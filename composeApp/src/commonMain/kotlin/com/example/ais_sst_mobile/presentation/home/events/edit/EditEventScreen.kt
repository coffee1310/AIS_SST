package com.example.ais_sst_mobile.presentation.events.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ais_sst_mobile.navigation.EditEventComponent
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

import com.example.ais_sst_mobile.presentation.events.create.RoleCard
import com.example.ais_sst_mobile.presentation.events.create.TimeTransformation
import com.example.ais_sst_mobile.presentation.events.create.ToggleRow
import com.example.ais_sst_mobile.presentation.events.create.getRolePlural

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditEventScreen(component: EditEventComponent) {
    val koin = getKoin()
    val screenModel = remember { koin.get<EditEventScreenModel>() }

    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val isScreenLoading by screenModel.isScreenLoading.collectAsState()
    val isLoading by screenModel.isLoading.collectAsState()

    val title by screenModel.title.collectAsState()
    val description by screenModel.description.collectAsState()
    val date by screenModel.date.collectAsState()
    val startTime by screenModel.startTime.collectAsState()
    val endTime by screenModel.endTime.collectAsState()
    val venue by screenModel.venue.collectAsState()

    val isPublic by screenModel.isPublic.collectAsState()
    val isFree by screenModel.isFree.collectAsState()
    val isDraft by screenModel.isDraft.collectAsState()
    val maxParticipants by screenModel.maxParticipants.collectAsState()
    val selectedSectorIds by screenModel.selectedSectorIds.collectAsState()
    val roles by screenModel.roles.collectAsState()

    var expandedSectorsMenu by remember { mutableStateOf(false) }
    var isParticipantExpanded by remember { mutableStateOf(true) }

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
    val sectors by screenModel.sectors.collectAsState()

    LaunchedEffect(component.eventId) {
        screenModel.loadEventData(component.eventId)
    }

    LaunchedEffect(Unit) {
        screenModel.effect.collect { effect ->
            when (effect) {
                is EditEventEffect.NavigateBack -> component.onGoBack()
                is EditEventEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    val selectedSectorsText = remember(selectedSectorIds, sectors) {
        if (selectedSectorIds.isEmpty()) "Без ограничений (Все сектора)"
        else sectors.filter { it.id in selectedSectorIds }.joinToString(", ") { it.title }
    }

    val imagePicker = rememberImagePickerLauncher(
        selectionMode = SelectionMode.Single,
        scope = coroutineScope,
        resizeOptions = ResizeOptions(width = 800, height = 800, compressionQuality = 0.8),
        onResult = { byteArrays ->
            byteArrays.firstOrNull()?.let { bytes ->
                if (bytes.size > 2 * 1024 * 1024) screenModel.showError("Фото слишком большое! Выберите другое")
                else screenModel.updateEventPhoto(bytes)
            }
        }
    )

    val glassBackgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)

    if (showDatePicker || activeRoleDateIndex != null) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false; activeRoleDateIndex = null },
            confirmButton = {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { showDatePicker = false; activeRoleDateIndex = null }) {
                        Text("Отмена", color = MaterialTheme.colorScheme.onSurface)
                    }
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val localDate = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
                            val formattedDate = "${localDate.dayOfMonth.toString().padStart(2, '0')}${localDate.monthNumber.toString().padStart(2, '0')}${localDate.year}"
                            if (showDatePicker) screenModel.date.value = formattedDate
                            else if (activeRoleDateIndex != null) {
                                val role = roles[activeRoleDateIndex!!].copy(deadlineDate = formattedDate)
                                screenModel.updateRole(activeRoleDateIndex!!, role)
                            }
                        }
                        showDatePicker = false; activeRoleDateIndex = null
                    }) { Text("ОК", color = MaterialTheme.colorScheme.secondary) }
                }
            },
            dismissButton = null
        ) { DatePicker(state = datePickerState) }
    }

    if (showStartTimePicker || showEndTimePicker || activeRoleTimeIndex != null) {
        val timePickerState = rememberTimePickerState()
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false; showEndTimePicker = false; activeRoleTimeIndex = null },
            confirmButton = {
                TextButton(onClick = {
                    val formattedTime = "${timePickerState.hour.toString().padStart(2, '0')}${timePickerState.minute.toString().padStart(2, '0')}"
                    if (showStartTimePicker) screenModel.startTime.value = formattedTime
                    else if (showEndTimePicker) screenModel.endTime.value = formattedTime
                    else if (activeRoleTimeIndex != null) {
                        val role = roles[activeRoleTimeIndex!!].copy(deadlineTime = formattedTime)
                        screenModel.updateRole(activeRoleTimeIndex!!, role)
                    }
                    showStartTimePicker = false; showEndTimePicker = false; activeRoleTimeIndex = null
                }) { Text("ОК", color = MaterialTheme.colorScheme.secondary) }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false; showEndTimePicker = false; activeRoleTimeIndex = null }) {
                    Text("Отмена", color = MaterialTheme.colorScheme.onSurface)
                }
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
                    .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)).padding(horizontal = 16.dp).height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CustomBackButton(onClick = component.onGoBack)
                    Text(
                        text = "Редактирование",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(40.dp))
                }

                if (isScreenLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Основная информация", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 12.dp))

                        CustomTextField(value = title, onValueChange = { if (it.length <= 128) screenModel.title.value = it }, placeholder = "* Название", keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = description, onValueChange = { screenModel.description.value = it },
                            placeholder = { Text("  Описание", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth(), minLines = 5, maxLines = 5, textStyle = MaterialTheme.typography.bodyLarge,
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = glassBackgroundColor, unfocusedContainerColor = glassBackgroundColor, focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface, cursorColor = MaterialTheme.colorScheme.primary),
                            shape = MaterialTheme.shapes.medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        CustomTextField(
                            value = date, onValueChange = { if (it.length <= 8 && it.all { char -> char.isDigit() }) screenModel.date.value = it }, placeholder = "* Дата (ДД.ММ.ГГГГ)",
                            trailingIcon = { IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Outlined.CalendarToday, "Выбрать дату", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp)) } },
                            visualTransformation = DateTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CustomTextField(
                                value = startTime, onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) screenModel.startTime.value = it }, placeholder = "* Начало", modifier = Modifier.weight(1f),
                                trailingIcon = { IconButton(onClick = { showStartTimePicker = true }) { Icon(Icons.Outlined.Schedule, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp)) } },
                                visualTransformation = TimeTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                            )
                            CustomTextField(
                                value = endTime, onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) screenModel.endTime.value = it }, placeholder = "* Конец", modifier = Modifier.weight(1f),
                                trailingIcon = { IconButton(onClick = { showEndTimePicker = true }) { Icon(Icons.Outlined.Schedule, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp)) } },
                                visualTransformation = TimeTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        CustomTextField(
                            value = venue, onValueChange = { if (it.length <= 255) screenModel.venue.value = it }, placeholder = "* Место",
                            trailingIcon = { Icon(Icons.Outlined.LocationOn, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp)) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Организаторы", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 12.dp, top = 12.dp))

                        if (selectedOrganizers.isNotEmpty()) {
                            FlowRow(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                selectedOrganizers.forEach { organizer ->
                                    InputChip(
                                        selected = false, onClick = { screenModel.removeOrganizer(organizer.id) },
                                        label = { Text("${organizer.surname} ${organizer.name}", style = MaterialTheme.typography.labelSmall) },
                                        trailingIcon = { Icon(Icons.Default.Close, "Удалить", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.surfaceTint) },
                                        colors = InputChipDefaults.inputChipColors(containerColor = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.4f), labelColor = MaterialTheme.colorScheme.onSurface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceTint)
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            CustomTextField(
                                value = searchQuery, onValueChange = { screenModel.updateSearchQuery(it) }, placeholder = "  Начните вводить ФИО...", keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                            )
                            AnimatedVisibility(visible = filteredActivists.isNotEmpty()) {
                                Card(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.4f)), border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.3f))) {
                                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                                        filteredActivists.forEachIndexed { index, activist ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().clickable { screenModel.addOrganizer(activist); focusManager.clearFocus() }.padding(horizontal = 16.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val avatarBytes: ByteArray? = remember(activist.photo) {
                                                    try {
                                                        activist.photo?.let { text -> text.substringAfter("base64,").trim().decodeBase64Bytes() }
                                                    } catch (e: Throwable) { null }
                                                }

                                                val avatarBitmap = remember(avatarBytes) {
                                                    try {
                                                        avatarBytes?.toImageBitmap()
                                                    } catch (e: Throwable) {
                                                        null
                                                    }
                                                }

                                                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                                    if (avatarBitmap != null) Image(bitmap = avatarBitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                                    else Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text("${activist.surname} ${activist.name}", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp), color = MaterialTheme.colorScheme.onSurface)
                                            }
                                            if (index < filteredActivists.size - 1) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 12.dp))
                                        }
                                    }
                                }
                            }
                        }

                        val hasOrganizerRole = roles.any { it.isOrganizer && !it.isDeleted }
                        if (!hasOrganizerRole) {
                            Text("+ Добавить заявку на организатора", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp).clickable { screenModel.addOrganizerRole() })
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Медиа и доступ", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 12.dp))

                        val photoBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        Box(
                            modifier = Modifier.fillMaxWidth().height(160.dp).clip(MaterialTheme.shapes.medium).background(glassBackgroundColor)
                                .drawBehind { if (eventPhotoBase64 == null) drawRoundRect(color = photoBorderColor, style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)), cornerRadius = CornerRadius(12.dp.toPx())) }
                                .clickable { imagePicker.launch() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (eventPhotoBase64 != null) {
                                val photoBytes: ByteArray? = remember(eventPhotoBase64) { try { eventPhotoBase64!!.decodeBase64Bytes() } catch (e: Exception) { null } }

                                val bitmap = remember(photoBytes) {
                                    try {
                                        photoBytes?.toImageBitmap()
                                    } catch (e: Throwable) {
                                        null
                                    }
                                }

                                if (bitmap != null) {
                                    Image(bitmap = bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.AddPhotoAlternate, "Ошибка", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(32.dp))
                                    }
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AddPhotoAlternate, "Добавить", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Обновить фото", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(colors = CardDefaults.cardColors(containerColor = glassBackgroundColor), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)), shape = MaterialTheme.shapes.medium) {
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                ToggleRow("Публичное событие", "Доступно всем активистам", Icons.Outlined.Public, MaterialTheme.colorScheme.secondary, isPublic) {
                                    screenModel.isPublic.value = it
                                    if (it) {
                                        screenModel.selectedSectorIds.value = emptySet()
                                    }
                                }
                                ToggleRow("Свободное событие", "Регистрация без отбора", Icons.Outlined.LockOpen, MaterialTheme.colorScheme.secondary, isFree) {
                                    screenModel.isFree.value = it
                                    if (!it) {
                                        // Очищаем данные участников, если событие больше не свободное
                                        screenModel.maxParticipants.value = ""
                                        screenModel.selectedSectorIds.value = emptySet()
                                    }
                                }
                                ToggleRow("Черновик", "Доступно только членам правления", Icons.Outlined.Edit, MaterialTheme.colorScheme.secondary, isDraft) { screenModel.isDraft.value = it }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                            Text("Роли и задачи", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text(getRolePlural(roles.count { !it.isDeleted } + if (isFree) 1 else 0), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }

                        AnimatedVisibility(visible = isFree) {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)), shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).animateContentSize()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Text("Участник", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                        IconButton(onClick = { isParticipantExpanded = !isParticipantExpanded }, modifier = Modifier.size(24.dp)) {
                                            Icon(if (isParticipantExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
                                        }
                                    }
                                    AnimatedVisibility(visible = isParticipantExpanded) {
                                        Column {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            CustomTextField(value = maxParticipants, onValueChange = { if (it.all { char -> char.isDigit() }) screenModel.maxParticipants.value = it }, placeholder = "* Макс. количество (0 - без ограничений)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next))
                                            AnimatedVisibility(visible = !isPublic) {
                                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                                    ExposedDropdownMenuBox(expanded = expandedSectorsMenu, onExpandedChange = { expandedSectorsMenu = !expandedSectorsMenu }) {
                                                        CustomTextField(modifier = Modifier.menuAnchor().fillMaxWidth(), value = selectedSectorsText, onValueChange = {}, readOnly = true, placeholder = "  Доступно для секторов", trailingIcon = { Icon(if (expandedSectorsMenu) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.secondary) })
                                                        ExposedDropdownMenu(expanded = expandedSectorsMenu, onDismissRequest = { expandedSectorsMenu = false }) {
                                                            sectors.forEach { sector ->
                                                                val isSelected = selectedSectorIds.contains(sector.id)
                                                                DropdownMenuItem(
                                                                    text = {
                                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                                            Checkbox(checked = isSelected, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary))
                                                                            Spacer(modifier = Modifier.width(8.dp))
                                                                            Text(sector.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                                                                        }
                                                                    },
                                                                    onClick = {
                                                                        screenModel.selectedSectorIds.value = if (isSelected) selectedSectorIds - sector.id else selectedSectorIds + sector.id
                                                                    }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        roles.forEachIndexed { index, role ->
                            androidx.compose.runtime.key(role.id) {
                                AnimatedVisibility(
                                    visible = !role.isDeleted,
                                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
                                ) {
                                    Column {
                                        RoleCard(
                                            role = role,
                                            globalRolesList = globalRoles,
                                            tealAccent = MaterialTheme.colorScheme.secondary,
                                            onRoleChange = { updatedRole -> screenModel.updateRole(index, updatedRole) },
                                            onRemove = { screenModel.removeRole(index) },
                                            onDateClick = { activeRoleDateIndex = index },
                                            onTimeClick = { activeRoleTimeIndex = index }
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                }
                            }
                        }

                        val addRoleBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        Box(
                            modifier = Modifier.fillMaxWidth().height(52.dp).clip(MaterialTheme.shapes.medium)
                                .drawBehind { drawRoundRect(color = addRoleBorderColor, style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)), cornerRadius = CornerRadius(12.dp.toPx())) }
                                .clickable { screenModel.addRole() },
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
                            text = if (isLoading) "Сохранение..." else "Сохранить изменения",
                            onClick = { if (!isLoading) screenModel.saveChanges(component.eventId) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 16.dp)) { CustomSnackbar(it) }
        }
    }
}