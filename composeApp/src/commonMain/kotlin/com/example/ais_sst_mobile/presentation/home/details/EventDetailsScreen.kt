package com.example.ais_sst_mobile.presentation.home.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ais_sst_mobile.core.prefs.SessionManager
import com.example.ais_sst_mobile.domain.model.AppRole
import com.example.ais_sst_mobile.domain.model.Organizer
import com.example.ais_sst_mobile.navigation.CoordinatorEventDetailsComponent
import com.example.ais_sst_mobile.presentation.components.AppBackground
import com.example.ais_sst_mobile.presentation.components.CustomSnackbar
import com.preat.peekaboo.image.picker.toImageBitmap
import io.ktor.util.decodeBase64Bytes
import org.koin.compose.getKoin

@Composable
fun EventDetailsScreen(component: CoordinatorEventDetailsComponent) {
    val koin = getKoin()
    val screenModel = remember { koin.get<EventDetailsScreenModel>() }
    val sessionManager = remember { koin.get<SessionManager>() }

    val activeRole by sessionManager.activeRoleFlow.collectAsState(initial = null)
    val state by screenModel.state.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(component.eventId, activeRole) {
        activeRole?.let { role ->
            screenModel.loadEvent(component.eventId, role)
        }
    }

    LaunchedEffect(Unit) {
        screenModel.effect.collect { effect ->
            when (effect) {
                is EventDetailsEffect.NavigateBack -> component.onGoBack()
                is EventDetailsEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val currentState = state) {
            is CoordinatorEventDetailsState.Loading -> {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is CoordinatorEventDetailsState.Error -> {
                Text(
                    text = currentState.message,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp)
                )
            }
            is CoordinatorEventDetailsState.Success -> {
                val event = currentState.event
                val roles = currentState.roles
                val creator = currentState.creator
                val showApps = currentState.showApplications
                val showMgmt = currentState.showManagementSection
                val showEdit = currentState.showEditButton
                val showFinish = currentState.showFinishButton
                val showDelete = currentState.showDeleteButton
                val showReport = currentState.showReportButton

                val imageBitmap = remember(event.photoBase64) {
                    try {
                        event.photoBase64?.let { rawString ->
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
                    } catch (e: Throwable) { null }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(Color(0xFF2A2346))
                    ) {
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = event.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CalendarToday, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(event.dateStrDetails, style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp), color = MaterialTheme.colorScheme.onSurface)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.LocationOn, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(event.venue, style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp), color = MaterialTheme.colorScheme.onSurface)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = event.description,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            lineHeight = 22.sp
                        )

                        // --- БЛОК: СОЗДАТЕЛЬ МЕРОПРИЯТИЯ ---
                        if (creator != null) {
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                text = "Создатель мероприятия",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            GroupedOrganizerCard(listOf(creator)) { userId ->
                                component.onNavigateToActivistProfile(userId)
                            }
                        }

                        // --- БЛОК: ОРГАНИЗАТОРЫ ---
                        if (event.organizers.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                text = if (event.organizers.size > 1) "Организаторы" else "Организатор",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            GroupedOrganizerCard(event.organizers) { userId ->
                                component.onNavigateToActivistProfile(userId)
                            }
                        }

                        // Дополнительный воздух перед заголовком ролей
                        if (creator == null && event.organizers.isEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                        } else {
                            Spacer(modifier = Modifier.height(32.dp))
                        }

                        Text(
                            text = "Роли и задачи",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        if (showApps && event.maxOrganizersCount > event.currentOrganizersCount) {
                            val available = event.maxOrganizersCount - event.currentOrganizersCount
                            RoleDetailCard(
                                title = "Организатор",
                                description = "Помощь в планировании, подготовке и проведении мероприятия. Организаторы получают доступ к управлению событием и контролю участников.",
                                deadline = null,
                                accentText = "Свободных мест: $available из ${event.maxOrganizersCount}"
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        if (showApps && event.isFreeEvent) {
                            val limitText = if (event.maxParticipantsCount > 0) {
                                "${event.currentParticipantsCount} из ${event.maxParticipantsCount}"
                            } else {
                                "${event.currentParticipantsCount} (без ограничений)"
                            }

                            val sectorInfo = if (event.isPublic) {
                                "\nДоступно всем (публичное мероприятие)"
                            } else if (!event.sectorTitle.isNullOrBlank()) {
                                "\nСектора: ${event.sectorTitle}"
                            } else {
                                "\nДоступно для всех секторов"
                            }

                            RoleDetailCard(
                                title = "Участник",
                                description = "Регистрация проходит без отбора. Вы автоматически станете участником мероприятия после подачи заявки.",
                                deadline = null,
                                accentText = "Занято мест: $limitText$sectorInfo"
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        roles.forEach { role ->
                            val deadlineFormatted = try {
                                val parts = role.deadline.split("T")
                                val dateParts = parts[0].split("-")
                                val time = parts[1].take(5)
                                "${dateParts[2]}.${dateParts[1]}.${dateParts[0]}, $time"
                            } catch (e: Exception) { role.deadline }

                            val occupied = role.totalOccupiedSlots ?: 0
                            val available = role.totalAvailableSlots ?: role.capacity
                            val slotsInfo = "Занято мест: $occupied из $available"

                            RoleDetailCard(
                                title = role.globalEventRoleTitle,
                                description = role.description,
                                deadline = "Дедлайн: $deadlineFormatted",
                                accentText = slotsInfo
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Доступ",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)),
                            border = BorderStroke(0.2.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                                ReadOnlyToggleRow("Публичное событие", "Доступно всем активистам", Icons.Outlined.Public, MaterialTheme.colorScheme.secondary, event.isPublic)
                                ReadOnlyToggleRow("Свободное событие", "Регистрация без отбора", Icons.Outlined.LockOpen, MaterialTheme.colorScheme.secondary, event.isFreeEvent)
                                ReadOnlyToggleRow("Черновик", "Доступно только членам правления", Icons.Outlined.Edit, MaterialTheme.colorScheme.secondary, event.isDraft)
                            }
                        }

                        if (showMgmt) {
                            Spacer(modifier = Modifier.height(32.dp))

                            Text(
                                text = "Управление",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            if (showReport) {
                                Button(
                                    onClick = { /* TODO: Выгрузить отчет */ },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = MaterialTheme.shapes.large,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Выгрузить отчёт", style = MaterialTheme.typography.titleMedium.copy(fontSize = 12.sp), textAlign = TextAlign.Center)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            if (showFinish) {
                                Button(
                                    onClick = { /* TODO: Завершить мероприятие */ },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = MaterialTheme.shapes.large,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                ) {
                                    Text(
                                        text = "Завершить\nмероприятие",
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 12.sp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            if (showEdit) {
                                Button(
                                    onClick = { /* TODO: Редактировать */ },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = MaterialTheme.shapes.large,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Редактировать\nмероприятие", style = MaterialTheme.typography.titleMedium.copy(fontSize = 12.sp), textAlign = TextAlign.Center)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            if (showDelete) {
                                OutlinedButton(
                                    onClick = { showDeleteDialog = true },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = MaterialTheme.shapes.large,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                                ) {
                                    Text("Удалить\nмероприятие", style = MaterialTheme.typography.titleMedium.copy(fontSize = 12.sp), textAlign = TextAlign.Center)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }

                if (showDeleteDialog) {
                    DeleteEventDialog(
                        onDismiss = { showDeleteDialog = false },
                        onConfirm = {
                            showDeleteDialog = false
                            screenModel.deleteEvent(event.id)
                        }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            snackbar = { snackbarData ->
                CustomSnackbar(snackbarData = snackbarData)
            }
        )
    }
}

@Composable
fun GroupedOrganizerCard(organizers: List<Organizer>, onOrganizerClick: ((Int) -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)),
        border = BorderStroke(0.2.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            organizers.forEachIndexed { index, organizer ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (onOrganizerClick != null) Modifier.clickable { onOrganizerClick(organizer.userId) }
                            else Modifier
                        )
                ) {
                    val avatarBitmap = remember(organizer.userPhoto) {
                        try {
                            organizer.userPhoto?.let { rawString ->
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
                        } catch (e: Throwable) { null }
                    }

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)),
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
                                Icons.Outlined.PersonOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        val fullName = listOfNotNull(organizer.userSurname, organizer.userName).joinToString(" ")
                        Text(
                            text = fullName,
                            style = MaterialTheme.typography.displayMedium.copy(fontSize = 15.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = organizer.groupInfo,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 2
                        )
                    }
                }

                if (index < organizers.lastIndex) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun RoleDetailCard(title: String, description: String?, deadline: String?, accentText: String? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)),
        border = BorderStroke(0.2.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )
            }

            if (accentText != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = accentText,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.secondary,
                    lineHeight = 18.sp
                )
            }

            if (deadline != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(deadline, style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
fun ReadOnlyToggleRow(title: String, subtitle: String, icon: ImageVector, iconTint: Color, checked: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp), color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.secondary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
fun DeleteEventDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(0.3.dp, MaterialTheme.colorScheme.outline),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Удаление мероприятия",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Вы уверены, что хотите удалить это мероприятие? Отменить это действие будет невозможно.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "Отмена",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedButton(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "Удалить",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSecondary,
                        )
                    }
                }
            }
        }
    }
}