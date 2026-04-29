package com.example.ais_sst_mobile.presentation.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.getKoin

@Composable
fun ProfileScreen() {
    val koin = getKoin()
    val screenModel = remember { koin.get<ProfileScreenModel>() }
    val state by screenModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        when (val currentState = state) {
            is ProfileState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                }
            }
            is ProfileState.Error -> {
                Text(currentState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
            }
            is ProfileState.Success -> {
                val user = currentState.profile

                // aватарка
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFEAD1)),
                    contentAlignment = Alignment.Center
                ) {
                    // TODO: AsyncImage(model = user.photoUrl)
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = Color(0xFFD6A87C)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = user.fullName,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = user.role,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                    ),
                    border = BorderStroke(0.2.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatItem(value = user.eventsCount, label = "мероприятий")
                        StatItem(value = user.pointsCount, label = "баллов")
                        StatItem(value = user.rank, label = "место в рейтинге")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileMenuRow(icon = Icons.Outlined.PersonOutline, title = "Мои данные", onClick = { })
                    ProfileMenuRow(icon = Icons.Outlined.StarOutline, title = "История баллов", onClick = { })
                    ProfileMenuRow(icon = Icons.Outlined.WorkOutline, title = "Портфолио", onClick = { })
                    ProfileMenuRow(icon = Icons.Outlined.Settings, title = "Настройки", onClick = { })

                    Spacer(modifier = Modifier.height(8.dp))

                    ProfileMenuRow(icon = Icons.Outlined.HelpOutline, title = "Поддержка", onClick = { })
                    ProfileMenuRow(icon = Icons.Outlined.Info, title = "О приложении", onClick = { })
                }

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = { screenModel.logout() },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(50.dp)
                        .border(width = 0.3.dp, color = MaterialTheme.colorScheme.onPrimary, shape = MaterialTheme.shapes.large),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                        contentDescription = "Выход",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Выход",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, letterSpacing = 2.sp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun ProfileMenuRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,

    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.4f))
                .border(1.dp, MaterialTheme.colorScheme.surfaceTint, MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 18.sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
    }
}