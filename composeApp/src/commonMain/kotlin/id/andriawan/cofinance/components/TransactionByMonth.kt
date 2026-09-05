package id.andriawan.cofinance.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.content_description_delete_transaction
import cofinance.composeapp.generated.resources.ic_trash
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import id.andriawan.cofinance.domain.model.response.Transaction
import id.andriawan.cofinance.domain.model.response.TransactionByDate
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions
import id.andriawan.cofinance.utils.NumberHelper
import id.andriawan.cofinance.utils.extensions.formatDayMonthYear
import id.andriawan.cofinance.utils.extensions.formatToString
import id.andriawan.cofinance.utils.extensions.toDate

@Composable
fun TransactionByMonth(
    modifier: Modifier = Modifier,
    item: TransactionByDate,
    onTransactionClicked: (Transaction) -> Unit = {},
    onTransactionDeleteRequested: ((Transaction) -> Unit)? = null
) {
    Column(
        modifier = modifier
            .dropShadow(
                shape = RectangleShape,
                shadow = Shadow(
                    radius = Dimensions.SIZE_10,
                    spread = Dimensions.SIZE_2,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    offset = DpOffset(x = Dimensions.zero, y = -Dimensions.SIZE_4)
                )
            )
            .background(
                color = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large
            )
            .padding(all = Dimensions.SIZE_16)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.dateLabel.toDate().formatToString(format = formatDayMonthYear),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Text(
                text = NumberHelper.formatRupiah(item.totalAmount),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = Dimensions.SIZE_16),
            thickness = Dimensions.SIZE_1,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        )

        Column(verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_24)) {
            item.transactions.forEach { transaction ->
                if (onTransactionDeleteRequested == null) {
                    TransactionItem(
                        transaction = transaction,
                        onTransactionClicked = onTransactionClicked
                    )
                } else {
                    SwipeableTransactionItem(
                        transaction = transaction,
                        onTransactionClicked = onTransactionClicked,
                        onDeleteRequested = onTransactionDeleteRequested
                    )
                }
            }
        }
    }
}

/**
 * One row that reveals a delete affordance when dragged aside.
 *
 * Both drag directions are live, and they are expressed as start-to-end / end-to-start rather than
 * as pixels, so the gesture follows the layout direction the platform reports instead of a hardcoded
 * side. Swiping the row to the left is the delete gesture users arrive with on either device; the
 * mirrored drag is accepted rather than fought, and the affordance moves to whichever edge the row
 * is being pulled away from.
 *
 * The swipe asks rather than deletes: a completed drag reports the intent and the box is reset, so
 * the row is back in place behind the confirmation dialog whichever way it is answered. The list is
 * driven by a database flow, so a confirmed deletion removes the row on its own and there is no
 * dismissed state to reconcile.
 */
@Composable
private fun SwipeableTransactionItem(
    transaction: Transaction,
    onTransactionClicked: (Transaction) -> Unit,
    onDeleteRequested: (Transaction) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            onDeleteRequested(transaction)
            dismissState.reset()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            // Settled is only observed between a reset and the next drag, when nothing is revealed.
            val alignment = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                else -> Alignment.CenterEnd
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = Dimensions.SIZE_16),
                contentAlignment = alignment
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_trash),
                    contentDescription = stringResource(Res.string.content_description_delete_transaction),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    ) {
        TransactionItem(
            modifier = Modifier.background(MaterialTheme.colorScheme.onPrimary),
            transaction = transaction,
            onTransactionClicked = onTransactionClicked
        )
    }
}

@Preview
@Composable
private fun TransactionByMonthPreview() {
    CofinanceTheme {
        TransactionByMonth(
            modifier = Modifier.fillMaxWidth(),
            item = TransactionByDate(dateLabel = "2025-05-15T00:00:00Z")
        )
    }
}
