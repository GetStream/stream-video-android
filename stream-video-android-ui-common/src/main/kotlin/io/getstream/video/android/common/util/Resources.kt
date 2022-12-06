package io.getstream.video.android.common.util

import android.content.Context
import android.os.Build
import android.util.TypedValue

public fun Context.getFloatResource(resId: Int): Float {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        resources.getFloat(resId)
    } else {
        val outValue = TypedValue()
        resources.getValue(resId, outValue, true)
        outValue.float
    }
}