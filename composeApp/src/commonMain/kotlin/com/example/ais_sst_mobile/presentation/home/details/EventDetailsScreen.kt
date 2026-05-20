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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ais_sst_mobile.navigation.CoordinatorEventDetailsComponent
import com.example.ais_sst_mobile.navigation.UpcomingEventDetailsComponent
import com.example.ais_sst_mobile.presentation.components.AppBackground
import com.preat.peekaboo.image.picker.toImageBitmap
import io.ktor.util.decodeBase64Bytes
import org.koin.compose.getKoin

@Composable
fun EventDetailsScreen(component: CoordinatorEventDetailsComponent) {
    val koin = getKoin()
    val screenModel = remember { koin.get<EventDetailsScreenModel>() }
    val state by screenModel.state.collectAsState()

    LaunchedEffect(component.eventId) {
        screenModel.loadEvent(component.eventId)
    }

    AppBackground {
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
                                .height(260.dp)
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

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .background(
                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                                        )
                                    )
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                                        .clickable { component.onGoBack() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = Color.White, modifier = Modifier.size(20.dp))
                                }

                                Text(
                                    text = "Мероприятие",
                                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.width(36.dp))
                            }
                        }

                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
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

                            Spacer(modifier = Modifier.height(32.dp))

                            Text(
                                text = "Роли и задачи",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (event.organizers.isNotEmpty()) {
                                val orgNames = event.organizers.joinToString(separator = "\n") { "• ${it.userSurname} ${it.userName}" }
                                RoleDetailCard(
                                    title = "Организатор",
                                    description = orgNames,
                                    deadline = null
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            roles.forEach { role ->
                                val formattedDesc = role.description.split("\n").joinToString("\n") { if (it.trim().startsWith("•") || it.trim().startsWith("-")) it else "• $it" }

                                val deadlineFormatted = try {
                                    val parts = role.deadline.split("T")
                                    val dateParts = parts[0].split("-")
                                    val time = parts[1].take(5)
                                    "${dateParts[2]}.${dateParts[1]}.${dateParts[0]}, $time"
                                } catch (e: Exception) { role.deadline }

                                RoleDetailCard(
                                    title = role.globalEventRoleTitle,
                                    description = formattedDesc,
                                    deadline = "Дедлайн: $deadlineFormatted"
                                )
                                Spacer(modifier = Modifier.height(12.dp))
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
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                    ReadOnlyToggleRow("Публичное событие", "Доступно всем зарегистрировавшимся", event.isPublic)
                                    ReadOnlyToggleRow("Черновик", "Доступно только членам правления", event.isDraft)
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Text(
                                text = "Организатор",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { /* TODO: Редактировать */ },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = MaterialTheme.shapes.large,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Редактировать\nмероприятие", style = MaterialTheme.typography.titleMedium.copy(fontSize = 12.sp), textAlign = TextAlign.Center)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = { /* TODO: Удалить */ },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = MaterialTheme.shapes.large,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                            ) {
                                Text("Удалить\nмероприятие", style = MaterialTheme.typography.titleMedium.copy(fontSize = 12.sp), textAlign = TextAlign.Center)
                            }

                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoleDetailCard(title: String, description: String, deadline: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(8.dp))
            Text(description, style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), lineHeight = 18.sp)

            if (deadline != null) {
                Spacer(modifier = Modifier.height(12.dp))
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
fun ReadOnlyToggleRow(title: String, subtitle: String, checked: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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