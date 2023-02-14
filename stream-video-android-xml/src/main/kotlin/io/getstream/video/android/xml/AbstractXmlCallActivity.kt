/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.xml

import android.view.Menu
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import io.getstream.video.android.common.AbstractCallActivity
import io.getstream.video.android.core.model.state.StreamCallState
import io.getstream.video.android.core.utils.formatAsTitle
import io.getstream.video.android.xml.binding.bindView
import io.getstream.video.android.xml.databinding.ActivityCallBinding
import io.getstream.video.android.xml.utils.extensions.streamThemeInflater
import io.getstream.video.android.xml.widget.active.ActiveCallView
import io.getstream.video.android.xml.widget.incoming.IncomingCallView
import io.getstream.video.android.xml.widget.outgoing.OutgoingCallView
import io.getstream.video.android.xml.widget.participant.CallParticipantView
import io.getstream.video.android.xml.widget.participant.PictureInPictureView
import io.getstream.video.android.xml.widget.participant.RendererInitializer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull

public abstract class AbstractXmlCallActivity : AbstractCallActivity() {

    private var pipJob: Job? = null

    private val binding by lazy { ActivityCallBinding.inflate(streamThemeInflater) }

    override fun setupUi() {
        setContentView(binding.root)

        setupToolbar()
    }

    /**
     * Observes the current call state and sets the screen state accordingly.
     */
    override fun observeStreamCallState() {
        lifecycleScope.launchWhenCreated {
            callViewModel.streamCallState.combine(callViewModel.isInPictureInPicture) { state, isPictureInPicture ->
                updateToolbar(state, isPictureInPicture)
                when {
                    state is StreamCallState.Incoming && !state.acceptedByMe -> showIncomingScreen()
                    state is StreamCallState.Outgoing && !state.acceptedByCallee -> showOutgoingScreen()
                    state is StreamCallState.Connected && isPictureInPicture -> showPipLayout()
                    state is StreamCallState.Idle -> finish()
                    else -> showActiveCallScreen()
                }
            }.collect()
        }
    }

    /**
     * Updates the toolbar title depending on the call state.
     *
     * @param streamCallState The state of the call we are observing.
     * @param isPictureInPicture Whether the app is in picture in picture mode. If true will hide the toolbar or hide it
     * if it is false.
     */
    private fun updateToolbar(streamCallState: StreamCallState, isPictureInPicture: Boolean) {
        binding.callToolbar.isVisible = !isPictureInPicture

        val callId = when (streamCallState) {
            is StreamCallState.Active -> streamCallState.callGuid.id
            else -> ""
        }
        val status = streamCallState.formatAsTitle(this)

        val title = when (callId.isBlank()) {
            true -> status
            else -> "$status: $callId"
        }
        binding.callToolbar.title = title
    }

    /**
     * Sets up the toolbar.
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.callToolbar)
        supportActionBar?.let {
            it.setDisplayShowTitleEnabled(false)
            it.setDisplayShowHomeEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }
        binding.callToolbar.setNavigationOnClickListener { handleBackPressed() }
    }

    /**
     * Shows the outgoing call screen and initialises the state observers required to populate the screen.
     */
    private fun showOutgoingScreen() {
        if (isViewInsideContainer<OutgoingCallView>()) return
        val outgoingCallView = OutgoingCallView(this)
        addContentView(outgoingCallView)
        outgoingCallView.bindView(
            viewModel = callViewModel,
            lifecycleOwner = this,
            onCallAction = ::handleCallAction
        )
    }

    /**
     * Shows the incoming call screen and initialises the state observers required to populate the screen.
     */
    private fun showIncomingScreen() {
        if (isViewInsideContainer<IncomingCallView>()) return
        val incomingCallView = IncomingCallView(this)
        addContentView(incomingCallView)
        incomingCallView.bindView(
            viewModel = callViewModel,
            lifecycleOwner = this,
            onCallAction = ::handleCallAction
        )
    }

    /**
     * Shows the active call screen and initialises the state observers required to populate the screen.
     */
    private fun showActiveCallScreen() {
        if (isViewInsideContainer<ActiveCallView>()) return
        val activeCallView = ActiveCallView(this)
        addContentView(activeCallView)
        activeCallView.bindView(
            viewModel = callViewModel,
            lifecycleOwner = this,
            onCallAction = ::handleCallAction
        )
    }

    /**
     * Shows the picture in picture layout which consists of the primary call participants feed.
     */
    private fun showPipLayout() {
        if (isViewInsideContainer<CallParticipantView>()) return
        val callParticipant = PictureInPictureView(this)
        callParticipant.rendererInitializer = RendererInitializer { videoRenderer, streamId, trackType, onRender ->
            callViewModel.callState.value?.initRenderer(videoRenderer, streamId, trackType, onRender)
        }
        addContentView(callParticipant)
        pipJob?.cancel()
        pipJob = lifecycleScope.launchWhenCreated {
            callViewModel.primarySpeaker.filterNotNull().collect {
                callParticipant.participant = it
            }
        }
    }

    private fun addContentView(view: View) {
        view.layoutParams =
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        binding.contentHolder.removeAllViews()
        binding.contentHolder.addView(view)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.call_menu, menu)
        return true
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
