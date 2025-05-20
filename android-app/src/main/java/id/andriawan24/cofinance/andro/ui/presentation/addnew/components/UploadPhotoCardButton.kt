package id.andriawan24.cofinance.andro.ui.presentation.addnew.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.utils.Dimensions

@Composable
fun UploadPhotoCardButton(onInputPictureClicked: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(all = Dimensions.SIZE_16)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.large
            )
            .border(
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(
                    width = Dimensions.SIZE_1,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            .clip(shape = MaterialTheme.shapes.large)
            .clickable(enabled = true, onClick = onInputPictureClicked)
            .padding(all = Dimensions.SIZE_16),
        horizontalArrangement = Arrangement.spacedBy(space = Dimensions.SIZE_12),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_camera),
            contentDescription = null
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.title_auto_fill_photo),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.Both
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Text(
                text = stringResource(R.string.description_auto_fill_photo),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.Both
                    )
                )
            )
        }

        Box(modifier = Modifier.padding(Dimensions.SIZE_6)) {
            Image(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null
            )
        }
    }
}

@Preview
@Composable
private fun UploadPhotoCardButtonPreview() {
    UploadPhotoCardButton {

    }
}