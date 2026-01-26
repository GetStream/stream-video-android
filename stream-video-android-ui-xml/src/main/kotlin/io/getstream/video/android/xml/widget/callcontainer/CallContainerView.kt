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

package io.getstream.video.android.xml.widget.callcontainer

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.children
import io.getstream.video.android.xml.utils.extensions.clearConstraints
import io.getstream.video.android.xml.utils.extensions.constrainViewToParentBySide
import io.getstream.video.android.xml.utils.extensions.constrainViewTopToBottomOfView
import io.getstream.video.android.xml.utils.extensions.createStreamThemeWrapper
import io.getstream.video.android.xml.utils.extensions.getFirstViewInstance
import io.getstream.video.android.xml.utils.extensions.isLandscape
import io.getstream.video.android.xml.utils.extensions.updateConstraints
import io.getstream.video.android.xml.utils.extensions.updateLayoutParams
import io.getstream.video.android.xml.widget.appbar.CallAppBarView
import io.getstream.video.android.xml.widget.call.CallView
import io.getstream.video.android.xml.widget.incoming.IncomingCallView
import io.getstream.video.android.xml.widget.outgoing.OutgoingCallView
import io.getstream.video.android.xml.widget.participant.PictureInPictureView
import io.getstream.video.android.xml.widget.view.CallConstraintLayout

/**
 * View that is the highest in the hierarchy that handles switching of [IncomingCallView], [OutgoingCallView],
 * [CallView] and [PictureInPictureView] based on the call state.
 */
public class CallContainerView : CallConstraintLayout {

    private lateinit var style: CallContainerStyle

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
        style = CallContainerStyle(context, attrs)
    }

    private fun addToolbar(onViewInitialized: (CallAppBarView) -> Unit) {
        if (getFirstViewInstance<CallAppBarView>() != null) return
        val callAppBar = CallAppBarView(context).apply {
            id = ViewGroup.generateViewId()
            this@CallContainerView.addView(this)
            onViewInitialized(this)
            updateLayoutParams {
                width = LayoutParams.MATCH_CONSTRAINT
                height = style.appBarHeight
            }
        }

        updateConstraints {
            constrainViewToParentBySide(callAppBar, ConstraintSet.TOP)
            constrainViewToParentBySide(callAppBar, ConstraintSet.START)
            constrainViewToParentBySide(callAppBar, ConstraintSet.END)
        }
    }

    /**
     * Shows the outgoing call screen and initialises the state observers required to populate the screen.
     *
     * @param onViewInitialized Notifies when a [OutgoingCallView] has been initialised so that it can be bound to the
     * view model.
     */
    internal fun showOutgoingScreen(onViewInitialized: (View) -> Unit) {
        addToolbar(onViewInitialized)

        if (getFirstViewInstance<OutgoingCallView>() != null) return
        OutgoingCallView(context).apply {
            id = ViewGroup.generateViewId()
            setContentView(this)
            onViewInitialized(this)
        }
    }

    /**
     * Shows the incoming call screen and initialises the state observers required to populate the screen.
     *
     * @param onViewInitialized Notifies when a [IncomingCallView] has been initialised so that it can be bound to the
     * view model.
     */
    internal fun showIncomingScreen(onViewInitialized: (View) -> Unit) {
        addToolbar(onViewInitialized)

        if (getFirstViewInstance<IncomingCallView>() != null) return
        IncomingCallView(context).apply {
            id = ViewGroup.generateViewId()
            setContentView(this)
            onViewInitialized(this)
        }
    }

    /**
     * Shows the active call screen and initialises the state observers required to populate the screen.
     *
     * @param onViewInitialized Notifies when [CallView] or [CallControlsView] has been initialised so that it can be
     * bound to the view model.
     */
    internal fun showCallContent(onViewInitialized: (View) -> Unit) {
        addToolbar(onViewInitialized)

        children.forEach {
            if (it !is CallAppBarView && it !is CallView) removeView(it)
        }

        if (getFirstViewInstance<CallView>() == null) {
            CallView(context).apply {
                id = ViewGroup.generateViewId()
                this@CallContainerView.addView(this)
                onViewInitialized(this)
            }
        }

        updateCallContentConstraints()
    }

    override fun onOrientationChanged(isLandscape: Boolean) {
        updateCallContentConstraints()
    }

    /**
     * Updates constraints when the call is active.
     */
    private fun updateCallContentConstraints() {
        val appBar = getFirstViewInstance<CallAppBarView>() ?: return
        val callView = getFirstViewInstance<CallView>() ?: return

        if (isLandscape) {
            updateConstraints(true) {
                constrainViewToParentBySide(appBar, ConstraintSet.TOP)
                constrainViewToParentBySide(appBar, ConstraintSet.START)
                constrainViewToParentBySide(
                    appBar,
                    ConstraintSet.END,
                    callView.getCallControlsSize(),
                )
            }
            appBar.updateLayoutParams {
                width = LayoutParams.MATCH_CONSTRAINT
                height = style.landscapeAppBarHeight
            }
            callView.updateLayoutParams {
                width = LayoutParams.MATCH_PARENT
                height = LayoutParams.MATCH_PARENT
            }
        } else {
            updateConstraints {
                clearConstraints(appBar)
                constrainViewToParentBySide(appBar, ConstraintSet.TOP)

                constrainViewToParentBySide(callView, ConstraintSet.BOTTOM)
                constrainViewTopToBottomOfView(callView, appBar)
            }
            appBar.updateLayoutParams {
                width = LayoutParams.MATCH_PARENT
                height = style.appBarHeight
            }
            callView.updateLayoutParams {
                width = LayoutParams.MATCH_PARENT
                height = LayoutParams.MATCH_CONSTRAINT
            }
        }
    }

    /**
     * Shows the picture in picture layout which consists of the primary call participants feed.
     *
     * @param onViewInitialized Notifies when a [PictureInPictureView] has been initialised so that it can be bound to
     * the view model.
     */
    internal fun showPipLayout(onViewInitialized: (PictureInPictureView) -> Unit) {
        if (getFirstViewInstance<PictureInPictureView>() != null) return
        removeAllViews()
        PictureInPictureView(context).apply {
            id = ViewGroup.generateViewId()
            setContentView(this)
            onViewInitialized(this)
        }
    }

    /**
     * Sets the passed view as the primary content.
     *
     * @param view The view we wish to display as primary content.
     */
    private fun setContentView(view: View) {
        children.filter { it !is CallAppBarView }.forEach(::removeView)
        addView(
            view,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_CONSTRAINT,
            ),
        )
        view.updateLayoutParams {
            width = LayoutParams.MATCH_PARENT
            height = LayoutParams.MATCH_CONSTRAINT
        }
        val appBarView = getFirstViewInstance<CallAppBarView>()
        updateConstraints {
            if (appBarView != null) {
                constrainViewTopToBottomOfView(view, appBarView)
            } else {
                constrainViewToParentBySide(view, ConstraintSet.TOP)
            }
            constrainViewToParentBySide(view, ConstraintSet.BOTTOM)
        }
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        super.addView(child, index, params)
        getFirstViewInstance<CallAppBarView>()?.bringToFront()
    }
}
