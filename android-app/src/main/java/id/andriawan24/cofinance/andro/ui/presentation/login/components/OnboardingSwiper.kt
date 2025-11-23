package id.andriawan24.cofinance.andro.ui.presentation.login.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.TextSizes

@Composable
fun OnboardingSwiper(modifier: Modifier = Modifier) {
//    val pagerState = rememberPagerState(pageCount = { 3 })

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    bottom = Dimensions.SIZE_48,
                    top = Dimensions.SIZE_24
                ),
            verticalArrangement = Arrangement.spacedBy(
                Dimensions.SIZE_24,
                Alignment.CenterVertically
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.img_onboarding),
                contentDescription = null
            )

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.SIZE_60),
                text = buildAnnotatedString {
                    pushStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = TextSizes.SIZE_24
                        )
                    )
                    append(stringResource(R.string.label_track_your_money))
                    pushStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = TextSizes.SIZE_24
                        )
                    )
                    append(stringResource(R.string.app_name))
                },
                style = MaterialTheme.typography.displaySmall.copy(
                    lineHeight = TextSizes.SIZE_32
                ),
                textAlign = TextAlign.Center
            )
        }
//        HorizontalPager(state = pagerState) {
//
//        }
//
//        DotsIndicator(
//            modifier = Modifier.padding(bottom = Dimensions.SIZE_24),
//            dotCount = pagerState.pageCount,
//            type = ShiftIndicatorType(
//                dotsGraphic = DotGraphic(
//                    size = Dimensions.SIZE_4,
//                    color = MaterialTheme.colorScheme.primary
//                ),
//                shiftSizeFactor = 8f
//            ),
//            pagerState = pagerState,
//            dotSpacing = Dimensions.SIZE_2
//        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingContentPreview() {
    CofinanceTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            OnboardingSwiper()
        }
    }
}