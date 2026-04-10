package com.example.ais_sst_mobile.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null
) {
    val glassBackgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        },
        isError = isError,
        supportingText = errorMessage?.let {
            { Text(text = it, style = MaterialTheme.typography.labelMedium) }
        },
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        trailingIcon = trailingIcon,
        suffix = suffix,
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = glassBackgroundColor,
            unfocusedContainerColor = glassBackgroundColor,
            focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = MaterialTheme.colorScheme.primary,
            errorBorderColor = MaterialTheme.colorScheme.error,
            errorCursorColor = MaterialTheme.colorScheme.error,
            errorSupportingTextColor = MaterialTheme.colorScheme.error
            ),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
        singleLine = true
    )
}