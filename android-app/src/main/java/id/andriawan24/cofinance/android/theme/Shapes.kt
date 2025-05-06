package id.andriawan24.cofinance.android.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import id.andriawan24.cofinance.android.utils.Dimensions

val shapes = Shapes(
    extraSmall = RoundedCornerShape(Dimensions.SIZE_2),
    small = RoundedCornerShape(Dimensions.SIZE_4),
    medium = RoundedCornerShape(Dimensions.SIZE_8),
    large = RoundedCornerShape(Dimensions.SIZE_16),
    extraLarge = RoundedCornerShape(Dimensions.SIZE_32)
)