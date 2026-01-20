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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import io.getstream.video.android.xml.utils.OrientationChangeListener

internal inline val ViewGroup.inflater: LayoutInflater
    @JvmSynthetic get() = LayoutInflater.from(context)

internal val ViewGroup.streamThemeInflater: LayoutInflater
    @JvmSynthetic get() = LayoutInflater.from(context.createStreamThemeWrapper())

/**
 * Returns the first view that conforms to the predicate.
 *
 * @param predicate Optional parameter to find the first view that conforms to the predicate.
 */
@JvmSynthetic
internal inline fun <reified T : View> ViewGroup.getFirstViewInstance(
    predicate: (T) -> Boolean = { true },
): T? {
    return children.firstOrNull { it is T && predicate(it) } as? T
}

/**
 * Notifies the whole view tree that the orientation has changed. Views that implement [OrientationChangeListener] will
 * get notified of this change.
 *
 * @param isLandscape Whether the orientation is landscape or not.
 */
@JvmSynthetic
internal fun ViewGroup.orientationChanged(isLandscape: Boolean) {
    if (this is OrientationChangeListener) this.onOrientationChanged(isLandscape)
    children.forEach {
        if (it is ViewGroup) it.orientationChanged(isLandscape)
        if (it !is ViewGroup && it is OrientationChangeListener) {
            it.onOrientationChanged(
                isLandscape,
            )
        }
    }
}
