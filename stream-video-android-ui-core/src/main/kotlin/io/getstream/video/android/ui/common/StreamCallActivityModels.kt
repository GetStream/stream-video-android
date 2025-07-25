package io.getstream.video.android.ui.common

import io.getstream.video.android.core.Call
import java.lang.Exception

public data class StreamCallActivityError(
    val call: Call,
    val exception: Exception
)
