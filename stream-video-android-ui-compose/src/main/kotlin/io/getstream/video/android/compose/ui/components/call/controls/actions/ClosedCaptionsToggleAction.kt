package io.getstream.video.android.compose.ui.components.call.controls.actions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ClosedCaptionDisabled
import androidx.compose.material.icons.filled.ClosedCaptionOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.ClosedCaptionsAction
import io.getstream.video.android.core.call.state.Settings

/**
 * A call action button represents displaying the call captions.
 *
 * @param modifier Optional Modifier for this action button.
 * @param enabled Whether or not this action button will handle input events.
 * @param onCallAction A [CallAction] event that will be fired.
 */
@Composable
public fun ClosedCaptionsToggleAction(
    modifier: Modifier = Modifier,
    active: Boolean,
    enabled: Boolean = true,
    shape: Shape? = null,
    enabledColor: Color? = null,
    disabledColor: Color? = null,
    onCallAction: (ClosedCaptionsAction) -> Unit,
): Unit = ToggleAction(
    isActionActive = active,
    iconOnOff =
    Pair(Icons.Default.ClosedCaption, Icons.Default.ClosedCaptionOff),
    modifier = modifier,
    enabled = enabled, shape = shape,
    enabledColor = enabledColor, disabledColor = disabledColor,
    offStyle = VideoTheme.styles.buttonStyles.primaryIconButtonStyle(),
    onStyle = VideoTheme.styles.buttonStyles.secondaryIconButtonStyle(),
) {
    onCallAction(ClosedCaptionsAction(!active))
}

@Preview
@Composable
public fun ClosedCaptionsToggleActionPreview() {
    VideoTheme {
        Column {
            Row {
                ClosedCaptionsToggleAction(active = false) {
                }

                ClosedCaptionsToggleAction(active = true) {
                }
            }
        }
    }
}