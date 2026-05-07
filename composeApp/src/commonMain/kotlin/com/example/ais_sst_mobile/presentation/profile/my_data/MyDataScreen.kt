package com.example.ais_sst_mobile.presentation.profile.my_data

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.preat.peekaboo.image.picker.toImageBitmap
import io.ktor.util.decodeBase64Bytes
import org.koin.compose.getKoin
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material.icons.filled.ContentCopy
import kotlinx.coroutines.launch

@Composable
fun MyDataScreen(onBackClick: () -> Unit) {
    val koin = getKoin()
    val screenModel = remember { koin.get<MyDataScreenModel>() }
    val state by screenModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    AppBackground {
        Box(modifier = Modifier.fillMaxSize()) {
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
                        text = "Мои данные",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(40.dp))
                }

                when (val currentState = state) {
                    is MyDataState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    is MyDataState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                currentState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    is MyDataState.Success -> {
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

                            Text(
                                text = "Группа: ${user.courseNumber}${user.specialityTitle.take(4).uppercase()}-${user.groupTitle}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 16.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedButton(
                                onClick = { /* TODO */ },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                border = BorderStroke(0.2.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text("Изменить контактную информацию",
                                    style = MaterialTheme.typography.displayMedium,
                                    color = MaterialTheme.colorScheme.secondary)
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
                            InfoRow("Телефон", user.phoneNumber)
                            InfoRow("Дополнительная почта", user.additionalEmail ?: "Не указана")
                            InfoRow("Ссылка на Вконтакте", user.vkLink ?: "Не указана", showDivider = false)

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

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
    )
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    showDivider: Boolean = true,
    isCopyable: Boolean = false,
    onCopied: (() -> Unit)? = null
) {
    val clipboardManager = LocalClipboardManager.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (isCopyable) {
                    Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable {
                            clipboardManager.setText(AnnotatedString(value))
                            onCopied?.invoke()
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                } else Modifier
            ) {
                Text(
                    text = value,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp),
                    textAlign = TextAlign.End
                )

                if (isCopyable) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Скопировать",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 1.dp)
        }
    }
}

fun formatDate(dateStr: String?): String {
    if (dateStr == null) return "Не указана"
    val parts = dateStr.split("-")
    if (parts.size != 3) return dateStr

    val year = parts[0].toIntOrNull() ?: return dateStr
    val month = parts[1].toIntOrNull() ?: return dateStr
    val day = parts[2].toIntOrNull() ?: return dateStr

    val months = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
    val monthIndex = month - 1
    if (monthIndex !in 0..11) return dateStr

    val formattedDate = "$day ${months[monthIndex]} $year"

    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    var age = today.year - year
    if (today.monthNumber < month || (today.monthNumber == month && today.dayOfMonth < day)) {
        age--
    }

    val ageSuffix = when {
        age % 100 in 11..14 -> "лет"
        age % 10 == 1 -> "год"
        age % 10 in 2..4 -> "года"
        else -> "лет"
    }

    return "$formattedDate ($age $ageSuffix)"
}