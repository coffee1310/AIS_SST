package com.example.ais_sst_mobile.presentation.sectors.edit

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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ais_sst_mobile.navigation.EditSectorComponent
import com.example.ais_sst_mobile.presentation.components.AppBackground
import com.example.ais_sst_mobile.presentation.components.CustomBackButton
import com.example.ais_sst_mobile.presentation.components.CustomButton
import com.example.ais_sst_mobile.presentation.components.CustomSnackbar
import com.example.ais_sst_mobile.presentation.components.CustomTextField
import com.preat.peekaboo.image.picker.ResizeOptions
import com.preat.peekaboo.image.picker.SelectionMode
import com.preat.peekaboo.image.picker.rememberImagePickerLauncher
import com.preat.peekaboo.image.picker.toImageBitmap
import io.ktor.util.decodeBase64Bytes
import org.koin.compose.getKoin
import com.example.ais_sst_mobile.core.prefs.SessionManager
import com.example.ais_sst_mobile.domain.model.AppRole

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditSectorScreen(component: EditSectorComponent) {
    val koin = getKoin()
    val screenModel = remember { koin.get<EditSectorScreenModel>() }
    val sessionManager = remember { koin.get<SessionManager>() }
    val activeRole by sessionManager.activeRoleFlow.collectAsState()
    val canEditCoordinators = activeRole == AppRole.CHAIRMAN || activeRole == AppRole.CURATOR
    LaunchedEffect(component.sectorId) {
        screenModel.loadSector(component.sectorId)
    }

    val title by screenModel.title.collectAsState()
    val description by screenModel.description.collectAsState()
    val sectorPhotoBase64 by screenModel.sectorPhotoBase64.collectAsState()

    val searchQuery by screenModel.searchQuery.collectAsState()
    val filteredActivists by screenModel.filteredActivists.collectAsState()
    val selectedCoordinators by screenModel.selectedCoordinators.collectAsState()

    val isScreenLoading by screenModel.isScreenLoading.collectAsState()
    val isLoading by screenModel.isLoading.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val glassBackgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)
    val snackbarHostState = remember { SnackbarHostState() }

    val imagePicker = rememberImagePickerLauncher(
        selectionMode = SelectionMode.Single,
        scope = coroutineScope,
        resizeOptions = ResizeOptions(width = 800, height = 800, compressionQuality = 0.8),
        onResult = { byteArrays ->
            byteArrays.firstOrNull()?.let { bytes ->
                if (bytes.size > 2 * 1024 * 1024) screenModel.showError("Фото слишком большое! Выберите другое.")
                else screenModel.updateSectorPhoto(bytes)
            }
        }
    )

    LaunchedEffect(Unit) {
        screenModel.effect.collect { effect ->
            when (effect) {
                is EditSectorEffect.NavigateBack -> component.onGoBack()
                is EditSectorEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    AppBackground {
        Box(modifier = Modifier.fillMaxSize()){
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
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
                        text = "Редактирование сектора",
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

                        Text("Основная информация", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))

                        CustomTextField(
                            value = title,
                            onValueChange = { if (it.length <= 128) screenModel.updateTitle(it) }, // <-- Ограничение 128
                            placeholder = "* Название сектора",
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = description,
                            onValueChange = { screenModel.updateDescription(it) },
                            placeholder = { Text("  Описание сектора", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 8,
                            maxLines = 8,
                            textStyle = MaterialTheme.typography.bodyLarge,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = glassBackgroundColor, unfocusedContainerColor = glassBackgroundColor,
                                focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = MaterialTheme.shapes.medium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val strokeColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(glassBackgroundColor)
                                .drawBehind {
                                    if (sectorPhotoBase64 == null) {
                                        drawRoundRect(color = strokeColor, style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)), cornerRadius = CornerRadius(12.dp.toPx()))
                                    }
                                }
                                .clickable { imagePicker.launch() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (sectorPhotoBase64 != null) {
                                val bitmap = remember(sectorPhotoBase64) {
                                    try {
                                        var textToDecode = sectorPhotoBase64!!.trim()
                                        if (textToDecode.startsWith("ZGF0Y")) textToDecode = textToDecode.decodeBase64Bytes().decodeToString()
                                        if (textToDecode.contains("base64,")) textToDecode = textToDecode.substringAfter("base64,").trim()
                                        textToDecode.decodeBase64Bytes().toImageBitmap()
                                    } catch (e: Exception) { null }
                                }
                                if (bitmap != null) {
                                    Image(bitmap = bitmap, contentDescription = "Фото сектора", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
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

                        Text("Координаторы", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))

                        if (selectedCoordinators.isNotEmpty()) {
                            FlowRow(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                selectedCoordinators.forEach { coordinator ->
                                    InputChip(
                                        selected = false,
                                        onClick = { if (canEditCoordinators) screenModel.removeCoordinator(coordinator) },
                                        label = { Text(text = "${coordinator.surname} ${coordinator.name}", style = MaterialTheme.typography.labelSmall) },
                                        trailingIcon = {
                                            if (canEditCoordinators) {
                                                Icon(Icons.Default.Close, "Удалить", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.surfaceTint)
                                            }
                                        },
                                        colors = InputChipDefaults.inputChipColors(containerColor = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.4f), labelColor = MaterialTheme.colorScheme.onSurface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceTint)
                                    )
                                }
                            }
                        }

                        if (canEditCoordinators) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                CustomTextField(
                                    value = searchQuery,
                                    onValueChange = { screenModel.updateSearchQuery(it) },
                                    placeholder = "* Начните вводить ФИО...",
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                                )

                                AnimatedVisibility(visible = filteredActivists.isNotEmpty()) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        shape = MaterialTheme.shapes.extraLarge,
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.4f)),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.3f))
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                                            filteredActivists.forEachIndexed { index, activist ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().clickable { screenModel.addCoordinator(activist); focusManager.clearFocus() }.padding(horizontal = 16.dp, vertical = 12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                                        Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(activist.fullName, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp), color = MaterialTheme.colorScheme.onSurface)
                                                }
                                                if (index < filteredActivists.size - 1) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 12.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        CustomButton(
                            text = if (isLoading) "Сохранение..." else "Сохранить изменения",
                            onClick = { if (!isLoading) screenModel.saveChanges() },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 16.dp)
            ) { data -> CustomSnackbar(data) }
        }
    }
}