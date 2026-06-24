package com.example.ais_sst_mobile.presentation.profile.portfolio

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.ais_sst_mobile.navigation.PortfolioComponent
import org.koin.compose.getKoin
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(component: PortfolioComponent) {
    val koin = getKoin()
    val screenModel: PortfolioScreenModel = remember { koin.get() }
    val uiState by screenModel.uiState.collectAsState()

    val context = LocalContext.current

    // === Выбор ZIP ===
    val zipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val fileName = getFileName(context, it) ?: "portfolio.zip"
            val bytes = context.contentResolver.openInputStream(it)?.use { it.readBytes() }
            if (bytes != null) {
                screenModel.uploadPortfolio(bytes, fileName)
            }
        }
    }

    // === Выбор PDF ===
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val fileName = getFileName(context, it) ?: "portfolio.pdf"
            val bytes = context.contentResolver.openInputStream(it)?.use { it.readBytes() }
            if (bytes != null) {
                screenModel.uploadPortfolio(bytes, fileName)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Портфолио") },
                navigationIcon = {
                    IconButton(onClick = { component.onGoBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text("Загрузить документы", style = MaterialTheme.typography.titleMedium)

            OutlinedCard(onClick = { zipLauncher.launch("*/*") }, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.UploadFile, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(16.dp))
                    Text("Загрузить архив (.zip)")
                }
            }

            OutlinedCard(onClick = { pdfLauncher.launch("*/*") }, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(16.dp))
                    Text("Загрузить PDF")
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Просмотр и выгрузка", style = MaterialTheme.typography.titleMedium)

            // Просмотреть портфолио
            OutlinedCard(
                onClick = {
                    screenModel.downloadPortfolio { bytes ->
                        saveAndOpenFile(context, bytes, "portfolio.pdf")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(16.dp))
                    Text("Просмотреть портфолио")
                }
            }

            // Выгрузить портфолио в PDF
            OutlinedCard(
                onClick = {
                    screenModel.downloadPortfolio { bytes ->
                        saveAndOpenFile(context, bytes, "portfolio.pdf")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(16.dp))
                    Text("Выгрузить портфолио в PDF")
                }
            }

            when (val state = uiState) {
                is PortfolioUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                is PortfolioUiState.Success -> Text(state.message, color = MaterialTheme.colorScheme.primary)
                is PortfolioUiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
                else -> {}
            }
        }
    }
}

// Получение имени файла
private fun getFileName(context: Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index != -1) return it.getString(index)
        }
    }
    return null
}

// Сохранение и открытие файла (Android)
private fun saveAndOpenFile(context: Context, bytes: ByteArray, fileName: String) {
    try {
        val file = File(context.cacheDir, fileName)
        file.writeBytes(bytes)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, if (fileName.endsWith(".pdf")) "application/pdf" else "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(intent)
    } catch (e: Exception) {
        // Можно показать Snackbar или Toast
        e.printStackTrace()
    }
}