package id.andriawan24.cofinance.andro.ui.presentation.login

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.components.HorizontalSpacing
import id.andriawan24.cofinance.andro.ui.components.PrimaryButton
import id.andriawan24.cofinance.andro.ui.components.VerticalSpacing
import id.andriawan24.cofinance.andro.ui.presentation.login.components.OnboardingSwiper
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.ext.dropShadow

@Composable
fun LoginContent(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    uiState: LoginUiState,
    onContinueClicked: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
            )
            .padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VerticalSpacing(Dimensions.SIZE_12)
        Image(
            painter = painterResource(R.drawable.img_cofinance),
            contentDescription = null
        )
        OnboardingSwiper(modifier = Modifier.weight(1f))
        PrimaryButton(
            modifier = Modifier
                .dropShadow(
                    shape = ButtonDefaults.shape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    blur = Dimensions.SIZE_10,
                    offsetY = Dimensions.SIZE_4
                )
                .fillMaxWidth()
                .padding(horizontal = Dimensions.SIZE_16)
                .padding(bottom = Dimensions.SIZE_24),
            onClick = onContinueClicked,
            verticalPadding = Dimensions.SIZE_12,
            enabled = !uiState.isLoading
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_google),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
            HorizontalSpacing(Dimensions.SIZE_8)
            Text(
                text = stringResource(R.string.action_sign_in_google),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL)
@Composable
private fun LoginScreenPreview() {
    CofinanceTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            LoginContent(
                contentPadding = PaddingValues.Zero,
                uiState = LoginUiState(),
                onContinueClicked = {})
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = Devices.PIXEL_4_XL,
    uiMode = UI_MODE_NIGHT_YES
)
@Composable
private fun LoginScreenDarkPreview() {
    CofinanceTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            LoginContent(
                onContinueClicked = {},
                contentPadding = PaddingValues.Zero,
                uiState = LoginUiState()
            )
        }
    }
}
