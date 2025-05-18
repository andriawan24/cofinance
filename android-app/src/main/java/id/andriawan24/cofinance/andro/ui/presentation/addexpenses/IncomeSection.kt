package id.andriawan24.cofinance.andro.ui.presentation.addexpenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.text.isDigitsOnly
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.NumberFormatTransformation

@Composable
fun IncomeSection(modifier: Modifier = Modifier) {
    var amountValue by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(true) {
        focusManager.clearFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(all = Dimensions.SIZE_24)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "IDR",
                style = MaterialTheme.typography.displaySmall.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
            )

            Spacer(modifier = Modifier.width(Dimensions.SIZE_8))

            BasicTextField(
                modifier = Modifier.fillMaxWidth(),
                value = amountValue,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                textStyle = MaterialTheme.typography.displaySmall,
                onValueChange = {
                    if (it.length <= Long.MAX_VALUE.toString().length && it.isDigitsOnly()) {
                        amountValue = it
                    }
                },
                decorationBox = { innerTextField ->
                    Box {
                        if (amountValue.isEmpty()) {
                            Text(
                                text = "0",
                                style = MaterialTheme.typography.displaySmall
                            )
                        }

                        innerTextField()
                    }
                },
                visualTransformation = NumberFormatTransformation()
            )
        }
    }
}

@Preview(device = Devices.PIXEL_4_XL, showBackground = true)
@Composable
private fun IncomeSectionPreview() {
    CofinanceTheme {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
            IncomeSection()
        }
    }
}