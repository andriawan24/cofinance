package id.andriawan24.cofinance.android.ui.presentation.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.android.R
import id.andriawan24.cofinance.android.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.android.utils.Dimensions

@Composable
fun LoginScreen(onSignedIn: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = Dimensions.SIZE_24,
                vertical = Dimensions.SIZE_36
            ),
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            text = stringResource(R.string.title_login),
            style = MaterialTheme.typography.displaySmall
        )

        Spacer(modifier = Modifier.height(height = Dimensions.SIZE_8))

        Text(
            text = stringResource(R.string.description_login),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        )

        Spacer(modifier = Modifier.height(height = Dimensions.SIZE_24))

        Button(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            contentPadding = PaddingValues(vertical = Dimensions.SIZE_12),
            onClick = onSignedIn,
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceTint,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    space = Dimensions.SIZE_12,
                    alignment = Alignment.CenterHorizontally
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.action_sign_in_google),
                    style = MaterialTheme.typography.labelLarge
                )

                Icon(
                    painter = painterResource(R.drawable.ic_google),
                    contentDescription = stringResource(R.string.content_description_google_button)
                )
            }
        }
    }
}

@Preview
@Composable
private fun LoginScreenPreview() {
    CofinanceTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            LoginScreen(onSignedIn = { })
        }
    }
}