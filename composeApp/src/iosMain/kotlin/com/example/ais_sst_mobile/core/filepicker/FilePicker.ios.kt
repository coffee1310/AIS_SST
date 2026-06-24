package com.example.ais_sst_mobile.core.filepicker

import androidx.compose.runtime.Composable

actual class FilePicker {

    @Composable
    actual fun rememberFilePicker(
        onFileSelected: (bytes: ByteArray, fileName: String) -> Unit,
        allowedExtensions: List<String>
    ): () -> Unit {
        return {
            // TODO: Реализовать через UIDocumentPickerViewController
            println("FilePicker не реализован на iOS")
        }
    }
}