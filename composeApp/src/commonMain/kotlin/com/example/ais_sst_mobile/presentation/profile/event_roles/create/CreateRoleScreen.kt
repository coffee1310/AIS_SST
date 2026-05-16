package com.example.ais_sst_mobile.presentation.profile.event_roles.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ais_sst_mobile.data.network.dto.SectorDto
import com.example.ais_sst_mobile.navigation.CreateRoleComponent
import com.example.ais_sst_mobile.presentation.components.AppBackground
import com.example.ais_sst_mobile.presentation.components.CustomBackButton
import com.example.ais_sst_mobile.presentation.components.CustomButton
import com.example.ais_sst_mobile.presentation.components.CustomSnackbar
import com.example.ais_sst_mobile.presentation.components.CustomTextField
import org.koin.compose.getKoin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoleScreen(component: CreateRoleComponent) {
    val koin = getKoin()
    val screenModel = remember { koin.get<CreateRoleScreenModel>() }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedSector by remember { mutableStateOf<SectorDto?>(null) }
    var isSectorMenuExpanded by remember { mutableStateOf(false) }

    val isLoading by screenModel.isLoading.collectAsState()
    val sectors by screenModel.sectors.collectAsState()

    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val glassBackgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)

    LaunchedEffect(Unit) {
        screenModel.effect.collect { effect ->
            when (effect) {
                is CreateRoleEffect.NavigateBack -> component.onGoBack()
                is CreateRoleEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    AppBackground {
        Box(modifier = Modifier.fillMaxSize()) {
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
                        text = "Создание роли",
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
                        // Ограничение ввода до 32 символов
                        onValueChange = { if (it.length <= 32) title = it },
                        placeholder = "* Название роли",
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ExposedDropdownMenuBox(
                        expanded = isSectorMenuExpanded,
                        onExpandedChange = {
                            focusManager.clearFocus()
                            isSectorMenuExpanded = it
                        }
                    ) {
                        CustomTextField(
                            value = selectedSector?.title ?: "",
                            onValueChange = {},
                            placeholder = "* Выберите сектор",
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSectorMenuExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = isSectorMenuExpanded,
                            onDismissRequest = { isSectorMenuExpanded = false },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.background)
                                .heightIn(max = 240.dp)
                        ) {
                            sectors.forEach { sector ->
                                DropdownMenuItem(
                                    text = { Text(sector.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        selectedSector = sector
                                        isSectorMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = {
                            Text(
                                text = "  Описание роли",
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

                    Spacer(modifier = Modifier.height(40.dp))

                    CustomButton(
                        text = if (isLoading) "Создание..." else "Создать роль",
                        onClick = {
                            if (!isLoading) {
                                screenModel.createRole(title, description, selectedSector?.id)
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
            ) { data -> CustomSnackbar(data) }
        }
    }
}