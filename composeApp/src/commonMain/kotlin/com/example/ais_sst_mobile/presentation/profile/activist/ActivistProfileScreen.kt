package com.example.ais_sst_mobile.presentation.profile.activist

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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ais_sst_mobile.domain.model.AppRole
import com.example.ais_sst_mobile.presentation.components.AppBackground
import com.example.ais_sst_mobile.presentation.components.CustomBackButton
import com.example.ais_sst_mobile.presentation.components.CustomSnackbar
import com.example.ais_sst_mobile.presentation.components.clearFocusOnScroll
import com.example.ais_sst_mobile.presentation.components.clearFocusOnTap
import com.example.ais_sst_mobile.presentation.profile.my_data.InfoRow
import com.example.ais_sst_mobile.presentation.profile.my_data.SectionTitle
import com.example.ais_sst_mobile.presentation.profile.my_data.formatDate
import com.preat.peekaboo.image.picker.toImageBitmap
import io.ktor.util.decodeBase64Bytes
import kotlinx.coroutines.launch
import org.koin.compose.getKoin

@Composable
fun ActivistProfileScreen(
    userId: Int,
    onBackClick: () -> Unit
) {
    val koin = getKoin()
    val screenModel = remember { koin.get<ActivistProfileScreenModel>() }
    val state by screenModel.state.collectAsState()
    val activeRole by screenModel.activeRole.collectAsState(initial = AppRole.ACTIVIST)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(userId) {
        screenModel.loadActivist(userId)
    }

    AppBackground {
        Box(modifier = Modifier
            .fillMaxSize()
            .clearFocusOnTap(focusManager)
            .clearFocusOnScroll(focusManager)
        ) {
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
                        text = "Данные студента",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(40.dp))
                }

                when (val currentState = state) {
                    is ActivistProfileState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    is ActivistProfileState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = currentState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    is ActivistProfileState.Success -> {
                        val user = currentState.user

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))

                            val imageBitmap = remember(user.photo) {
                                try {
                                    user.photo?.let { rawString ->
                                        var textToDecode = rawString.trim()

                                        if (textToDecode.startsWith("ZGF0Y")) {
                                            val decodedText = textToDecode.decodeBase64Bytes().decodeToString()
                                            if (decodedText.startsWith("data:image")) {
                                                textToDecode = decodedText
                                            }
                                        }

                                        if (textToDecode.contains("base64,")) {
                                            textToDecode = textToDecode.substringAfter("base64,").trim()
                                        }

                                        val bytes = textToDecode.decodeBase64Bytes()

                                        if (bytes.isNotEmpty()) {
                                            bytes.toImageBitmap()
                                        } else null
                                    }
                                } catch (e: Throwable) {
                                    e.printStackTrace()
                                    null
                                }
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

                            val fullName = listOfNotNull(user.surname, user.name, user.patronymic).joinToString(" ")
                            Text(
                                text = fullName,
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            val speciality = user.shortSpecialityTitle ?: user.specialityTitle.take(4).uppercase()
                            Text(
                                text = "Группа: ${user.courseNumber}${speciality}-${user.groupTitle}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 16.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedButton(
                                onClick = { /* TODO: Переход в портфолио */ },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                                    contentColor = MaterialTheme.colorScheme.secondary
                                ),
                                border = BorderStroke(0.2.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text("Портфолио активиста", style = MaterialTheme.typography.displayMedium)
                            }

                            if (activeRole.isSecondBoardMember()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = { /* TODO: Изменить данные */ },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                                        contentColor = MaterialTheme.colorScheme.secondary
                                    ),
                                    border = BorderStroke(0.2.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Text("Изменить данные активиста", style = MaterialTheme.typography.displayMedium)
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            SectionTitle("Общая информация")
                            InfoRow("Дата рождения", formatDate(user.dateOfBirth))

                            InfoRow(
                                label = "Номер студенческого билета",
                                value = user.studentEmail.substringBefore("@"),
                                isCopyable = true,
                                onCopied = {
                                    scope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        snackbarHostState.showSnackbar("Студенческий билет скопирован")
                                    }
                                }
                            )

                            InfoRow("Социальный статус", if (user.socialStatuses.isNullOrEmpty()) "Отсутствует" else user.socialStatuses.joinToString(", "))
                            InfoRow("Пол", user.gender ?: "Не указан")

                            InfoRow(
                                label = "Учебная почта",
                                value = user.studentEmail,
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
                                value = user.phoneNumber,
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
                                value = user.additionalEmail ?: "Не указана",
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
                                value = user.vkLink ?: "Не указана",
                                showDivider = false,
                                isCopyable = true,
                                onCopied = {
                                    scope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        snackbarHostState.showSnackbar("Ссылка на Вконтакте скопирована")
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            SectionTitle("Список секторов")

                            if (user.userSectors.isEmpty()) {
                                InfoRow("Участие", "Не состоит в секторах", showDivider = false)
                            } else {
                                user.userSectors.forEachIndexed { index, sectorName ->
                                    val isCoordinatorHere = sectorName == user.coordinatorSectorTitle
                                    val roleInSector = if (isCoordinatorHere) "Координатор" else "Активист"

                                    SectorPlaceholderRow(
                                        sectorName = sectorName,
                                        role = roleInSector,
                                        showDivider = index < user.userSectors.lastIndex
                                    )
                                }
                            }

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
                    .padding(bottom = 16.dp),
                snackbar = { snackbarData ->
                    CustomSnackbar(snackbarData = snackbarData)
                }
            )
        }
    }
}

@Composable
fun SectorPlaceholderRow(sectorName: String, role: String, showDivider: Boolean = true) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sectorName,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = role,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp),
                textAlign = TextAlign.End
            )
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 1.dp)
        }
    }
}