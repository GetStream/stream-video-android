package io.getstream.video.android.activity

import android.content.Context
import io.getstream.video.android.StreamVideo

public interface StreamCallActivity {

    public fun getStreamCalls(context: Context): StreamVideo

}