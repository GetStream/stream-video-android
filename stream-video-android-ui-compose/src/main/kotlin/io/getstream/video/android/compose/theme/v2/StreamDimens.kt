package io.getstream.video.android.compose.theme.v2

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import io.getstream.video.android.compose.utils.textSizeResource
import io.getstream.video.android.ui.common.R

public data class StreamDimens(
    val genericMax: Dp,
    val genericXxl: Dp,
    val genericXl: Dp,
    val genericL: Dp,
    val genericM: Dp,
    val genericS: Dp,
    val genericXs: Dp,
    val roundnessXl: Dp,
    val roundnessL: Dp,
    val roundnessM: Dp,
    val roundnessS: Dp,
    val spacingXl: Dp,
    val spacingL: Dp,
    val spacingM: Dp,
    val spacingS: Dp,
    val spacingXs: Dp,
    val componentHeightL: Dp,
    val componentHeightM: Dp,
    val componentHeightS: Dp,
    val componentPaddingTop: Dp,
    val componentPaddingStart: Dp,
    val componentPaddingBottom: Dp,
    val componentPaddingEnd: Dp,
    val componentPaddingFixed: Dp,
    val textSizeXxl: TextUnit,
    val textSizeXl: TextUnit,
    val textSizeL: TextUnit,
    val textSizeM: TextUnit,
    val textSizeS: TextUnit,
    val textSizeXs: TextUnit,
    val lineHeightXxl: TextUnit,
    val lineHeightXl: TextUnit,
    val lineHeightL: TextUnit,
    val lineHeightM: TextUnit,
    val lineHeightS: TextUnit,
    val lineHeightXs: TextUnit
) {
    public companion object {

        @Composable
        public fun defaultDimens(): StreamDimens {
            return StreamDimens(
                genericMax = dimensionResource(R.dimen.stream_video_generic_max),
                genericXxl = dimensionResource(R.dimen.stream_video_generic_xxl),
                genericXl = dimensionResource(R.dimen.stream_video_generic_xl),
                genericL = dimensionResource(R.dimen.stream_video_generic_l),
                genericM = dimensionResource(R.dimen.stream_video_generic_m),
                genericS = dimensionResource(R.dimen.stream_video_generic_s),
                genericXs = dimensionResource(R.dimen.stream_video_generic_xs),
                roundnessXl = dimensionResource(R.dimen.stream_video_roundness_xl),
                roundnessL = dimensionResource(R.dimen.stream_video_roundness_l),
                roundnessM = dimensionResource(R.dimen.stream_video_roundness_m),
                roundnessS = dimensionResource(R.dimen.stream_video_roundness_s),
                spacingXl = dimensionResource(R.dimen.stream_video_spacing_xl),
                spacingL = dimensionResource(R.dimen.stream_video_spacing_l),
                spacingM = dimensionResource(R.dimen.stream_video_spacing_m),
                spacingS = dimensionResource(R.dimen.stream_video_spacing_s),
                spacingXs = dimensionResource(R.dimen.stream_video_spacing_xs),
                componentHeightL = dimensionResource(R.dimen.stream_video_component_height_l),
                componentHeightM = dimensionResource(R.dimen.stream_video_component_height_m),
                componentHeightS = dimensionResource(R.dimen.stream_video_component_height_s),
                componentPaddingTop = dimensionResource(R.dimen.stream_video_component_padding_top),
                componentPaddingStart = dimensionResource(R.dimen.stream_video_component_padding_start),
                componentPaddingBottom = dimensionResource(R.dimen.stream_video_component_padding_bottom),
                componentPaddingEnd = dimensionResource(R.dimen.stream_video_component_padding_end),
                componentPaddingFixed = dimensionResource(R.dimen.stream_video_component_padding_fixed),
                textSizeXxl = textSizeResource(R.dimen.stream_video_text_size_xxl),
                textSizeXl = textSizeResource(R.dimen.stream_video_text_size_xl),
                textSizeL = textSizeResource(R.dimen.stream_video_text_size_l),
                textSizeM = textSizeResource(R.dimen.stream_video_text_size_m),
                textSizeS = textSizeResource(R.dimen.stream_video_text_size_s),
                textSizeXs = textSizeResource(R.dimen.stream_video_text_size_xs),
                lineHeightXxl = textSizeResource(R.dimen.stream_video_line_height_xxl),
                lineHeightXl = textSizeResource(R.dimen.stream_video_line_height_xl),
                lineHeightL = textSizeResource(R.dimen.stream_video_line_height_l),
                lineHeightM = textSizeResource(R.dimen.stream_video_line_height_m),
                lineHeightS = textSizeResource(R.dimen.stream_video_line_height_s),
                lineHeightXs = textSizeResource(R.dimen.stream_video_line_height_xs)
            )
        }
    }
}