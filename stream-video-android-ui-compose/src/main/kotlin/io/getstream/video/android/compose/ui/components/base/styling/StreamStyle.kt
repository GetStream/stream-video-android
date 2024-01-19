package io.getstream.video.android.compose.ui.components.base.styling

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * Marker interface for all stream styles.
 */
@Stable
public interface StreamStyle

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
}

