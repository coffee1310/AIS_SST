package com.example.ais_sst_mobile.presentation.main

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.example.ais_sst_mobile.navigation.MainComponent
import com.example.ais_sst_mobile.presentation.components.AppBackground
import com.example.ais_sst_mobile.presentation.home.HomeScreen

@Composable
fun MainScreen(component: MainComponent) {
    val childStack by component.stack.subscribeAsState()
    val activeComponent = childStack.active.instance

    val title = when (activeComponent) {
        is MainComponent.Child.Home -> "Главная"
        is MainComponent.Child.Tasks -> "Мои заявки"
        is MainComponent.Child.Calendar -> "Календарь"
        is MainComponent.Child.Sectors -> "Сектора ССТ"
        is MainComponent.Child.Profile -> "Профиль"
    }

    val selectedIndex = when (activeComponent) {
        is MainComponent.Child.Home -> 0
        is MainComponent.Child.Tasks -> 1
        is MainComponent.Child.Calendar -> 2
        is MainComponent.Child.Sectors -> 3
        is MainComponent.Child.Profile -> 4
    }

    AppBackground {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = { SharedTopBar(title = title) },
            bottomBar = { SharedBottomNav(selectedIndex, component::onTabSelected) }
        ) { paddingValues ->
            Children(
                stack = childStack,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) { child ->
                when (val instance = child.instance) {
                    is MainComponent.Child.Home -> HomeScreen()
                    is MainComponent.Child.Tasks -> Box(Modifier.fillMaxSize()) { Text("Задачи", color = Color.White) }
                    is MainComponent.Child.Calendar -> Box(Modifier.fillMaxSize()) { Text("Календарь", color = Color.White) }
                    is MainComponent.Child.Sectors -> Box(Modifier.fillMaxSize()) { Text("Сектора", color = Color.White) }
                    is MainComponent.Child.Profile -> Box(Modifier.fillMaxSize()) { Text("Профиль", color = Color.White) }
                }
            }
        }
    }
}

@Composable
fun SharedTopBar(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .height(60.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        IconButton(
            onClick = { /* TODO: Уведомления */ },
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = "Уведомления",
                tint = MaterialTheme.colorScheme.outline
            )
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
        modifier = Modifier.height(60.dp),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp
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