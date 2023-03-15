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

package io.getstream.video.android.xml.widget.callcontainer

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.core.view.isVisible
import io.getstream.video.android.xml.databinding.ViewCallContainerBinding
import io.getstream.video.android.xml.utils.extensions.createStreamThemeWrapper
import io.getstream.video.android.xml.utils.extensions.streamThemeInflater
import io.getstream.video.android.xml.widget.call.CallView
import io.getstream.video.android.xml.widget.incoming.IncomingCallView
import io.getstream.video.android.xml.widget.outgoing.OutgoingCallView
import io.getstream.video.android.xml.widget.participant.PictureInPictureView

/**
 * View that is the highest in the hierarchy that handles switching of [IncomingCallView], [OutgoingCallView],
 * [CallView] and [PictureInPictureView] based on the call state.
 */
public class CallContainerView : ConstraintLayout {

    internal val binding = ViewCallContainerBinding.inflate(streamThemeInflater, this)

    public constructor(context: Context) : this(context, null, 0)
    public constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context.createStreamThemeWrapper(),
        attrs,
        defStyleAttr
    )

    /**
     * Shows the outgoing call screen and initialises the state observers required to populate the screen.
     *
     * @param onViewInitialized Notifies when a [OutgoingCallView] has been initialised so that it can be bound to the
     * view model.
     */
    internal fun showOutgoingScreen(onViewInitialized: (OutgoingCallView) -> Unit) {
        if (isViewInsideContainer<OutgoingCallView>()) return
        val outgoingCallView = OutgoingCallView(context)
        setContentView(outgoingCallView)
        binding.callToolbar.isVisible = true
        onViewInitialized(outgoingCallView)
    }

    /**
     * Shows the incoming call screen and initialises the state observers required to populate the screen.
     *
     * @param onViewInitialized Notifies when a [IncomingCallView] has been initialised so that it can be bound to the
     * view model.
     */
    internal fun showIncomingScreen(onViewInitialized: (IncomingCallView) -> Unit) {
        if (isViewInsideContainer<IncomingCallView>()) return
        val incomingCallView = IncomingCallView(context)
        setContentView(incomingCallView)
        binding.callToolbar.isVisible = true
        onViewInitialized(incomingCallView)
    }

    /**
     * Shows the active call screen and initialises the state observers required to populate the screen.
     *
     * @param onViewInitialized Notifies when a [CallView] has been initialised so that it can be bound to the
     * view model.
     */
    internal fun showCallContentScreen(onViewInitialized: (CallView) -> Unit) {
        if (isViewInsideContainer<CallView>()) return
        val callView = CallView(context)
        setContentView(callView)
        binding.callToolbar.isVisible = true
        onViewInitialized(callView)
    }

    /**
     * Shows the picture in picture layout which consists of the primary call participants feed.
     *
     * @param onViewInitialized Notifies when a [PictureInPictureView] has been initialised so that it can be bound to
     * the view model.
     */
    internal fun showPipLayout(onViewInitialized: (PictureInPictureView) -> Unit) {
        if (isViewInsideContainer<PictureInPictureView>()) return
        val pictureInPicture = PictureInPictureView(context)
        setContentView(pictureInPicture)
        binding.callToolbar.isVisible = false
        onViewInitialized(pictureInPicture)
    }

    /**
     * Sets the passed view as the primary content.
     *
     * @param view The view we wish to display as primary content.
     */
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
