package id.andriawan.cofinance.pages.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.img_splash_screen
import cofinance.composeapp.generated.resources.message_fetching_information
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions
import id.andriawan.cofinance.utils.ResultState
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SplashScreen(
    onNavigateToMain: () -> Unit,
    onNavigateToLogin: () -> Unit,
    splashViewModel: SplashViewModel = koinViewModel()
) {
    LaunchedEffect(true) {
        splashViewModel.fetchUser().collectLatest {
            when (it) {
                is ResultState.Error -> {
                    println("SplashScreen: Not logged in ${it.exception}")
                    onNavigateToLogin()
                    // TODO: Handle error
                }

                ResultState.Loading -> {
                    /* no-op */
                }

                is ResultState.Success<*> -> {
                    onNavigateToMain()
                }
            }
        }
    }

    Scaffold { contentPadding ->
        SplashScreenContent(
            modifier = Modifier.padding(contentPadding)
        )
    }
}

@Composable
fun SplashScreenContent(modifier: Modifier = Modifier) {
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
                .padding(bottom = Dimensions.SIZE_24),
            text = stringResource(Res.string.message_fetching_information),
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
