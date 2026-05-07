package com.example.ais_sst_mobile.presentation.profile.requests

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ais_sst_mobile.presentation.components.AppBackground
import com.example.ais_sst_mobile.presentation.components.CustomBackButton
import com.example.ais_sst_mobile.presentation.profile.my_data.InfoRow
import com.example.ais_sst_mobile.presentation.profile.my_data.SectionTitle
import com.example.ais_sst_mobile.presentation.profile.my_data.formatDate
import com.preat.peekaboo.image.picker.toImageBitmap
import io.ktor.util.decodeBase64Bytes
import kotlinx.coroutines.launch
import org.koin.compose.getKoin

@Composable
fun RequestDetailsScreen(
    requestId: Int,
    onBackClick: () -> Unit
) {
    val koin = getKoin()
    val screenModel = remember { koin.get<RequestDetailsScreenModel>() }

    val state by screenModel.state.collectAsState()
    val isActionLoading by screenModel.isActionLoading.collectAsState()
    val isActionComplete by screenModel.isActionComplete.collectAsState()

    var rejectingRequestId by remember { mutableStateOf<Int?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(requestId) {
        screenModel.loadRequest(requestId)
    }

    LaunchedEffect(Unit) {
        screenModel.effect.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(isActionComplete) {
        if (isActionComplete) {
            onBackClick()
        }
    }

    AppBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            rejectingRequestId?.let { id ->
                RejectDialog(
                    onDismiss = { rejectingRequestId = null },
                    onConfirm = { reason ->
                        screenModel.rejectRequest(id, reason)
                        rejectingRequestId = null
                    }
                )
            }

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                        .padding(horizontal = 16.dp)
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CustomBackButton(onClick = onBackClick)
                    Text(
                        text = "Данные заявки",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(40.dp))
                }

                when (val currentState = state) {
                    is RequestDetailsState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    is RequestDetailsState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                currentState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    is RequestDetailsState.Success -> {
                        val request = currentState.request

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))

                            val imageBitmap = remember(request.photo) {
                                try {
                                    request.photo?.substringAfter("base64,")?.trim()?.decodeBase64Bytes()?.toImageBitmap()
                                } catch (e: Exception) { null }
                            }

                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (imageBitmap != null) {
                                    Image(bitmap = imageBitmap, contentDescription = "Аватар", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                } else {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.secondary)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            val fullName = listOfNotNull(request.surname, request.name, request.patronymic).joinToString(" ")
                            Text(
                                text = fullName,
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            val groupText = request.groupName ?: "-"
                            val specText = request.specialityName?.take(4)?.uppercase() ?: ""
                            Text(
                                text = "Группа: ${request.courseNumber ?: ""}$specText-$groupText",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 16.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (isActionLoading) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { rejectingRequestId = request.id },
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        shape = MaterialTheme.shapes.small,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            "Отклонить",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 10.sp)
                                        )
                                    }

                                    Button(
                                        onClick = { screenModel.acceptRequest(request.id) },
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        shape = MaterialTheme.shapes.small,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            "Принять",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 10.sp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            SectionTitle("Общая информация")
                            InfoRow("Дата рождения", formatDate(request.dateOfBirth))

                            InfoRow(
                                label = "Номер студенческого билета",
                                value = request.studentEmail?.substringBefore("@") ?: "-",
                                isCopyable = true,
                                onCopied = {
                                    scope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        snackbarHostState.showSnackbar("Студенческий билет скопирован")
                                    }
                                }
                            )

                            //InfoRow("Социальный статус", if (request.socialStatuses.isNullOrEmpty()) "Отсутствует" else request.socialStatuses.joinToString(", "))
                            InfoRow("Пол", request.gender ?: "Не указан")

                            InfoRow(
                                label = "Учебная почта",
                                value = request.studentEmail ?: "-",
                                showDivider = false,
                                isCopyable = true,
                                onCopied = {
                                    scope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        snackbarHostState.showSnackbar("Учебная почта скопирована")
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            SectionTitle("Контактная информация")

                            InfoRow(
                                label = "Телефон",
                                value = request.phoneNumber ?: "-",
                                isCopyable = true,
                                onCopied = {
                                    scope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        snackbarHostState.showSnackbar("Номер телефона скопирован")
                                    }
                                }
                            )

                            InfoRow(
                                label = "Дополнительная почта",
                                value = request.additionalEmail ?: "Не указана",
                                isCopyable = true,
                                onCopied = {
                                    scope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        snackbarHostState.showSnackbar("Дополнительная почта скопирована")
                                    }
                                }
                            )

                            InfoRow(
                                label = "Ссылка на Вконтакте",
                                value = request.vkLink ?: "Не указана",
                                showDivider = false,
                                isCopyable = true,
                                onCopied = {
                                    scope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        snackbarHostState.showSnackbar("Ссылка на Вконтакте скопирована")
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            )
        }
    }
}