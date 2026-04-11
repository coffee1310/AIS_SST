package com.example.ais_sst_mobile.presentation.components.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class PhoneTransformation(private val isFocused: Boolean = false) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (text.text.isEmpty() && !isFocused) return TransformedText(text, OffsetMapping.Identity)

        val trimmed = if (text.text.length >= 10) text.text.substring(0..9) else text.text
        var out = "+7 ("

        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 2) out += ") "
            if (i == 5 || i == 7) out += " - "
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 4
                if (offset <= 2) return offset + 4
                if (offset <= 5) return offset + 6
                if (offset <= 7) return offset + 9
                return offset + 12
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 4) return 0
                if (offset <= 6) return offset - 4
                if (offset <= 11) return offset - 6
                if (offset <= 16) return offset - 9
                return offset - 12
            }
        }
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

class DateTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 8) text.text.substring(0..7) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 1 || i == 3) out += "."
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 1) return offset
                if (offset <= 3) return offset + 1
                if (offset <= 8) return offset + 2
                return 10
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 2) return offset
                if (offset <= 5) return offset - 1
                if (offset <= 10) return offset - 2
                return 8
            }
        }
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

class PrefixTransformation(private val prefix: String, private val isFocused: Boolean = false) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (text.text.isEmpty() && !isFocused) return TransformedText(text, OffsetMapping.Identity)

        val out = prefix + text.text
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset + prefix.length
            override fun transformedToOriginal(offset: Int): Int =
                if (offset < prefix.length) 0 else offset - prefix.length
        }
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}