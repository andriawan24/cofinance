package id.andriawan24.cofinance.andro.ui.presentation.camera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions

@Composable
fun CameraContent(
    modifier: Modifier = Modifier,
    isFlashOn: Boolean,
    onBackPressed: () -> Unit,
    onOpenGalleryClicked: () -> Unit,
    onFlashClicked: () -> Unit,
    cameraContent: @Composable () -> Unit,
    onTakePictureClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        cameraContent()

        Box(modifier = modifier.fillMaxSize()) {
            IconButton(
                modifier = Modifier.padding(all = Dimensions.SIZE_16),
                onClick = onBackPressed,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = null
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = Dimensions.SIZE_40)
                    .padding(bottom = Dimensions.SIZE_24),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onOpenGalleryClicked,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_picture),
                        contentDescription = null
                    )
                }

                Box(
                    modifier = Modifier
                        .size(Dimensions.SIZE_66)
                        .border(
                            width = Dimensions.SIZE_4,
                            color = MaterialTheme.colorScheme.onPrimary,
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .clickable(onClick = onTakePictureClicked)
                        .padding(Dimensions.SIZE_6)
                        .background(
                            color = MaterialTheme.colorScheme.onPrimary,
                            shape = CircleShape
                        )
                )

                IconButton(
                    onClick = onFlashClicked,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isFlashOn) {
                        Icon(
                            painter = painterResource(R.drawable.ic_flash_off),
                            contentDescription = null
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_flash_on),
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun CameraContentPreview() {
    CofinanceTheme {
        CameraContent(
            isFlashOn = false,
            onBackPressed = { },
            onOpenGalleryClicked = { },
            onFlashClicked = { },
            cameraContent = { }
        ) {

        }
    }
}