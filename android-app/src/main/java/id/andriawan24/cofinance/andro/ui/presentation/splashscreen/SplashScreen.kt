package id.andriawan24.cofinance.andro.ui.presentation.splashscreen

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun SplashScreen(
    onNavigateToMain: () -> Unit,
    onNavigateToLogin: () -> Unit,
    splashViewModel: SplashViewModel = koinViewModel()
) {
    LaunchedEffect(true) {
        splashViewModel.fetchUser().collectLatest {
            val user = it.getOrNull()
            if (user != null) {
                onNavigateToMain()
            } else {
                onNavigateToLogin()
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
            painter = painterResource(R.drawable.img_splash_screen),
            contentDescription = null
        )

        Text(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Dimensions.SIZE_24),
            text = stringResource(R.string.label_fetching_information),
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