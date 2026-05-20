package com.example.ais_sst_mobile.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ais_sst_mobile.domain.model.Event
import com.preat.peekaboo.image.picker.toImageBitmap
import io.ktor.util.decodeBase64Bytes

@Composable
fun EventCard(
    event: Event,
    isHorizontal: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val imageBitmap = remember(event.photoBase64) {
        try {
            event.photoBase64?.let { rawString ->
                var textToDecode = rawString.trim()

                if (textToDecode.startsWith("ZGF0Y")) {
                    val decodedText = textToDecode.decodeBase64Bytes().decodeToString()
                    if (decodedText.startsWith("data:image")) {
                        textToDecode = decodedText
                    }
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
    val bgColor = when {
        event.isDraft -> Color.Gray.copy(alpha = 0.5f)
        event.isOverdue -> Color.Red.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.onPrimaryContainer.copy(0.15f)
    }

    val borderColor = when {
        event.isOverdue -> Color.Red.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outline
    }
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(0.5.dp, borderColor),
        modifier = modifier
            .height(254.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color(0xFF2A2346))
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = event.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (event.relationBadge != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(
                                color = Color(0xFFA28BFF).copy(0.7f),
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = event.relationBadge,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = event.dateStrCard,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = event.venue,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CustomTabRow(
    tabs: List<Pair<String, ImageVector>>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
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