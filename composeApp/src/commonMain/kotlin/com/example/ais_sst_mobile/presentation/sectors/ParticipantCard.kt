package com.example.ais_sst_mobile.presentation.sectors

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ais_sst_mobile.data.network.dto.ParticipantDto
import com.preat.peekaboo.image.picker.toImageBitmap
import io.ktor.util.decodeBase64Bytes

@Composable
fun ParticipantCard(
    participant: ParticipantDto,
    onOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imageBitmap = remember(participant.studentPhoto) {
        try {
            participant.studentPhoto?.let { rawString ->
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
            }
        } catch (e: Throwable) { null }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
        ),
        border = BorderStroke(0.2.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.secondary)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                val fullName = "${participant.studentSurname} ${participant.studentName}"
                Text(
                    text = fullName,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 16.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                val groupText = "Группа: ${participant.studentCourseNumber}${participant.studentSpecialityTitle}-${participant.studentGroupTitle}"
                Text(
                    text = groupText,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onOptionsClick, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.MoreVert, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    }
}