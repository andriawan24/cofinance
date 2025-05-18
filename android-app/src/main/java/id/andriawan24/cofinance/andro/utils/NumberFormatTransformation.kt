package id.andriawan24.cofinance.andro.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class NumberFormatTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        var formattedNumber = ""
        if (text.text.toLongOrNull() != null) {
            formattedNumber = NumberHelper.formatNumber(text.text.toLong())
        }

        return TransformedText(
            text = AnnotatedString(formattedNumber),
            offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = formattedNumber.length
                override fun transformedToOriginal(offset: Int): Int = text.length
            }
        )
    }
}