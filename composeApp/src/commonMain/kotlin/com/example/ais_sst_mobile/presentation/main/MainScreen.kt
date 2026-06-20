package com.example.ais_sst_mobile.presentation.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.example.ais_sst_mobile.navigation.MainComponent
import com.example.ais_sst_mobile.navigation.SectorsComponent
import com.example.ais_sst_mobile.presentation.components.CustomBackButton
import com.example.ais_sst_mobile.presentation.home.HomeScreen
import com.example.ais_sst_mobile.presentation.profile.ProfileScreen
import com.example.ais_sst_mobile.presentation.sectors.SectorsTab
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.ais_sst_mobile.core.prefs.SessionManager
import com.example.ais_sst_mobile.domain.model.AppRole
import com.example.ais_sst_mobile.domain.repository.UserRepository
import com.example.ais_sst_mobile.navigation.HomeComponent
import com.example.ais_sst_mobile.presentation.calendar.CalendarScreen
import com.example.ais_sst_mobile.presentation.components.AppBackground
import org.koin.compose.getKoin

@Composable
fun MainScreen(component: MainComponent) {
    val koin = getKoin()
    val sessionManager = remember { koin.get<SessionManager>() }
    val userRepository = remember { koin.get<UserRepository>() }

    val activeRole by sessionManager.activeRoleFlow.collectAsState(initial = AppRole.ACTIVIST)
    var coordinatorSectorTitle by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(activeRole) {
        if (activeRole == AppRole.SECTOR_COORDINATOR) {
            userRepository.getUserProfile().onSuccess { user ->
                coordinatorSectorTitle = user.coordinatorSectorTitle ?: user.coordinatorSector
            }
        }
    }

    val childStack by component.stack.subscribeAsState()
    val activeComponent = childStack.active.instance

    var title = ""
    var showBackButton = false
    var onBackClick: (() -> Unit)? = null

    if (activeComponent is MainComponent.Child.Sectors) {
        val sectorsStack by activeComponent.component.stack.subscribeAsState()
        val sectorsActive = sectorsStack.active.instance

        when (sectorsActive) {
            is SectorsComponent.Child.Details -> {
                title = "Данные сектора"
                showBackButton = true
                onBackClick = { sectorsActive.component.onGoBack() }
            }
            is SectorsComponent.Child.Participants -> {
                title = sectorsActive.component.sectorTitle
                showBackButton = true
                onBackClick = { sectorsActive.component.onGoBack() }
            }
            else -> {
                title = if (activeRole == AppRole.SECTOR_COORDINATOR && coordinatorSectorTitle != null) {
                    coordinatorSectorTitle!!
                } else {
                    "Сектора ССТ"
                }
            }
        }
    } else if (activeComponent is MainComponent.Child.Home) {
        val homeStack by activeComponent.component.stack.subscribeAsState()
        val homeActive = homeStack.active.instance

        when (homeActive) {
            is HomeComponent.Child.UpcomingEvents -> {
                title = "Ближайшие мероприятия"
                showBackButton = true
                onBackClick = { homeActive.component.onGoBack() }
            }
            is HomeComponent.Child.UpcomingEventDetails -> {
                title = "Мероприятие"
                showBackButton = true
                onBackClick = { homeActive.component.onGoBack() }
            }
            is HomeComponent.Child.AvailableEventDetails -> {
                title = "Регистрация на мероприятие"
                showBackButton = true
                onBackClick = { homeActive.component.onGoBack() }
            }
            // Добавили заголовок и кнопку назад для Координаторского окна
            is HomeComponent.Child.CoordinatorEventDetails -> {
                title = "Мероприятие"
                showBackButton = true
                onBackClick = { homeActive.component.onGoBack() }
            }
            else -> {
                title = "Главная"
            }
        }
    } else {
        title = when (activeComponent) {
            is MainComponent.Child.Home -> "Главная"
            is MainComponent.Child.Tasks -> "Мои заявки"
            is MainComponent.Child.Calendar -> "Календарь"
            is MainComponent.Child.Profile -> "Профиль"
            else -> ""
        }
    }

    val selectedIndex = when (activeComponent) {
        is MainComponent.Child.Home -> 0
        is MainComponent.Child.Tasks -> 1
        is MainComponent.Child.Calendar -> 2
        is MainComponent.Child.Sectors -> 3
        is MainComponent.Child.Profile -> 4
        else -> 4
    }
    val isKeyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    AppBackground {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = { SharedTopBar(title = title, showBackButton = showBackButton, onBackClick = onBackClick)},
            bottomBar = {
                if (!isKeyboardOpen) {
                    SharedBottomNav(selectedIndex, component::onTabSelected)
                }
            }
        ) { paddingValues ->
            Children(
                stack = childStack,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) { child ->
                when (val instance = child.instance) {
                    is MainComponent.Child.Home -> HomeScreen(instance.component)
                    is MainComponent.Child.Tasks -> Box(Modifier.fillMaxSize()) { Text("Задачи", color = Color.White) }
                    is MainComponent.Child.Calendar -> CalendarScreen(instance.component)
                    is MainComponent.Child.Sectors -> SectorsTab(instance.component)
                    is MainComponent.Child.Profile -> ProfileScreen(instance.component)
                }
            }
        }
    }
}

@Composable
fun SharedTopBar(
    title: String,
    showBackButton: Boolean = false,
    onBackClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(40.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (showBackButton && onBackClick != null) {
                CustomBackButton(onClick = onBackClick)
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier.width(40.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            IconButton(
                onClick = { /* TODO: Уведомления */ },
                modifier = Modifier.offset(x = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Уведомления",
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun SharedBottomNav(selectedIndex: Int, onTabSelected: (Int) -> Unit) {
    val navItems = listOf(
        Triple(2, "Календарь", Icons.Outlined.CalendarToday),
        Triple(3, "Сектора", Icons.Outlined.Groups),
        Triple(0, "Главная", Icons.Outlined.Home),
        Triple(1, "Заявки", Icons.Outlined.Article),
        Triple(4, "Профиль", Icons.Outlined.PersonOutline)
    )

    NavigationBar(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            .height(50.dp),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        navItems.forEach { (navIndex, title, icon) ->
            val isSelected = selectedIndex == navIndex
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(navIndex) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isSelected) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}