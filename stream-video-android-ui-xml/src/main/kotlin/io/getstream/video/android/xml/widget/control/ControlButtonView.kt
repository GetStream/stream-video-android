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

package io.getstream.video.android.xml.widget.control

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.cardview.widget.CardView
import io.getstream.video.android.xml.databinding.StreamVideoViewControlButtonBinding
import io.getstream.video.android.xml.utils.extensions.createStreamThemeWrapper
import io.getstream.video.android.xml.utils.extensions.dpToPxPrecise
import io.getstream.video.android.xml.utils.extensions.streamThemeInflater

/**
 * Class used to show call actions. Call [setEnabled] to make the button opaque or not depending on state.
 */
internal class ControlButtonView : CardView {

    private val binding = StreamVideoViewControlButtonBinding.inflate(streamThemeInflater, this)

    private lateinit var style: ControlButtonStyle

    public constructor(context: Context) : this(context, null, 0)
    public constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context.createStreamThemeWrapper(),
        attrs,
        defStyleAttr,
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        style = ControlButtonStyle(context, attrs)
        initCard()
        initImageView()
    }

    private fun initCard() {
        radius = 2000.dpToPxPrecise()
        background = style.background
        background?.setTint(style.backgroundTint)
        isEnabled = style.enabled
    }

    private fun initImageView() {
        with(binding.buttonIcon) {
            setImageDrawable(style.icon)
            setColorFilter(style.iconTint)
        }
    }

    /**
     * Sets the background alpha depending if the control is enabled or disabled.
     *
     * @param enabled Whether the control is enabled or not.
     */
    override fun setEnabled(enabled: Boolean) {
        val alpha = if (enabled) style.backgroundEnabledAlpha else style.backgroundDisabledAlpha
        background?.alpha = (alpha * 255).toInt()
    }

    /**
     * Sets the background for the view. Creates new drawable so if a drawable is reused the states don't mix.
     *
     * @param background The background [Drawable] to be set, null to remove it.
     */
    override fun setBackground(background: Drawable?) {
        super.setBackground(background?.constantState?.newDrawable())
    }

    /**
     * Sets the icon for the control button.
     *
     * @param resId The resource id to be used as the icon.
     */
    public fun setImageResource(@DrawableRes resId: Int) {
        binding.buttonIcon.setImageResource(resId)
    }

    /**
     * Sets the icon tint the control button.
     *
     * @param color The color of the icon.
     */
    public fun setColorFilter(@ColorInt color: Int) {
        binding.buttonIcon.setColorFilter(color)
    }

    /**
     * Sets the icon for the control button.
     *
     * @param drawable The drawable to be used as the icon.
     */
    public fun setImageDrawable(drawable: Drawable?) {
        binding.buttonIcon.setImageDrawable(drawable)
    }
}
