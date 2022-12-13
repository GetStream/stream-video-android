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

package io.getstream.video.android.ui.xml.widget.participant

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.getstream.video.android.model.VideoTrack
import io.getstream.video.android.ui.xml.databinding.ViewCallParticipantBinding
import io.getstream.video.android.ui.xml.utils.extensions.inflater

public class CallParticipantView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewCallParticipantBinding.inflate(inflater, this)

    private var wasRendererInitialised: Boolean = false
    private var rendererInitializer: RendererInitializer? = null
    private var track: VideoTrack? = null

    public fun setData(imageUrl: String, name: String) {
        binding.participantAvatar.setData(imageUrl, name)
        binding.participantName.text = name.ifBlank { "Empty" }
    }

    public fun setActive(isActive: Boolean) {
        // activeSpeakerView.isVisible = isActive
    }

    public fun setRendererInitializer(rendererInitializer: RendererInitializer) {
        this.rendererInitializer = rendererInitializer
        initRenderer()
    }

    public fun setTrack(track: VideoTrack?) {
        val isOverlayVisible = track?.video?.enabled() != true
        binding.overlayImage.isVisible = isOverlayVisible
        binding.participantAvatar.isVisible = isOverlayVisible

        if (this.track == track) return
        if (track == null) {
            this.track?.video?.removeSink(binding.participantVideoRenderer)
            this.track = null
            return
        }
        this.track = track
        this.track!!.video.addSink(binding.participantVideoRenderer)
        initRenderer()
    }

    private fun initRenderer() {
        if (!wasRendererInitialised) {
            track?.let {
                rendererInitializer?.initRenderer(binding.participantVideoRenderer, it.streamId) { /* no-op */ }
                wasRendererInitialised = true
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        binding.participantVideoRenderer.apply {
            track?.video?.removeSink(this)
            release()
        }
        track = null
    }
}
