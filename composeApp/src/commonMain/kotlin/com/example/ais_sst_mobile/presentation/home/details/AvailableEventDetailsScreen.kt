package com.example.ais_sst_mobile.presentation.home.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ais_sst_mobile.navigation.AvailableEventDetailsComponent
import com.example.ais_sst_mobile.presentation.components.CustomButton
import com.preat.peekaboo.image.picker.toImageBitmap
import io.ktor.util.decodeBase64Bytes
import org.koin.compose.getKoin

@Composable
fun AvailableEventDetailsScreen(component: AvailableEventDetailsComponent) {
    val koin = getKoin()
    val screenModel = remember { koin.get<AvailableEventDetailsScreenModel>() }
    val state by screenModel.state.collectAsState()

    LaunchedEffect(component.eventId) {
        screenModel.loadEvent(component.eventId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val currentState = state) {
            is AvailableEventDetailsState.Loading -> {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is AvailableEventDetailsState.Error -> {
                Text(
                    text = currentState.message,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp)
                )
            }
            is AvailableEventDetailsState.Success -> {
                val event = currentState.event
                val roles = currentState.roles

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

                Box(modifier = Modifier.fillMaxSize()) {
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

                            Spacer(modifier = Modifier.height(24.dp))

                            // Карточка с организаторами
                            if (event.organizers.isNotEmpty()) {
                                Text(
                                    text = if (event.organizers.size > 1) "Организаторы" else "Организатор",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Card(
                                    shape = MaterialTheme.shapes.large,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                                    ),
                                    border = BorderStroke(0.2.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp)
                                    ) {
                                        event.organizers.forEachIndexed { index, organizer ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
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

                                            if (index < event.organizers.lastIndex) {
                                                Spacer(modifier = Modifier.height(16.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            // Дополнительный отступ (воздух) перед заголовком ролей
                            if (event.organizers.isEmpty()) {
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

                            // Воздух после заголовка ролей
                            Spacer(modifier = Modifier.height(24.dp))

                            // Заявка на организатора
                            if (event.maxOrganizersCount > event.currentOrganizersCount) {
                                ReadOnlyRoleCard(
                                    title = "Организатор",
                                    description = "Помощь в планировании, подготовке и проведении мероприятия. Организаторы получают доступ к управлению событием и контролю участников.",
                                    deadline = null
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Карточка участника (для свободных мероприятий)
                            if (event.isFreeEvent) {
                                ReadOnlyRoleCard(
                                    title = "Участник",
                                    description = "Регистрация проходит без отбора. Вы автоматически станете участником мероприятия после подачи заявки.",
                                    deadline = null
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Остальные роли
                            roles.forEach { role ->
                                val deadlineFormatted = try {
                                    val parts = role.deadline.split("T")
                                    val dateParts = parts[0].split("-")
                                    val time = parts[1].take(5)
                                    "${dateParts[2]}.${dateParts[1]}.${dateParts[0]}, $time"
                                } catch (e: Exception) { role.deadline }

                                ReadOnlyRoleCard(
                                    title = role.globalEventRoleTitle,
                                    description = role.description,
                                    deadline = "Дедлайн: $deadlineFormatted"
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Transparent)
                            .padding(16.dp)
                            .navigationBarsPadding()
                    ) {
                        CustomButton(
                            text = "Зарегистрироваться",
                            onClick = { component.onNavigateToRoleSelection(event.id) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReadOnlyRoleCard(title: String, description: String?, deadline: String?, accentText: String? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)),
        border = BorderStroke(0.2.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 12.sp),
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