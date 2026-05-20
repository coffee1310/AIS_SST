package com.example.ais_sst_mobile.presentation.home.details

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ais_sst_mobile.navigation.UpcomingEventDetailsComponent
import com.preat.peekaboo.image.picker.toImageBitmap
import io.ktor.util.decodeBase64Bytes
import org.koin.compose.getKoin

@Composable
fun UpcomingEventDetailsScreen(component: UpcomingEventDetailsComponent) {
    val koin = getKoin()
    val screenModel = remember { koin.get<UpcomingEventDetailsScreenModel>() }

    val state by screenModel.state.collectAsState()

    LaunchedEffect(component.eventId) {
        screenModel.loadEvent(component.eventId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val currentState = state) {
            is EventDetailsState.Loading -> {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is EventDetailsState.Error -> {
                Text(
                    text = currentState.message,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp)
                )
            }
            is EventDetailsState.Success -> {
                val event = currentState.event

                val imageBitmap = remember(event.photoBase64) {
                    try {
                        event.photoBase64?.let { rawString ->
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
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        null
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = event.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CalendarToday, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(event.dateStrDetails, style = MaterialTheme.typography.labelSmall.copy(fontSize = 15.sp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.LocationOn, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(event.venue, style = MaterialTheme.typography.labelSmall.copy(fontSize = 15.sp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        Text(
                            text = event.description,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 17.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            textAlign = TextAlign.Left,
                            lineHeight = 21.sp
                        )

                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }
}