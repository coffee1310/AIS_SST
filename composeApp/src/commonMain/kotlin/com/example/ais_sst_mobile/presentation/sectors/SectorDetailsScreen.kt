package com.example.ais_sst_mobile.presentation.sectors

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonOutline
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
import com.example.ais_sst_mobile.domain.model.AppRole
import com.example.ais_sst_mobile.navigation.SectorDetailsComponent
import com.example.ais_sst_mobile.presentation.components.CustomButton
import com.example.ais_sst_mobile.presentation.components.CustomSnackbar
import com.preat.peekaboo.image.picker.toImageBitmap
import io.ktor.util.decodeBase64Bytes
import org.koin.compose.getKoin

@Composable
fun SectorDetailsScreen(component: SectorDetailsComponent) {
    val koin = getKoin()
    val screenModel = remember { koin.get<SectorDetailsScreenModel>() }
    val state by screenModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        screenModel.effect.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(component.sectorId) {
        screenModel.loadSector(component.sectorId)
    }

    when (val currentState = state) {
        is SectorDetailsState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            }
        }

        is SectorDetailsState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    currentState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        is SectorDetailsState.Success -> {
            val sector = currentState.sector
            val activeRole = currentState.activeRole

            val imageBitmap = remember(sector.photo) {
                try {
                    sector.photo?.let { rawString ->
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

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .background(MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.2f))
                    ) {
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = sector.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = sector.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = sector.description,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 15.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                            textAlign = TextAlign.Justify,
                            lineHeight = 21.sp
                        )

                        Spacer(modifier = Modifier.height(40.dp))

                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                    alpha = 0.15f
                                )
                            ),
                            border = BorderStroke(0.2.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.PersonOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Координатор",
                                        style = MaterialTheme.typography.displayMedium.copy(
                                            fontSize = 18.sp
                                        ),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val coordImage = remember(sector.coordinatorPhoto) {
                                        try {
                                            sector.coordinatorPhoto?.let { rawString ->
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
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(
                                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (coordImage != null) {
                                            Image(
                                                bitmap = coordImage,
                                                contentDescription = null,
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
                                        if (sector.coordinatorFullName != null) {
                                            val fullName = listOfNotNull(sector.coordinatorSurname, sector.coordinatorName).joinToString(" ")
                                            Text(
                                                text = fullName,
                                                style = MaterialTheme.typography.displayMedium.copy(fontSize = 16.sp),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))

                                            //val speciality = sector.coordinatorShortSpecialityTitle ?: sector.coordinatorSpecialityTitle
//                                            val groupText =
//                                                if (sector.coordinatorCourseNumber != null && speciality != null && sector.coordinatorGroupTitle != null) {
//                                                    "студент группы ${sector.coordinatorCourseNumber}${speciality}-${sector.coordinatorGroupTitle}"
//                                                } else if (sector.coordinatorGroupTitle != null) {
//                                                    "студент группы ${sector.coordinatorGroupTitle}"
//                                                } else {
//                                                    "координатор сектора"
//                                                }
                                            val groupText =
                                                if (sector.coordinatorCourseNumber != null && sector.coordinatorSpecialityTitle != null && sector.coordinatorGroupTitle != null) {
                                                    "студент группы ${sector.coordinatorCourseNumber}${sector.coordinatorSpecialityTitle}-${sector.coordinatorGroupTitle}"
                                                } else if (sector.coordinatorGroupTitle != null) {
                                                    "студент группы ${sector.coordinatorGroupTitle}"
                                                } else {
                                                    "координатор сектора"
                                                }

                                            Text(
                                                text = groupText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                maxLines = 2
                                            )
                                        } else {
                                            Text(
                                                text = "Координатор не указан",
                                                style = MaterialTheme.typography.displayMedium.copy(fontSize = 16.sp),
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(45.dp))

                        if (activeRole == AppRole.ACTIVIST && !sector.isCoordinator) {
                            val isExited = sector.requestStatus == "Вышедший"

                            val isApproved =
                                !isExited && (sector.isParticipant || sector.requestStatus == "Одобрена")
                            val isPending =
                                !isExited && sector.hasActiveRequest && (sector.requestStatus == "На рассмотрении" || sector.requestStatus == null)

                            if (isApproved) {
                                CustomButton(
                                    text = "Выйти из сектора",
                                    onClick = { screenModel.leaveSector(sector.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            } else if (isPending) {
                                val statusText = sector.requestStatus ?: "На рассмотрении"
                                CustomButton(
                                    text = "Заявка: $statusText",
                                    onClick = { },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = false,
                                    colors = ButtonDefaults.buttonColors(
                                        disabledContainerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                            alpha = 0.3f
                                        ),
                                        disabledContentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            } else {
                                CustomButton(
                                    text = "Подать заявку",
                                    onClick = { screenModel.joinSector(sector.id) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        val isCuratorOrChairman = activeRole == AppRole.CURATOR || activeRole == AppRole.CHAIRMAN
                        val isDeputy = activeRole == AppRole.DEPUTY_CHAIRMAN
                        val isSecretary = activeRole == AppRole.SECRETARY

                        if (isCuratorOrChairman || isDeputy || isSecretary) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                if (isCuratorOrChairman || isDeputy) {
                                    CustomButton(
                                        text = "Просмотр участников сектора",
                                        onClick = { component.onNavigateToParticipants(sector.id) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    CustomButton(
                                        text = "Редактировать информацию",
                                        onClick = { /* TODO */ },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                if (isCuratorOrChairman || isDeputy || isSecretary) {
                                    CustomButton(
                                        text = "Выгрузка отчета",
                                        onClick = { /* TODO */ },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                if (isCuratorOrChairman) {
                                    CustomButton(
                                        text = "Удалить сектор",
                                        onClick = { /* TODO */ },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.Transparent,
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }
}