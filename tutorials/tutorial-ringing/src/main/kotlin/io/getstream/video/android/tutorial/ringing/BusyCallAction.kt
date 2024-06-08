package io.getstream.video.android.tutorial.ringing

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.controls.actions.GenericAction
import io.getstream.video.android.core.call.state.CustomAction

@Composable
public fun BusyCallAction(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onCallAction: (BusyCall) -> Unit,
    icon: ImageVector? = null,
    bgColor: Color? = null,
    iconTint: Color? = null,
): Unit = GenericAction(
    modifier = modifier,
    enabled = enabled,
    onAction = { onCallAction(BusyCall) },
    icon = icon ?: Icons.Default.Close,
    color = bgColor ?: VideoTheme.colors.alertWarning,
    iconTint = iconTint ?: VideoTheme.colors.basePrimary,
)

public data object BusyCall : CustomAction(tag = "busy")