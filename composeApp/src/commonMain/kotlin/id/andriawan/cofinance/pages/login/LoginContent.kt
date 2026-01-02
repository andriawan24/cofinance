package id.andriawan.cofinance.pages.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.action_sign_in_google
import cofinance.composeapp.generated.resources.ic_google
import cofinance.composeapp.generated.resources.img_cofinance
import id.andriawan.cofinance.components.OnboardingSwiper
import id.andriawan.cofinance.components.PrimaryButton
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

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
            .padding(
                top = contentPadding.calculateTopPadding(),
                start = contentPadding.calculateStartPadding(LayoutDirection.Ltr),
                end = contentPadding.calculateEndPadding(LayoutDirection.Ltr)
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(Dimensions.SIZE_12))

        Image(
            painter = painterResource(Res.drawable.img_cofinance),
            contentDescription = null
        )

        OnboardingSwiper(modifier = Modifier.weight(1f))

        PrimaryButton(
            modifier = Modifier
                .dropShadow(
                    shape = ButtonDefaults.shape,
                    shadow = Shadow(
                        radius = Dimensions.SIZE_10,
                        spread = Dimensions.SIZE_2,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        offset = DpOffset(x = Dimensions.zero, y = Dimensions.SIZE_4)
                    )
                )
                .fillMaxWidth()
                .padding(horizontal = Dimensions.SIZE_16)
                .padding(bottom = Dimensions.SIZE_24),
            onClick = onContinueClicked,
            enabled = !uiState.isLoading
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_google),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(Dimensions.SIZE_8))
            Text(
                text = stringResource(Res.string.action_sign_in_google),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    CofinanceTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            LoginContent(
                contentPadding = PaddingValues(0.dp),
                uiState = LoginUiState(),
                onContinueClicked = {})
        }
    }
}
