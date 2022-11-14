package io.getstream.video.android.compose.state.ui.call

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import io.getstream.video.android.call.state.CallAction

/**
 * Represents a single Call Control item in the UI.
 *
 * @param actionBackgroundTint The tint of the background for the action button.
 * @param icon The icon within the button.
 * @param iconTint The tint of the action icon.
 * @param callAction The action that this item represents.
 * @param description The content description of the item.
 */
public data class CallControlAction(
    val actionBackgroundTint: Color,
    val icon: Painter,
    val iconTint: Color,
    val callAction: CallAction,
    val description: String? = null
)