/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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
import androidx.constraintlayout.widget.ConstraintLayout
import io.getstream.log.StreamLog

/**
 * Sets the ripple effect to background for clickable views.
 */
@JvmSynthetic
internal fun View.setBackgroundRipple() {
    val outValue = TypedValue()
    context.theme.resolveAttribute(
        android.R.attr.selectableItemBackgroundBorderless,
        outValue,
        true,
    )
    setBackgroundResource(outValue.resourceId)
}

internal val View.isLandscape: Boolean
    @JvmSynthetic get() = context.isLandscape

/**
 * Used to update the layout params of views inside [ConstraintLayout].
 *
 * @param updateParams Lambda that exposes the [ConstraintLayout.LayoutParams] to be manipulated and applied.
 */
@JvmSynthetic
internal inline fun View.updateLayoutParams(
    updateParams: ConstraintLayout.LayoutParams.() -> Unit,
) {
    if (layoutParams !is ConstraintLayout.LayoutParams) {
        StreamLog.w("View::updateLayoutParams") {
            "Layout params are ${layoutParams::class.java.simpleName}. ConstraintLayout.LayoutParams required."
        }
    }
    layoutParams = (layoutParams as ConstraintLayout.LayoutParams).apply { updateParams() }
}
