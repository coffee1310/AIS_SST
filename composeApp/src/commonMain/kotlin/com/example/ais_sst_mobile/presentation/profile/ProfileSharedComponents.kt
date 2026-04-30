package com.example.ais_sst_mobile.presentation.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ais_sst_mobile.domain.model.AppRole

@Composable
fun ProfileHeader(
    user: UserUiModel,
    activeRole: AppRole,
    realRole: AppRole,
    onRoleSelected: (AppRole) -> Unit
) {
    Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(Color(0xFFFFEAD1)), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.Person, null, modifier = Modifier.size(60.dp), tint = Color(0xFFD6A87C))
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text(user.fullName, style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp), color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
    Spacer(modifier = Modifier.height(6.dp))

    var isRoleMenuExpanded by remember { mutableStateOf(false) }

    Box(contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable(enabled = realRole.isBoardMember()) { isRoleMenuExpanded = true }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = activeRole.uiName,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
            )
            if (realRole.isBoardMember()) {
                Icon(
                    imageVector = if (isRoleMenuExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Сменить роль",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp).size(20.dp)
                )
            }
        }

        if (realRole.isBoardMember()) {
            DropdownMenu(
                expanded = isRoleMenuExpanded,
                onDismissRequest = { isRoleMenuExpanded = false },
                containerColor = MaterialTheme.colorScheme.background
            ) {
                DropdownMenuItem(
                    text = { Text(realRole.uiName, style = MaterialTheme.typography.labelMedium) },
                    onClick = { onRoleSelected(realRole); isRoleMenuExpanded = false }
                )
                DropdownMenuItem(
                    text = { Text(AppRole.ACTIVIST.uiName, style = MaterialTheme.typography.labelMedium) },
                    onClick = { onRoleSelected(AppRole.ACTIVIST); isRoleMenuExpanded = false }
                )
            }
        }
    }
}

@Composable
fun ProfileStatsCard(user: UserUiModel) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)),
        border = BorderStroke(0.2.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            StatItem(value = user.eventsCount, label = "мероприятий")
            StatItem(value = user.pointsCount, label = "баллов")
            StatItem(value = user.rank, label = "место в рейтинге")
        }
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp), color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
fun ProfileMenuRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.4f)).border(1.dp, MaterialTheme.colorScheme.surfaceTint, MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.labelSmall.copy(fontSize = 18.sp), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun LogoutButton(onLogoutClick: () -> Unit) {
    Button(
        onClick = onLogoutClick,
        modifier = Modifier.fillMaxWidth(0.9f).height(50.dp).border(width = 0.3.dp, color = MaterialTheme.colorScheme.onPrimary, shape = MaterialTheme.shapes.large),
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Icon(Icons.AutoMirrored.Outlined.ExitToApp, "Выход", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text("Выход", style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, letterSpacing = 2.sp), color = MaterialTheme.colorScheme.onPrimary, textAlign = TextAlign.Center)
    }
}