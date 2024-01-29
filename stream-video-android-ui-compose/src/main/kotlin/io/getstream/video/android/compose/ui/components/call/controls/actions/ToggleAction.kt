package io.getstream.video.android.compose.ui.components.call.controls.actions

import androidx.compose.material.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.state.ToggleableState
import io.getstream.video.android.compose.theme.base.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamIconToggleButton
import io.getstream.video.android.compose.ui.components.base.styling.StreamFixedSizeButtonStyle
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.ToggleMicrophone

/**
 * A call action button represents toggling a microphone.
 *
 * @param modifier Optional Modifier for this action button.
 * @param isMicrophoneEnabled Represent is camera enabled.
 * @param enabled Whether or not this action button will handle input events.
 * @param onCallAction A [CallAction] event that will be fired.
 */
@Composable
public fun ToggleAction(
    modifier: Modifier = Modifier,
    isActionActive: Boolean,
    iconOnOff: Pair<ImageVector, ImageVector>,
    enabled: Boolean = true,
    shape: Shape? = null,
    enabledColor: Color? = null,
    disabledColor: Color? = null,
    enabledIconTint: Color? = null,
    disabledIconTint: Color? = null,
    onStyle: StreamFixedSizeButtonStyle? = null,
    offStyle: StreamFixedSizeButtonStyle? = null,
    onAction: () -> Unit,
): Unit = StreamIconToggleButton(
    modifier = modifier,
    enabled = enabled,
    toggleState = rememberUpdatedState(newValue = ToggleableState(!isActionActive)),
    onIcon = iconOnOff.first,
    offIcon = iconOnOff.second,
    onStyle = onStyle ?: VideoTheme.styles.buttonStyles.primaryIconButtonStyle()
        .let {
            it.copyFixed(
                shape = shape ?: it.shape,
                colors = enabledColor?.let { ButtonDefaults.buttonColors(backgroundColor = enabledColor) }
                    ?: it.colors,
                iconStyle = it.iconStyle.copy(
                    default = it.iconStyle.default.copy(
                        color = enabledIconTint ?: it.iconStyle.default.color
                    )
                )
            )
        },
    offStyle = offStyle ?: VideoTheme.styles.buttonStyles.alertIconButtonStyle().let {
        it.copyFixed(
            shape = shape ?: it.shape,
            colors = disabledColor?.let {
                ButtonDefaults.buttonColors(
                    backgroundColor = disabledColor,
                    disabledBackgroundColor = disabledColor
                )
            }
                ?: it.colors,
            iconStyle = it.iconStyle.copy(
                default = it.iconStyle.default.copy(
                    color = disabledIconTint ?: it.iconStyle.default.color
                )
            )
        )
    },
    onClick = {
        onAction()
    }
)