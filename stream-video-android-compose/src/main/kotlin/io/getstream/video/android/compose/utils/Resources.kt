package io.getstream.video.android.compose.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import io.getstream.video.android.common.util.getFloatResource

@Composable
internal fun textSizeResource(id: Int): TextUnit {
    return dimensionResource(id = id).value.sp
}

@Composable
internal fun floatResource(id: Int): Float {
    return LocalContext.current.getFloatResource(id)
}