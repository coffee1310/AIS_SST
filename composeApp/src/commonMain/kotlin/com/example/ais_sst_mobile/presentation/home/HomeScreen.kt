package com.example.ais_sst_mobile.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ais_sst_mobile.domain.model.AppRole
import com.example.ais_sst_mobile.presentation.components.EventCard
import org.koin.compose.getKoin

@Composable
fun HomeScreen() {
    val koin = getKoin()
    val screenModel = remember { koin.get<HomeScreenModel>() }

    val activeRole by screenModel.activeRole.collectAsState()

    when (activeRole) {
        AppRole.ACTIVIST, AppRole.STUDENT -> ActivistHomeContent(screenModel)
        else -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Главная страница для роли: ${activeRole.uiName} в разработке", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun ActivistHomeContent(screenModel: HomeScreenModel) {
    val selectedTab by screenModel.selectedTab.collectAsState()
    val upcomingEvents by screenModel.upcomingEvents.collectAsState()
    val availableEvents by screenModel.availableEvents.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(5.dp))

        CustomTabRow(selectedTab = selectedTab, onTabSelected = { screenModel.selectTab(it) })

        Spacer(modifier = Modifier.height(5.dp))

        if (selectedTab == 0) {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .clickable { /* TODO */ },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ближайшие мероприятия", style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, "Все", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }

                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(upcomingEvents) { event ->
                            EventCard(event = event, isHorizontal = true, modifier = Modifier.fillParentMaxWidth(0.93f), onClick = { })
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Мероприятия доступные для регистрации", style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).clickable { }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Search, "Поиск", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                items(availableEvents) { event ->
                    EventCard(event = event, isHorizontal = false, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), onClick = { })
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Контент вкладки $selectedTab", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun CustomTabRow(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf(
        Pair("Мероприятия", Icons.Outlined.Event),
        Pair("Быстрые задачи", Icons.Outlined.FlashOn),
        Pair("Внутренние проекты", Icons.Outlined.Folder)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(53.dp)
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.onBackground)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, (title, icon) ->
            val isSelected = selectedTab == index

            val weight by animateFloatAsState(targetValue = if (isSelected) 4f else 1f, label = "tabWidth")

            Box(
                modifier = Modifier
                    .weight(weight)
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.medium)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onTabSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else Color(0xFFCBCBCB),
                        modifier = Modifier.size(20.dp)
                    )

                    AnimatedVisibility(visible = isSelected) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

