package id.andriawan24.cofinance.andro.ui.presentation.splashscreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.models.CofinanceAppState
import id.andriawan24.cofinance.andro.ui.navigation.Destinations
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun SplashScreen(
    appState: CofinanceAppState,
    splashViewModel: SplashViewModel = koinViewModel()
) {
    LaunchedEffect(true) {
        splashViewModel.fetchUser().collectLatest {
            val user = it.getOrNull()
            if (user != null) {
                appState.navController.navigate(Destinations.Main)
            } else {
                appState.navController.navigate(Destinations.Login)
            }
        }
    }

    SplashScreenContent()
}

@Composable
fun SplashScreenContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.img_splash_screen),
            contentDescription = null
        )

        Text(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Dimensions.SIZE_24),
            text = "Fetching information...",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview(modifier: Modifier = Modifier) {
    CofinanceTheme {
        Surface {
            SplashScreenContent()
        }
    }
}