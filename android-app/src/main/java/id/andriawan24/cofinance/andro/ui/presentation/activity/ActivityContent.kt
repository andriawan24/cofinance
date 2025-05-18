package id.andriawan24.cofinance.andro.ui.presentation.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.components.VerticalSpacing
import id.andriawan24.cofinance.andro.ui.presentation.activity.components.BalanceCard
import id.andriawan24.cofinance.andro.ui.presentation.activity.components.DateSwitcher
import id.andriawan24.cofinance.andro.ui.presentation.activity.components.ExpenseByMonth
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions

@Composable
fun ActivityContent(onBookmarkClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimensions.SIZE_16)
    ) {
        ActivityTitle(onBookmarkClicked = onBookmarkClicked)
        VerticalSpacing(size = Dimensions.SIZE_24)
        DateSwitcher(label = "May 2025")

        // EmptyActivity(modifier = Modifier.weight(1f))
        BalanceCard(
            modifier = Modifier.padding(vertical = Dimensions.SIZE_24),
            balance = 6_000_000,
            income = 10_000_000,
            expense = 4_000_000
        )

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_24)
        ) {
            ExpenseByMonth()
            ExpenseByMonth()
        }
    }
}

@Composable
private fun ActivityTitle(modifier: Modifier = Modifier, onBookmarkClicked: () -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.label_activity),
            style = MaterialTheme.typography.displaySmall
        )

        IconButton(
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary
            ),
            onClick = onBookmarkClicked
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_bookmark),
                contentDescription = null
            )
        }
    }
}

@Preview
@Composable
private fun ActivityContentPreview() {
    CofinanceTheme {
        Surface {
            ActivityContent(onBookmarkClicked = { })
        }
    }
}