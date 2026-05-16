package com.example.ais_sst_mobile.presentation.sectors.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ais_sst_mobile.navigation.CreateSectorComponent
import com.example.ais_sst_mobile.presentation.components.AppBackground
import com.example.ais_sst_mobile.presentation.components.CustomBackButton
import com.example.ais_sst_mobile.presentation.components.CustomButton
import com.example.ais_sst_mobile.presentation.components.CustomTextField
import com.preat.peekaboo.image.picker.toImageBitmap
import com.preat.peekaboo.image.picker.ResizeOptions
import io.ktor.util.decodeBase64Bytes
import org.koin.compose.getKoin
import com.preat.peekaboo.image.picker.SelectionMode
import com.preat.peekaboo.image.picker.rememberImagePickerLauncher
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ais_sst_mobile.presentation.components.CustomSnackbar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateSectorScreen(component: CreateSectorComponent) {
    val koin = getKoin()
    val screenModel = remember { koin.get<CreateSectorScreenModel>() }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val sectorPhotoBase64 by screenModel.sectorPhotoBase64.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val imagePicker = rememberImagePickerLauncher(
        selectionMode = SelectionMode.Single,
        scope = coroutineScope,
        resizeOptions = ResizeOptions(
            width = 800,
            height = 800,
            compressionQuality = 0.8
        ),
        onResult = { byteArrays ->
            byteArrays.firstOrNull()?.let { bytes ->
                val maxSizeInBytes = 2 * 1024 * 1024
                if (bytes.size > maxSizeInBytes) {
                    screenModel.showError("Фото слишком большое! Выберите другое.")
                } else {
                    screenModel.updateSectorPhoto(bytes)
                }
            }
        }
    )
    val searchQuery by screenModel.searchQuery.collectAsState()
    val filteredActivists by screenModel.filteredActivists.collectAsState()
    val selectedCoordinators by screenModel.selectedCoordinators.collectAsState()

    val focusManager = LocalFocusManager.current
    val glassBackgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)
    val isLoading by screenModel.isLoading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        screenModel.effect.collect { effect ->
            when (effect) {
                is CreateSectorEffect.NavigateBack -> component.onGoBack()
                is CreateSectorEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    AppBackground {
        Box(modifier = Modifier.fillMaxSize()){
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            focusManager.clearFocus()
                        })
                    }
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
                        text = "Создание сектора",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(40.dp))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Основная информация",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )

                    CustomTextField(
                        value = title,
                        onValueChange = { if (it.length <= 128) title = it }, // <-- Ограничение 128
                        placeholder = "* Название сектора",
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = {
                            Text(
                                text = "  Описание сектора",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 8,
                        maxLines = 8,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = glassBackgroundColor,
                            unfocusedContainerColor = glassBackgroundColor,
                            focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = MaterialTheme.colorScheme.primary
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
                                    drawRoundRect(
                                        color = strokeColor,
                                        style = Stroke(
                                            width = 3f,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                                        ),
                                        cornerRadius = CornerRadius(12.dp.toPx())
                                    )
                                }
                            }
                            .clickable {
                                imagePicker.launch()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (sectorPhotoBase64 != null) {
                            val bitmap = remember(sectorPhotoBase64) {
                                try {
                                    sectorPhotoBase64!!.decodeBase64Bytes().toImageBitmap()
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = "Фото сектора",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Добавить",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Добавить",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Координаторы",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )

                    if (selectedCoordinators.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectedCoordinators.forEach { coordinator ->
                                InputChip(
                                    selected = false,
                                    onClick = { screenModel.removeCoordinator(coordinator) },
                                    label = {
                                        Text(
                                            text = coordinator.surname + " " + coordinator.name,
                                            style = MaterialTheme.typography.labelSmall)
                                    },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Удалить",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.surfaceTint
                                        )
                                    },
                                    colors = InputChipDefaults.inputChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.4f),
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceTint)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        CustomTextField(
                            value = searchQuery,
                            onValueChange = { screenModel.updateSearchQuery(it) },
                            placeholder = "* Начните вводить ФИО...",
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                        )

                        AnimatedVisibility(visible = filteredActivists.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                shape = MaterialTheme.shapes.extraLarge,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.4f)),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 240.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    filteredActivists.forEachIndexed { index, activist ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    screenModel.addCoordinator(activist)
                                                    focusManager.clearFocus()
                                                }
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val avatarBitmap = remember(activist.photo) {
                                                try {
                                                    activist.photo?.let { rawString ->
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
                                                    e.printStackTrace()
                                                    null
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.2f)),
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
                                                        Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Text(
                                                text = activist.fullName,
                                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        if (index < filteredActivists.size - 1) {
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                                modifier = Modifier.padding(horizontal = 12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    CustomButton(
                        text = if (isLoading) "Создание..." else "Создать сектор",
                        onClick = {
                            if (!isLoading) {
                                screenModel.createSector(title, description)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) { data ->
                CustomSnackbar(data)
            }
        }

    }
}