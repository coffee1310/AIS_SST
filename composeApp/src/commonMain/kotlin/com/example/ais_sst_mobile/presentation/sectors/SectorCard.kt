package com.example.ais_sst_mobile.presentation.sectors

import ais_sst_mobile.composeapp.generated.resources.Res
import ais_sst_mobile.composeapp.generated.resources.default_sector
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ais_sst_mobile.data.network.dto.SectorDto
import com.example.ais_sst_mobile.domain.model.AppRole
import com.preat.peekaboo.image.picker.toImageBitmap
import io.ktor.util.decodeBase64Bytes
import org.jetbrains.compose.resources.painterResource

@Composable
fun SectorCard(
    sector: SectorDto,
    activeRole: AppRole,
    onClick: () -> Unit
) {
    val imageBitmap = remember(sector.photo) {
        try {
            sector.photo?.let { rawString ->
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

                if (bytes.isNotEmpty()) {
                    bytes.toImageBitmap()
                } else null
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
        ),
        border = BorderStroke(0.2.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(end = 16.dp)
            ) {
                AutoSizeTitle(
                    text = sector.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = sector.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                if (activeRole == AppRole.ACTIVIST) {
                    val isCoordinator = sector.isCoordinator

                    val isExited = sector.requestStatus == "Вышедший"

                    val isApproved = !isExited && (sector.isParticipant || sector.requestStatus == "Одобрена")
                    val isPending = !isExited && sector.hasActiveRequest && (sector.requestStatus == "На рассмотрении" || sector.requestStatus == null)

                    if (isCoordinator) {
                        Spacer(modifier = Modifier.height(12.dp))
                        SectorStatusBadge(
                            text = "Координатор",
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (isApproved) {
                        Spacer(modifier = Modifier.height(12.dp))
                        SectorStatusBadge(
                            text = "Участник",
                            color = MaterialTheme.colorScheme.surfaceTint
                        )
                    } else if (isPending) {
                        Spacer(modifier = Modifier.height(12.dp))
                        SectorStatusBadge(
                            text = "Ожидание",
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .width(110.dp)
                    .height(75.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.2f))
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = sector.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(Res.drawable.default_sector),
                        contentDescription = "Заглушка",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun SectorStatusBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .border(1.dp, color, MaterialTheme.shapes.small)
            .background(color.copy(alpha = 0.4f), MaterialTheme.shapes.small)
            .padding(horizontal = 28.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp)
        )
    }
}

@Composable
fun AutoSizeTitle(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    maxLines: Int = 2,
    modifier: Modifier = Modifier
) {
    var scaledTextStyle by remember(text) { mutableStateOf(style) }
    var readyToDraw by remember(text) { mutableStateOf(false) }

    Text(
        text = text,
        style = scaledTextStyle,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.drawWithContent {
            if (readyToDraw) drawContent()
        },
        onTextLayout = { textLayoutResult ->
            var isOverflowing = textLayoutResult.hasVisualOverflow

            if (!isOverflowing && textLayoutResult.lineCount > 1) {
                for (i in 0 until textLayoutResult.lineCount - 1) {
                    val lineEnd = textLayoutResult.getLineEnd(i)
                    if (lineEnd > 0 && lineEnd < text.length) {
                        val charBefore = text[lineEnd - 1]
                        val charAt = text[lineEnd]

                        if (charBefore.isLetterOrDigit() && charAt.isLetterOrDigit()) {
                            isOverflowing = true
                            break
                        }
                    }
                }
            }

            if (isOverflowing && scaledTextStyle.fontSize.value > 12f) {
                scaledTextStyle = scaledTextStyle.copy(fontSize = scaledTextStyle.fontSize * 0.95f)
            } else {
                readyToDraw = true
            }
        }
    )
}