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

package io.getstream.video.android.xml.widget.callcontent

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.model.state.StreamCallState
import io.getstream.video.android.core.utils.formatAsTitle
import io.getstream.video.android.core.viewmodel.CallViewModel
import io.getstream.video.android.xml.binding.bindView
import io.getstream.video.android.xml.databinding.ViewCallContentBinding
import io.getstream.video.android.xml.utils.extensions.createStreamThemeWrapper
import io.getstream.video.android.xml.utils.extensions.streamThemeInflater
import io.getstream.video.android.xml.widget.active.ActiveCallView
import io.getstream.video.android.xml.widget.incoming.IncomingCallView
import io.getstream.video.android.xml.widget.outgoing.OutgoingCallView
import io.getstream.video.android.xml.widget.participant.PictureInPictureView

/**
 * View that is the highest in the hierarchy that handles switching of [IncomingCallView], [OutgoingCallView],
 * [ActiveCallView] and [PictureInPictureView] based on the call state.
 */
public class CallContentView : ConstraintLayout {

    private val binding = ViewCallContentBinding.inflate(streamThemeInflater, this)

    public var handleBackPressed: () -> Unit = { }

    public var handleCallAction: (CallAction) -> Unit = { }

    public constructor(context: Context) : this(context, null, 0)
    public constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context.createStreamThemeWrapper(),
        attrs,
        defStyleAttr
    )

    /**
     * Updates the toolbar title depending on the call state.
     *
     * @param streamCallState The state of the call we are observing.
     * @param isPictureInPicture Whether the app is in picture in picture mode. If true will hide the toolbar or hide it
     * if it is false.
     */
    internal fun updateToolbar(streamCallState: StreamCallState, isPictureInPicture: Boolean) {
        binding.callToolbar.isVisible = !isPictureInPicture
        if (isPictureInPicture) return

        val callId = when (streamCallState) {
            is StreamCallState.Active -> streamCallState.callGuid.id
            else -> ""
        }
        val status = streamCallState.formatAsTitle(context)

        val title = when (callId.isBlank()) {
            true -> status
            else -> "$status: $callId"
        }
        binding.callToolbar.title = title
    }

    /**
     * Sets up the toolbar.
     */
    internal fun setupToolbar(activity: AppCompatActivity) {
        activity.setSupportActionBar(binding.callToolbar)
        activity.supportActionBar?.let {
            it.setDisplayShowTitleEnabled(false)
            it.setDisplayShowHomeEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }
        binding.callToolbar.setNavigationOnClickListener { handleBackPressed() }
    }

    /**
     * Shows the outgoing call screen and initialises the state observers required to populate the screen.
     */
    internal fun showOutgoingScreen(): OutgoingCallView? {
        if (isViewInsideContainer<OutgoingCallView>()) return null
        val outgoingCallView = OutgoingCallView(context)
        setContentView(outgoingCallView)
        return outgoingCallView
    }

    /**
     * Shows the incoming call screen and initialises the state observers required to populate the screen.
     */
    internal fun showIncomingScreen(): IncomingCallView? {
        if (isViewInsideContainer<IncomingCallView>()) return null
        val incomingCallView = IncomingCallView(context)
        setContentView(incomingCallView)
        return incomingCallView
    }

    /**
     * Shows the active call screen and initialises the state observers required to populate the screen.
     */
    internal fun showActiveCallScreen(): ActiveCallView? {
        if (isViewInsideContainer<ActiveCallView>()) return null
        val activeCallView = ActiveCallView(context)
        setContentView(activeCallView)
        return activeCallView
    }

    /**
     * Shows the picture in picture layout which consists of the primary call participants feed.
     */
    internal fun showPipLayout(): PictureInPictureView? {
        if (isViewInsideContainer<PictureInPictureView>()) return null
        val pictureInPicture = PictureInPictureView(context)
        setContentView(pictureInPicture)
        return pictureInPicture
    }

    private fun setContentView(view: View) {
        view.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        binding.contentHolder.removeAllViews()
        binding.contentHolder.addView(view)
    }

    /**
     * Returns the view of type [T] if it is inside the content holder.
     *
     * @return The instance of [T] if one is inside the content holder, otherwise null.
     */
    private inline fun <reified T : View> getChildInstanceOf(): T? {
        return binding.contentHolder.children.firstOrNull { it is T } as? T
    }

    /**
     * Checks if the view inside the content is of type [T].
     *
     * @return Whether the instance of [T] is inside the content holder.
     */
    private inline fun <reified T : View> isViewInsideContainer(): Boolean {
        return getChildInstanceOf<T>() != null
    }
}