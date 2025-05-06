package id.andriawan24.cofinance.android.presentation.onboarding

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices.PIXEL_2_XL
import androidx.compose.ui.tooling.preview.Devices.PIXEL_7_PRO
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.android.R
import id.andriawan24.cofinance.android.theme.CofinanceTheme
import id.andriawan24.cofinance.android.utils.Dimensions
import id.andriawan24.cofinance.android.utils.ext.dropShadow

@Composable
fun OnboardingScreen(modifier: Modifier = Modifier, onContinueClicked: () -> Unit) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.SIZE_32)
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.img_onboarding),
                contentDescription = null
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.SIZE_24)
                .dropShadow(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = Color(0xFF1E1E1E).copy(0.10f),
                    blur = Dimensions.SIZE_4,
                    offsetY = Dimensions.SIZE_2,
                    spread = Dimensions.zero
                )
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.extraLarge
                )
                .padding(Dimensions.SIZE_36),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.title_onboarding),
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Dimensions.SIZE_24))

            Text(
                text = stringResource(R.string.description_onboarding),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f)
                )
            )

            Spacer(modifier = Modifier.height(Dimensions.SIZE_48))

            Button(
                contentPadding = PaddingValues(
                    horizontal = Dimensions.SIZE_48,
                    vertical = Dimensions.SIZE_16
                ),
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceTint,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                onClick = {
                    onContinueClicked()
                }
            ) {
                Text(
                    text = stringResource(R.string.action_continue_now),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Preview(
    device = PIXEL_2_XL,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun OnboardingScreenPreview() {
    CofinanceTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            OnboardingScreen(onContinueClicked = {})
        }
    }
}

@Preview(
    device = PIXEL_7_PRO,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun OnboardingScreenDarkPreview() {
    CofinanceTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            OnboardingScreen(onContinueClicked = { })
        }
    }
}