package io.getstream.video.android.compose.ui.components.base.styling

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

@Composable
internal fun styleState(
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
): StyleState {
    val pressed by interactionSource.collectIsPressedAsState()
    val state = if (enabled) {
        StyleState.ENABLED
    } else if (pressed) {
        StyleState.PRESSED
    } else {
        StyleState.DISABLED
    }
    return state
}