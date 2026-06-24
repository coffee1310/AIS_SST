package com.example.ais_sst_mobile.presentation.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ais_sst_mobile.navigation.TasksComponent
import com.example.ais_sst_mobile.presentation.components.CustomTextField
import com.example.ais_sst_mobile.presentation.components.clearFocusOnScroll
import com.example.ais_sst_mobile.presentation.components.clearFocusOnTap
import com.example.ais_sst_mobile.presentation.home.CustomTabRow
import com.preat.peekaboo.image.picker.toImageBitmap
import io.ktor.util.decodeBase64Bytes
import org.koin.compose.getKoin

// ==================== ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ====================

fun getRolePlural(count: Int, tabIndex: Int): String {
    val mod10 = count % 10
    val mod100 = count % 100

    val wordRole = when {
        mod100 in 11..19 -> "ролей"
        mod10 == 1 -> "роль"
        mod10 in 2..4 -> "роли"
        else -> "ролей"
    }

    val statusText = when (tabIndex) {
        0 -> "на рассмотрении"
        1 -> when {
            mod100 in 11..19 -> "принято"
            mod10 == 1 -> "принята"
            mod10 in 2..4 -> "приняты"
            else -> "принято"
        }
        2 -> when {
            mod100 in 11..19 -> "отклонено"
            mod10 == 1 -> "отклонена"
            mod10 in 2..4 -> "отклонены"
            else -> "отклонено"
        }
        else -> ""
    }
    return "$count $wordRole $statusText"
}

private fun formatDeadline(deadline: String?): String? {
    if (deadline.isNullOrBlank()) return null
    return try {
        val datePart = deadline.substringBefore("T")
        val timePart = deadline.substringAfter("T").take(5)
        val parts = datePart.split("-")
        if (parts.size == 3) {
            val day = parts[2].toInt()
            val month = when (parts[1]) {
                "01" -> "янв"; "02" -> "фев"; "03" -> "мар"; "04" -> "апр"
                "05" -> "мая"; "06" -> "июн"; "07" -> "июл"; "08" -> "авг"
                "09" -> "сен"; "10" -> "окт"; "11" -> "ноя"; "12" -> "дек"
                else -> ""
            }
            "$day $month, $timePart"
        } else null
    } catch (e: Exception) {
        null
    }
}

// ==================== ОСНОВНОЙ ЭКРАН ====================

@Composable
fun TasksScreen(component: TasksComponent) {
    val koin = getKoin()
    val screenModel = remember { koin.get<TasksScreenModel>() }
    val state by screenModel.state.collectAsState()
    val selectedTab by screenModel.selectedTab.collectAsState()
    val searchQuery by screenModel.searchQuery.collectAsState()

    val focusManager = LocalFocusManager.current

    val tabs = listOf(
        Pair("На рассмотрении", Icons.Outlined.Pending),
        Pair("Принято", Icons.Outlined.CheckCircle),
        Pair("Отклонены", Icons.Outlined.Cancel),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clearFocusOnTap(focusManager)
            .clearFocusOnScroll(focusManager)
    ) {
        Spacer(modifier = Modifier.height(5.dp))

        CustomTabRow(
            tabs = tabs,
            selectedTab = selectedTab,
            onTabSelected = { screenModel.selectTab(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Поисковая строка
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CustomTextField(
                value = searchQuery,
                onValueChange = { screenModel.updateSearchQuery(it) },
                placeholder = "Поиск по мероприятию",
                modifier = Modifier.weight(1f).height(52.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f),
                        MaterialTheme.shapes.medium
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        MaterialTheme.shapes.medium
                    )
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { /* TODO: Фильтры */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Фильтр",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (val currentState = state) {
            is TasksState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                }
            }
            is TasksState.Error -> {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = currentState.message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
            is TasksState.Success -> {
                val currentGroups = currentState.groups

                if (currentGroups.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(bottom = 60.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "Ничего не найдено" else "Здесь пока пусто",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(currentGroups) { group ->
                            TaskGroupCard(
                                group = group,
                                tabIndex = selectedTab,
                                onLeaveParticipant = { participantId ->
                                    screenModel.leaveParticipantRole(participantId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== КАРТОЧКА МЕРОПРИЯТИЯ ====================

@Composable
fun TaskGroupCard(
    group: EventTaskGroup,
    tabIndex: Int,
    onLeaveParticipant: (Int) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f)

    var showLeaveDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1633).copy(alpha = 0.75f)),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val imageBitmap = remember(group.eventPhoto) {
                    try {
                        group.eventPhoto?.let { raw ->
                            var base64String = raw.trim()
                            if (base64String.contains("base64,")) {
                                base64String = base64String.substringAfter("base64,").trim()
                            }
                            val bytes = base64String.decodeBase64Bytes()
                            if (bytes.isNotEmpty()) bytes.toImageBitmap() else null
                        }
                    } catch (e: Throwable) {
                        null
                    }
                }

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2A2346)),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = group.eventTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Outlined.Event,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.eventTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = getRolePlural(
                            group.roles.size + if (group.participantInfo != null) 1 else 0,
                            tabIndex
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Раскрыть",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.rotate(arrowRotation)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    // Участник
                    if (group.participantInfo != null) {
                        RoleItemDetail(
                            roleName = "Участник",
                            deadlineText = null,
                            showLeaveButton = tabIndex == 1,
                            onLeaveClick = {
                                showLeaveDialog = true
                            }
                        )
                    }

                    // Роли
                    group.roles.forEach { role ->
                        val deadlineFormatted = formatDeadline(role.roleDeadline)
                        RoleItemDetail(
                            roleName = role.eventRoleTitle,
                            deadlineText = deadlineFormatted
                        )
                    }
                }
            }
        }
    }

    // Диалог подтверждения отказа
    if (showLeaveDialog && group.participantInfo != null) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Отказаться от участия?") },
            text = { Text("Вы уверены, что хотите отказаться от роли участника на мероприятии «${group.eventTitle}»?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onLeaveParticipant(group.participantInfo.id)
                        showLeaveDialog = false
                    }
                ) {
                    Text("Отказаться", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun RoleItemDetail(
    roleName: String,
    deadlineText: String?,
    showLeaveButton: Boolean = false,
    onLeaveClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 70.dp, bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Роль: $roleName",
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 16.sp),
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            if (showLeaveButton && onLeaveClick != null) {
                TextButton(
                    onClick = onLeaveClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Отказаться",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        if (deadlineText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Дедлайн: $deadlineText",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}