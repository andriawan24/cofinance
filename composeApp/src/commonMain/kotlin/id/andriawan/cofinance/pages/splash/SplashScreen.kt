package id.andriawan.cofinance.pages.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.img_splash_screen
import cofinance.composeapp.generated.resources.message_fetching_information
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The launch screen, which is where the app decides what the user is allowed to see.
 *
 * It renders nothing but progress. Every branch that needs the user — setting up encryption,
 * unlocking, restoring from a phrase — is a destination this screen navigates to rather than a mode
 * it enters, so the launch sequence stays one linear function and no screen has to know what came
 * before it.
 */
@Composable
fun SplashScreen(
    onNavigate: (LaunchRoute) -> Unit,
    splashViewModel: SplashViewModel = koinViewModel()
) {
    val uiState by splashViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { splashViewModel.start() }

    LaunchedEffect(uiState.route) {
        uiState.route?.let(onNavigate)
    }

    Scaffold { contentPadding ->
        SplashScreenContent(
            modifier = Modifier.padding(contentPadding),
            uiState = uiState
        )
    }
}

@Composable
fun SplashScreenContent(
    modifier: Modifier = Modifier,
    uiState: SplashUiState = SplashUiState()
) {
    when (uiState.phase) {
        LaunchPhase.Preparing -> BrandedContent(
            modifier = modifier,
            message = stringResource(Res.string.message_fetching_information)
        )
    }
}

@Composable
private fun BrandedContent(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(Res.drawable.img_splash_screen),
            contentDescription = null
        )

        Text(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = Dimensions.SIZE_24, vertical = Dimensions.SIZE_24),
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    CofinanceTheme {
        Surface {
            SplashScreenContent()
        }
    }
}
