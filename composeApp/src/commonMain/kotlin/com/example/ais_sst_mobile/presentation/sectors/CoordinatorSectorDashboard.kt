package com.example.ais_sst_mobile.presentation.sectors

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ais_sst_mobile.navigation.SectorListComponent
import com.example.ais_sst_mobile.presentation.components.CustomTextField
import com.example.ais_sst_mobile.presentation.components.clearFocusOnScroll
import com.example.ais_sst_mobile.presentation.components.clearFocusOnTap

@Composable
fun CoordinatorSectorDashboard(
    screenModel: SectorsScreenModel,
    component: SectorListComponent
) {    val participants by screenModel.participantsState.collectAsState()
    val isParticipantsLoading by screenModel.isParticipantsLoading.collectAsState()
    val selectedTab by screenModel.selectedDashboardTab.collectAsState()
    val searchQuery by screenModel.searchQuery.collectAsState()
    val requests by screenModel.requestsState.collectAsState()
    val isRequestsLoading by screenModel.isRequestsLoading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val savedTab = rememberSaveable { mutableStateOf(0) }
    val participantsScrollState = rememberLazyListState()
    val requestsScrollState = rememberLazyListState()

    LaunchedEffect(Unit) {
        screenModel.selectDashboardTab(savedTab.value)
        screenModel.loadCoordinatorData()
        screenModel.loadRequests()

        screenModel.effect.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val filteredParticipants = participants.filter {
        it.status == "Активный"
    }.filter {
        it.studentSurname.contains(searchQuery, ignoreCase = true) ||
                it.studentName.contains(searchQuery, ignoreCase = true)
    }.sortedWith(
        Comparator { p1, p2 ->
            if (p1.isCoordinator && !p2.isCoordinator) return@Comparator -1
            if (!p1.isCoordinator && p2.isCoordinator) return@Comparator 1

            val surnameCompare = p1.studentSurname.compareTo(p2.studentSurname, ignoreCase = true)
            if (surnameCompare != 0) {
                surnameCompare
            } else {
                p1.studentName.compareTo(p2.studentName, ignoreCase = true)
            }
        }
    )

    val filteredRequests = requests.filter {
        (it.surname ?: "").contains(searchQuery, ignoreCase = true) ||
                (it.name ?: "").contains(searchQuery, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clearFocusOnTap(focusManager)
                .clearFocusOnScroll(focusManager)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(5.dp))

            CoordinatorTabs(
                selectedTab = selectedTab,
                onTabSelected = {
                    savedTab.value = it
                    screenModel.selectDashboardTab(it)
                },
                requestsCount = requests.size
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomTextField(
                    value = searchQuery,
                    onValueChange = { screenModel.updateSearchQuery(it) },
                    placeholder = "Поиск по ФИО",
                    modifier = Modifier.weight(1f).height(52.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f),
                            shape = MaterialTheme.shapes.medium
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = MaterialTheme.shapes.medium
                        )
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { /* TODO: Фильтры */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Фильтр",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (selectedTab == 0) {
                if (isParticipantsLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    }
                } else {
                    Text(
                        text = "Список участников (${filteredParticipants.size})",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    LazyColumn(
                        state = participantsScrollState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(filteredParticipants) { participant ->
                            ParticipantCard(
                                participant = participant,
                                onOptionsClick = { /* TODO: Меню действий */ },
                                modifier = Modifier.clickable {
                                    component.onNavigateToActivistProfile(participant.studentId)
                                }
                            )
                        }
                    }
                }
            } else {
                if (isRequestsLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    }
                } else if (filteredRequests.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Нет новых заявок", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                } else {
                    Text(
                        text = "Заявки на вступление (${filteredRequests.size})",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    LazyColumn(
                        state = requestsScrollState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(filteredRequests) { request ->
                            SectorRequestCard(
                                request = request,
                                onAccept = { screenModel.acceptRequest(request.id) },
                                onReject = { screenModel.rejectRequest(request.id) },
                                modifier = Modifier.clickable {
                                    component.onNavigateToActivistProfile(request.user_id)
                                }
                            )
                        }
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
                com.example.ais_sst_mobile.presentation.components.CustomSnackbar(snackbarData = snackbarData)
            }
        )
    }

}

@Composable
fun CoordinatorTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    requestsCount: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(53.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.onBackground)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(6.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onTabSelected(0) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Участники",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(6.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onTabSelected(1) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Заявки",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                )

                if (requestsCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-3).dp, y = 2.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = requestsCount.toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                        )
                    }
                }
            }
        }
    }
}