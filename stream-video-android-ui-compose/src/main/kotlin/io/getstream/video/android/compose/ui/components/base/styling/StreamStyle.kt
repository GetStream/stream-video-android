package io.getstream.video.android.compose.ui.components.base.styling

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState

/**
 * Marker interface for all stream styles.
 */
@Stable
public interface StreamStyle

/**
 * Possible interaction states.
 */
public enum class StyleState {
    ENABLED, DISABLED, PRESSED
}

/**
 * Possible sizes for the stile.
 */
public enum class StyleSize {
    XS, S, M, L, XL, XXL
}

/**
 * Stream style container, containing multiple styles
 */
@Stable
public interface StreamStateStyle<T : StreamStyle> {

    /** Default style for the component. */
    public val default: T

    /** Pressed style for the component */
    public val pressed: T

    /** Disabled style for the component */
    public val disabled: T

    /**
     * Get the style  based on [StyleState].
     */
    @Composable
    public fun of(state: StyleState): State<T> = rememberUpdatedState(
        when (state) {
            StyleState.ENABLED -> default
            StyleState.DISABLED -> disabled
            StyleState.PRESSED -> pressed
        }
    )
}

