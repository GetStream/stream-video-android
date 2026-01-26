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

package io.getstream.video.android.xml.widget.appbar

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.children
import androidx.core.view.setPadding
import io.getstream.video.android.core.ConnectionState
import io.getstream.video.android.xml.databinding.StreamVideoViewCallAppBarBinding
import io.getstream.video.android.xml.font.TextStyle
import io.getstream.video.android.xml.font.setTextStyle
import io.getstream.video.android.xml.utils.extensions.createStreamThemeWrapper
import io.getstream.video.android.xml.utils.extensions.getFirstViewInstance
import io.getstream.video.android.xml.utils.extensions.isLandscape
import io.getstream.video.android.xml.utils.extensions.streamThemeInflater
import io.getstream.video.android.xml.widget.appbar.internal.DefaultCallAppBarCenterContent
import io.getstream.video.android.xml.widget.appbar.internal.DefaultCallAppBarLeadingContent
import io.getstream.video.android.xml.widget.appbar.internal.DefaultCallAppBarTrailingContent
import io.getstream.video.android.xml.widget.view.CallConstraintLayout

/**
 * UI component designed to show a app bar. By default will contain a back button, title and a participants button
 * to view all participants and invite new ones.
 */
public class CallAppBarView : CallConstraintLayout {

    private val binding = StreamVideoViewCallAppBarBinding.inflate(streamThemeInflater, this)

    private lateinit var style: CallAppBarStyle

    /**
     * Handler that notifies when back button was pressed.
     */
    public var onBackPressed: () -> Unit = { }

    /**
     * Handler that notifies when participants button was pressed.
     */
    public var onParticipantsPressed: () -> Unit = { }

    public constructor(context: Context) : this(context, null)
    public constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context.createStreamThemeWrapper(),
        attrs,
        defStyleAttr,
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        style = CallAppBarStyle(context, attrs)

        background = ColorDrawable(style.backgroundColour)
        setPadding(style.appBarPadding)

