package io.getstream.video.android.compose.ui

import io.getstream.android.sample.audiocall.sample.compose.StreamCallActivityComposeDelegate
import io.getstream.video.android.ui.common.StreamActivityUiDelegate
import io.getstream.video.android.ui.common.StreamCallActivity

/**
 * Default [StreamCallActivity] for use with compose.
 * Extend this activity if you are using compose and want default call behavior.
 */
public open class ComposeStreamCallActivity : StreamCallActivity() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : StreamCallActivity> uiDelegate(): StreamActivityUiDelegate<T> {
        return StreamCallActivityComposeDelegate() as StreamActivityUiDelegate<T>
    }
}