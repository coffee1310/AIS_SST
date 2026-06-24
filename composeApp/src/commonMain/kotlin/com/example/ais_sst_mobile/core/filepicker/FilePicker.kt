package com.example.ais_sst_mobile.core.filepicker

import androidx.compose.runtime.Composable

expect class FilePicker() {
    @Composable
    fun rememberFilePicker(
        onFileSelected: (bytes: ByteArray, fileName: String) -> Unit,
        allowedExtensions: List<String> = listOf("pdf", "zip")
    ): () -> Unit
}