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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.ArrayRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import io.getstream.video.android.xml.R

/**
 * Helper method to check is the system requests RTL direction.
 */
internal val Context.isRtlLayout: Boolean
    @JvmSynthetic get() = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

@Px
@JvmSynthetic
internal fun Context.getDimension(@DimenRes dimen: Int): Int {
    return resources.getDimensionPixelSize(dimen)
}

@JvmSynthetic
internal fun Context.getIntArray(@ArrayRes id: Int): IntArray {
    return resources.getIntArray(id)
}

@ColorInt
@JvmSynthetic
internal fun Context.getColorCompat(@ColorRes color: Int): Int {
    return ContextCompat.getColor(this, color)
}

@JvmSynthetic
internal fun Context.getColorStateListCompat(@ColorRes color: Int): ColorStateList? {
    return ContextCompat.getColorStateList(this, color)
}

@JvmSynthetic
internal fun Context.getDrawableCompat(@DrawableRes id: Int): Drawable? {
    return ContextCompat.getDrawable(this, id)
}

@JvmSynthetic
internal fun Context?.getFragmentManager(): FragmentManager? {
    return when (this) {
        is AppCompatActivity -> supportFragmentManager
        is ContextWrapper -> baseContext.getFragmentManager()
        else -> null
    }
}

@JvmSynthetic
internal fun Context.copyToClipboard(text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(null, text))
}

internal val Context.streamThemeInflater: LayoutInflater
    @JvmSynthetic get() = LayoutInflater.from(this.createStreamThemeWrapper())

@JvmSynthetic
internal fun Context.createStreamThemeWrapper(): Context {
    val typedValue = TypedValue()
    return when {
        theme.resolveAttribute(R.attr.streamVideoValidTheme, typedValue, true) -> this
        theme.resolveAttribute(R.attr.streamVideoTheme, typedValue, true) -> ContextThemeWrapper(
            this,
            typedValue.resourceId,
        )
        else -> ContextThemeWrapper(this, R.style.StreamVideoTheme)
    }
}

@JvmSynthetic
internal fun Context.getResourceId(style: Int, attr: Int): Int {
    return theme.obtainStyledAttributes(
        style,
        intArrayOf(attr),
    ).getResourceId(0, 0)
}

internal val Context.isLandscape: Boolean
    @JvmSynthetic get() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
