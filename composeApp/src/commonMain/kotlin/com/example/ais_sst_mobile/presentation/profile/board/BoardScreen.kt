package com.example.ais_sst_mobile.presentation.profile.board

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ais_sst_mobile.data.network.dto.UserProfileDto
import com.example.ais_sst_mobile.domain.model.AppRole
import com.example.ais_sst_mobile.navigation.BoardComponent
import com.example.ais_sst_mobile.presentation.components.AppBackground
import com.example.ais_sst_mobile.presentation.components.CustomBackButton
import com.preat.peekaboo.image.picker.toImageBitmap
import io.ktor.util.decodeBase64Bytes
import org.koin.compose.getKoin
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.lazy.LazyColumn
import com.example.ais_sst_mobile.presentation.components.CustomSnackbar
import com.example.ais_sst_mobile.presentation.components.CustomTextField

data class KickDialogData(
    val fullName: String,
    val onConfirm: () -> Unit
)

data class AppointDialogData(
    val sectorId: Int,
    val oldUserId: Int? = null
)

@Composable
fun BoardScreen(component: BoardComponent) {
    val koin = getKoin()
    val screenModel = remember { koin.get<BoardScreenModel>() }
    val state by screenModel.state.collectAsState()
    val activeRole by screenModel.activeRole.collectAsState(initial = AppRole.ACTIVIST)

    val canEditChairman = activeRole == AppRole.CURATOR
    val canEditOthers = activeRole == AppRole.CHAIRMAN || activeRole == AppRole.CURATOR

    val searchQuery by screenModel.searchQuery.collectAsState()
    val filteredActivists by screenModel.filteredActivists.collectAsState()

    var appointDialogData by remember { mutableStateOf<AppointDialogData?>(null) }

    val handleCardClick: (Int) -> Unit = { userId ->
        if (activeRole.isThirdBoardMember()) {
            component.onNavigateToActivistProfile(userId)
        }
    }

    if (appointDialogData != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {
                appointDialogData = null
                screenModel.updateSearchQuery("")
            },
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(0.3.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (appointDialogData?.oldUserId != null) "Сменить координатора" else "Назначить координатора",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))

                    CustomTextField(
                        value = searchQuery,
                        onValueChange = { screenModel.updateSearchQuery(it) },
                        placeholder = "Поиск по ФИО...",
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )

                    Spacer(Modifier.height(8.dp))

                    AnimatedVisibility(visible = filteredActivists.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.1f)),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.3f))
                        ) {
                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                itemsIndexed(filteredActivists) { index, activist ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (appointDialogData!!.oldUserId != null) {
                                                    screenModel.changeCoordinator(
                                                        sectorId = appointDialogData!!.sectorId,
                                                        oldUserId = appointDialogData!!.oldUserId!!,
                                                        newUserId = activist.id
                                                    )
                                                } else {
                                                    screenModel.appointCoordinator(
                                                        sectorId = appointDialogData!!.sectorId,
                                                        userId = activist.id
                                                    )
                                                }
                                                appointDialogData = null
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val avatarBitmap = remember(activist.photo) { parseAvatar(activist.photo) }
                                            if (avatarBitmap != null) {
                                                Image(bitmap = avatarBitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                            } else {
                                                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "${activist.surname} ${activist.name} ${activist.patronymic ?: ""}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (index < filteredActivists.size - 1) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 12.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            appointDialogData = null
                            screenModel.updateSearchQuery("")
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Text("Отмена", style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp))
                    }
                }
            }
        }
    }

    var kickDialogData by remember { mutableStateOf<KickDialogData?>(null) }

    if (kickDialogData != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { kickDialogData = null },
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(0.3.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Подтверждение",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Вы уверены, что хотите исключить ${kickDialogData?.fullName}?",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { kickDialogData = null },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Text("Отмена", style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp))
                        }

                        Button(
                            onClick = {
                                kickDialogData?.onConfirm?.invoke()
                                kickDialogData = null
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Text(
                                "Исключить",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp)
                            )
                        }
                    }
                }
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        screenModel.effect.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    AppBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CustomBackButton(onClick = component.onGoBack)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Правление ССт",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(40.dp))

                }

                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Председатель",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        state.chairman?.let { user ->
                            ChairmanCard(
                                user = user,
                                canEdit = canEditChairman,
                                onCardClick = { handleCardClick(user.id) }
                            )
                        } ?: Text("Должность вакантна", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelMedium)

                        Spacer(modifier = Modifier.height(24.dp))

                        val deputiesTitle = if (state.deputies.size == 1) "Заместитель" else "Заместители"
                        SectionHeader(title = deputiesTitle, showAdd = canEditOthers, onAddClick = { /* TODO: Смена зама */ })

                        if (state.deputies.isEmpty()) {
                            Text("Нет заместителей", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp), style = MaterialTheme.typography.bodySmall)
                        } else {
                            state.deputies.forEach { user ->
                                val groupText = "студент группы ${user.courseNumber ?: ""}${user.specialityShortTitle ?: ""}-${user.groupName ?: ""}"
                                val fullName = "${user.surname} ${user.name}"
                                MemberCard(
                                    name = fullName,
                                    group = groupText,
                                    photo = user.photo,
                                    showMenu = canEditOthers,
                                    onEditClick = { /* TODO */ },
                                    onKickClick = {
                                        kickDialogData = KickDialogData(fullName) { /* TODO: screenModel.removeDeputy(user.id) */ }
                                    },
                                    onCardClick = { handleCardClick(user.id) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        val secretariesTitle = if (state.secretaries.size == 1) "Секретарь" else "Секретари"
                        SectionHeader(title = secretariesTitle, showAdd = canEditOthers, onAddClick = { /* TODO: Смена секретаря */ })

                        if (state.secretaries.isEmpty()) {
                            Text("Нет секретарей", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp), style = MaterialTheme.typography.bodySmall)
                        } else {
                            state.secretaries.forEach { user ->
                                val groupText = "студент группы ${user.courseNumber ?: ""}${user.specialityShortTitle ?: ""}-${user.groupName ?: ""}"
                                val fullName = "${user.surname} ${user.name}"
                                MemberCard(
                                    name = fullName,
                                    group = groupText,
                                    photo = user.photo,
                                    showMenu = canEditOthers,
                                    onEditClick = { /* TODO */ },
                                    onKickClick = {
                                        kickDialogData = KickDialogData(fullName) { /* TODO: screenModel.removeSecretary(user.id) */ }
                                    },
                                    onCardClick = { handleCardClick(user.id) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        Text("Координаторы", style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 8.dp))
                        Spacer(modifier = Modifier.height(5.dp))

                        state.sectors.forEach { sector ->
                            if (sector.coordinators.isNotEmpty() || canEditOthers) {
                                SectionHeader(
                                    title = sector.title,
                                    showAdd = canEditOthers,
                                    onAddClick = { appointDialogData = AppointDialogData(sector.id) },
                                    isSubSection = true
                                )

                                if (sector.coordinators.isEmpty()) {
                                    Text("Нет координаторов", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp), style = MaterialTheme.typography.bodySmall)
                                } else {
                                    sector.coordinators.forEach { coord ->
                                        val groupText = "студент группы ${coord.studentCourseNumber ?: ""}${coord.studentSpecialityTitle ?: ""}-${coord.studentGroupTitle ?: ""}"
                                        val fullName = "${coord.studentSurname} ${coord.studentName}"
                                        MemberCard(
                                            name = fullName,
                                            group = groupText,
                                            photo = coord.studentPhoto,
                                            showMenu = canEditOthers,
                                            onEditClick = {
                                                appointDialogData = AppointDialogData(sector.id, coord.studentId)
                                            },
                                            onKickClick = {
                                                kickDialogData = KickDialogData(fullName) {
                                                    screenModel.removeCoordinator(sector.id, coord.studentId)
                                                }
                                            },
                                            onCardClick = { handleCardClick(coord.studentId) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(40.dp))
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
fun SectionHeader(title: String, showAdd: Boolean, onAddClick: () -> Unit, isSubSection: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, top = if(isSubSection) 8.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = if (isSubSection) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
        if (showAdd) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary)
                    .clickable { onAddClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить", tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun ChairmanCard(user: UserProfileDto, canEdit: Boolean, onCardClick: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)),
        border = BorderStroke(0.2.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable { onCardClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${user.surname} ${user.name}",
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 18.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                val groupText = "студент группы ${user.courseNumber ?: ""}${user.specialityShortTitle ?: ""}-${user.groupName ?: ""}"
                Text(
                    text = groupText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (canEdit) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { /* TODO: Смена председателя */ },
                        modifier = Modifier.height(36.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Text("Сменить", style = MaterialTheme.typography.titleMedium.copy(fontSize = 10.sp))
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier.size(76.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                val bitmap = remember(user.photo) { parseAvatar(user.photo) }
                if (bitmap != null) {
                    Image(bitmap = bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
fun MemberCard(name: String, group: String, photo: String?, showMenu: Boolean, onEditClick: () -> Unit, onKickClick: () -> Unit, onCardClick: () -> Unit) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)),
        border = BorderStroke(0.2.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCardClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                val bitmap = remember(photo) { parseAvatar(photo) }
                if (bitmap != null) {
                    Image(bitmap = bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = group,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (showMenu) {
                Box {
                    IconButton(
                        onClick = { isMenuExpanded = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Опции", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }

                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.background)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Сменить", style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp)) },
                            onClick = {
                                isMenuExpanded = false
                                onEditClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Удалить", style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp)) },
                            onClick = {
                                isMenuExpanded = false
                                onKickClick()
                            },
                            leadingIcon = { Icon(Icons.Default.PersonRemove, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }
        }
    }
}

fun parseAvatar(rawString: String?): androidx.compose.ui.graphics.ImageBitmap? {
    if (rawString.isNullOrBlank()) return null
    return try {
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
    } catch (e: Exception) { null }
}