        setupLeadingContent()
        setupCenterContent()
        setupTrailingContent()
    }

    /**
     * Adds margins and sets up the leading content on view initialization.
     */
    private fun setupLeadingContent() {
        val params = binding.callAppBarLeadingContent.layoutParams as LayoutParams
        params.marginStart = style.leadingContentMarginStart
        params.marginEnd = style.leadingContentMarginEnd
        binding.callAppBarLeadingContent.layoutParams = params
        setLeadingContent(defaultLeadingContent())
    }

    /**
     * Adds margins and sets up the center content on view initialization.
     */
    private fun setupCenterContent() {
        val params = binding.callAppBarCenterContent.layoutParams as LayoutParams
        params.marginStart = style.centerContentMarginStart
        params.marginEnd = style.centerContentMarginEnd
        binding.callAppBarCenterContent.layoutParams = params
        setCenterContent(defaultCenterContent())
    }

    /**
     * Adds margins and sets up the trailing content on view initialization.
     */
    private fun setupTrailingContent() {
        val params = binding.callAppBarTrailingContent.layoutParams as LayoutParams
        params.marginStart = style.trailingContentMarginStart
        params.marginEnd = style.trailingContentMarginEnd
        binding.callAppBarTrailingContent.layoutParams = params
        setTrailingContent(defaultTrailingContent())
    }

    /**
     * Builds a default call app bar leading content.
     *
     * @return [DefaultCallAppBarLeadingContent]
     */
    private fun defaultLeadingContent(): DefaultCallAppBarLeadingContent {
        return DefaultCallAppBarLeadingContent(context).apply {
            setImageDrawable(style.leadingContentIcon)
            setColorFilter(style.leadingContentIconTint)
            setOnClickListener { onBackPressed() }
        }
    }

    /**
     * Builds a default call app bar center content.
     *
     * @return [DefaultCallAppBarCenterContent]
     */
    private fun defaultCenterContent(): DefaultCallAppBarCenterContent {
        return DefaultCallAppBarCenterContent(context).apply {
            setTextStyle(style.centerContentTextStyle)
        }
    }

    /**
     * Builds a default call app bar trailing content.
     *
     * @return [DefaultCallAppBarTrailingContent]
     */
    private fun defaultTrailingContent(): DefaultCallAppBarTrailingContent {
        return DefaultCallAppBarTrailingContent(context).apply {
            setImageDrawable(style.trailingContentIcon)
            setColorFilter(style.trailingContentIconTint)
            setOnClickListener { onParticipantsPressed() }
        }
    }

    /**
     * Sets custom center content view. It must implement [CallAppBarContent] interface.
     * The current mode is propagated to the [contentView] in the [CallAppBarView.renderState] function.
     *
     * @param contentView The [View] showing the leading content.
     * @param layoutParams The layout parameters to set on the content view.
     * @see [DefaultCallAppBarLeadingContent]
     */
    public fun <V> setLeadingContent(
        contentView: V,
        layoutParams: FrameLayout.LayoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.START,
        ),
    ) where V : View, V : CallAppBarContent {
        binding.callAppBarLeadingContent.removeAllViews()
        binding.callAppBarLeadingContent.addView(contentView, layoutParams)
    }

    /**
     * Sets custom center content view. It must implement [CallAppBarContent] interface.
     * The current mode is propagated to the [contentView] in the [CallAppBarView.renderState] function.
     *
     * @param contentView The [View] showing the center content.
     * @param layoutParams The layout parameters to set on the content view.
     * @see [DefaultCallAppBarCenterContent]
     */
    public fun <V> setCenterContent(
        contentView: V,
        layoutParams: FrameLayout.LayoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.START,
        ),
    ) where V : View, V : CallAppBarContent {
        binding.callAppBarCenterContent.removeAllViews()
        binding.callAppBarCenterContent.addView(contentView, layoutParams)
    }

    /**
     * Sets custom center content view. It must implement [CallAppBarContent] interface.
     * The current mode is propagated to the [contentView] in the [CallAppBarView.renderState] function.
     *
     * @param contentView The [View] showing the trailing content.
     * @param layoutParams The layout parameters to set on the content view.
     * @see [DefaultCallAppBarTrailingContent]
     */
    public fun <V> setTrailingContent(
        contentView: V,
        layoutParams: FrameLayout.LayoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.START,
        ),
    ) where V : View, V : CallAppBarContent {
        binding.callAppBarTrailingContent.removeAllViews()
        binding.callAppBarTrailingContent.addView(contentView, layoutParams)
    }

    /**
     * Invoked when the state has changed and the UI needs to be updated accordingly.
     *
     * @param callState The state that will be used to render the updated UI.
     */
    public fun renderState(callState: ConnectionState) {
        (binding.callAppBarLeadingContent.children.firstOrNull() as? CallAppBarContent)?.renderState(
            callState,
        )
        (binding.callAppBarCenterContent.children.firstOrNull() as? CallAppBarContent)?.renderState(
            callState,
        )
        (binding.callAppBarTrailingContent.children.firstOrNull() as? CallAppBarContent)?.renderState(
            callState,
        )
    }

    override fun onOrientationChanged(isLandscape: Boolean) {
        updateContentStyles()
    }

    /**
     * Updates the colour and styles of the toolbar content on orientation change.
     */
    private fun updateContentStyles() {
        val leadingIconTint: Int
        val centerContentTextStyle: TextStyle
        val trailingContentIconTint: Int
        val backgroundColor: Int

        if (isLandscape) {
            leadingIconTint = style.leadingContentIconTintLandscape
            centerContentTextStyle = style.centerContentTextStyleLandscape
            trailingContentIconTint = style.trailingContentIconTintLandscape
            backgroundColor = style.backgroundColourLandscape
        } else {
            leadingIconTint = style.leadingContentIconTint
            centerContentTextStyle = style.centerContentTextStyle
            trailingContentIconTint = style.trailingContentIconTint
            backgroundColor = style.backgroundColour
        }

        binding.callAppBarLeadingContent.getFirstViewInstance<DefaultCallAppBarLeadingContent>()
            ?.setColorFilter(leadingIconTint)
        binding.callAppBarCenterContent.getFirstViewInstance<DefaultCallAppBarCenterContent>()
            ?.setTextStyle(centerContentTextStyle)
        binding.callAppBarTrailingContent.getFirstViewInstance<DefaultCallAppBarTrailingContent>()
            ?.setColorFilter(trailingContentIconTint)

        setBackgroundColor(backgroundColor)
    }
}
