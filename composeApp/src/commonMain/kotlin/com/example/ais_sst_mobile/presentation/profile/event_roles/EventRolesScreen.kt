package com.example.ais_sst_mobile.presentation.profile.event_roles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ais_sst_mobile.data.network.dto.EventGlobalRoleDto
import com.example.ais_sst_mobile.navigation.EventRolesComponent
import com.example.ais_sst_mobile.navigation.FullScreenRoute
import com.example.ais_sst_mobile.presentation.components.AppBackground
import com.example.ais_sst_mobile.presentation.components.CustomBackButton
import com.example.ais_sst_mobile.presentation.components.CustomSnackbar
import com.example.ais_sst_mobile.presentation.components.CustomTextField
import com.example.ais_sst_mobile.presentation.components.clearFocusOnScroll
import com.example.ais_sst_mobile.presentation.components.clearFocusOnTap
import org.koin.compose.getKoin

@Composable
fun EventRolesScreen(
    component: EventRolesComponent
) {
    val koin = getKoin()
    val screenModel = remember { koin.get<EventRolesScreenModel>() }
    val state by screenModel.state.collectAsState()
    val focusManager = LocalFocusManager.current

    val snackbarHostState = remember { SnackbarHostState() }
    var previousRoleCount by rememberSaveable { mutableStateOf(-1) }

    LaunchedEffect(Unit) {
        screenModel.loadRoles()
    }

    AppBackground {
        Box(modifier = Modifier.fillMaxSize()){
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .clearFocusOnTap(focusManager)
                    .clearFocusOnScroll(focusManager)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CustomBackButton(onClick = component.onGoBack)
                    Text(
                        text = "Роли на мероприятия",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(40.dp))
                }

                when (val currentState = state) {
                    is EventRolesState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    is EventRolesState.Success -> {

                        LaunchedEffect(currentState.roles.size, currentState.searchQuery) {
                            if (currentState.searchQuery.isBlank()) {
                                if (previousRoleCount != -1 && currentState.roles.size > previousRoleCount) {
                                    snackbarHostState.showSnackbar("Роль успешно создана!")
                                }
                                previousRoleCount = currentState.roles.size
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CustomTextField(
                                value = currentState.searchQuery,
                                onValueChange = { screenModel.search(it) },
                                placeholder = "Поиск по названию",
                                modifier = Modifier.weight(1f).height(52.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f),
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), MaterialTheme.shapes.medium)
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable { /* TODO: Filter */ },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Tune, null, tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        if (currentState.roles.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (currentState.searchQuery.isNotBlank()) "Ничего не найдено" else "Список ролей пуст",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            Text(
                                text = "Список ролей (${currentState.roles.size})",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(currentState.roles) { role ->
                                    EventRoleCard(
                                        role = role,
                                        onEditClick = { component.onNavigateToFullScreen(FullScreenRoute.EditRole(role.id)) },
                                        onDeleteClick = { /* TODO: Логика удаления */ }
                                    )
                                }
                            }
                        }
                    }
                    is EventRolesState.Error -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(currentState.message, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .padding(end = 16.dp, bottom = 66.dp)
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary)
                    .clickable { component.onNavigateToFullScreen(FullScreenRoute.CreateRole) }
                    .align(Alignment.BottomEnd),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Создать роль",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
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

@Composable
fun EventRoleCard(
    role: EventGlobalRoleDto,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
        ),
        border = BorderStroke(0.2.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = role.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = role.sectorTitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.secondary
                )

                if (!role.description.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = role.description,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        lineHeight = 20.sp
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { isMenuExpanded = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Опции",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                DropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.background)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Редактировать",
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp)
                            )
                        },
                        onClick = {
                            isMenuExpanded = false
                            onEditClick()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Удалить",
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            isMenuExpanded = false
                            onDeleteClick()
                        }
                    )
                }
            }
        }
    }
}