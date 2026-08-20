package id.andriawan.cofinance.pages.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import cofinance.composeapp.generated.resources.action_retry
import cofinance.composeapp.generated.resources.error_migration_failed
import cofinance.composeapp.generated.resources.img_splash_screen
import cofinance.composeapp.generated.resources.message_encrypting_existing_data
import cofinance.composeapp.generated.resources.message_encrypting_existing_data_preparing
import cofinance.composeapp.generated.resources.message_fetching_information
import id.andriawan.cofinance.components.PrimaryButton
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The launch screen, which is where the app decides what the user is allowed to see.
 *
 * It renders nothing but progress and, when migration stops part-way, a retry. Every branch that
 * needs the user — setting up encryption, unlocking, restoring from a phrase — is a destination
 * this screen navigates to rather than a mode it enters, so the launch sequence stays one linear
 * function and no screen has to know what came before it.
 *
 * Migration gets a message with a count rather than an indeterminate spinner because it is the one
 * step here that can take minutes, and Decision 6 makes it blocking. A user staring at an unmoving
 * splash concludes the app is broken and force-quits it, which is precisely the interruption
 * migration is written to survive but should not have to.
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
            uiState = uiState,
            onEvent = splashViewModel::onEvent
        )
    }
}

@Composable
fun SplashScreenContent(
    modifier: Modifier = Modifier,
    uiState: SplashUiState = SplashUiState(),
    onEvent: (SplashUiEvent) -> Unit = {}
) {
    when (val phase = uiState.phase) {
        LaunchPhase.MigrationFailed -> MessageContent(
            modifier = modifier,
            message = stringResource(Res.string.error_migration_failed),
            actionLabel = stringResource(Res.string.action_retry),
            onAction = { onEvent(SplashUiEvent.RetryMigration) }
        )

        is LaunchPhase.Migrating -> BrandedContent(
            modifier = modifier,
            message = if (phase.total == 0) {
                stringResource(Res.string.message_encrypting_existing_data_preparing)
            } else {
                stringResource(
                    Res.string.message_encrypting_existing_data,
                    phase.finished,
                    phase.total
                )
            }
        )

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

@Composable
private fun MessageContent(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimensions.SIZE_24),
        verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_16, Alignment.CenterVertically)
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onAction
        ) {
            Text(text = actionLabel, style = MaterialTheme.typography.labelMedium)
        }
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
