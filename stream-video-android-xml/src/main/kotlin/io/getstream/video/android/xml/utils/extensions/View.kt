/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/GetStream/stream-video-android/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getstream.video.android.xml.utils.extensions

import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import io.getstream.log.StreamLog

/**
 * Sets the ripple effect to background for clickable views.
 */
internal fun View.setBackgroundRipple() {
    val outValue = TypedValue()
    context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
    setBackgroundResource(outValue.resourceId)
}

internal val View.isLandscape: Boolean
    get() = context.isLandscape

fun ViewGroup.orientationChanged(isLandscape: Boolean) {
    if (this is OrientationChangeListener) this.onOrientationChanged(isLandscape)
    children.forEach {
        if (it is ViewGroup) it.orientationChanged(isLandscape)
        if (it !is ViewGroup && it is OrientationChangeListener) it.onOrientationChanged(isLandscape)
    }
}

interface OrientationChangeListener {
    fun onOrientationChanged(isLandscape: Boolean) {}
}

internal fun View.updateLayoutParams(updateParams: ConstraintLayout.LayoutParams.() -> Unit) {
    if (layoutParams !is ConstraintLayout.LayoutParams) {
        StreamLog.w("View::updateLayoutParams") {
            "Layout params are ${layoutParams::class.java.simpleName}. ConstraintLayout.LayoutParams required."
        }
    }
    layoutParams = (layoutParams as ConstraintLayout.LayoutParams).apply { updateParams() }
}
