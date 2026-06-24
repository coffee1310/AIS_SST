package com.example.ais_sst_mobile.core.filepicker

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.InputStream

actual class FilePicker {

    @Composable
    actual fun rememberFilePicker(
        onFileSelected: (bytes: ByteArray, fileName: String) -> Unit,
        allowedExtensions: List<String>
    ): () -> Unit {
        val context = LocalContext.current

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                val fileName = getFileName(context, it) ?: "unknown_file.${allowedExtensions.firstOrNull() ?: "bin"}"
                val bytes = context.contentResolver.openInputStream(it)?.use { stream: InputStream ->
                    stream.readBytes()
                }
                if (bytes != null) {
                    onFileSelected(bytes, fileName)
                }
            }
        }

        return {
            // Можно улучшить фильтрацию, но для начала используем */*
            launcher.launch("*/*")
        }
    }

    private fun getFileName(context: Context, uri: android.net.Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    return it.getString(nameIndex)
                }
            }
        }
        return null
    }
}