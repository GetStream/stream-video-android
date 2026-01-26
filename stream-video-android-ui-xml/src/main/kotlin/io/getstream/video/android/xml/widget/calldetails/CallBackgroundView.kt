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

package io.getstream.video.android.xml.widget.calldetails

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import io.getstream.video.android.core.model.CallUser
import io.getstream.video.android.xml.utils.extensions.load

/**
 * Sets the incoming/outgoing call background. If the call is 1:1 will show the avatar of the other user, if there are
 * more participants it will show a default background.
 */
public class CallBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr) {

    init {
        scaleType = ScaleType.CENTER_CROP
    }

    /**
     * Set the call participants to show the background image.
     *
     * @param participants The list of participants in the call.
     * @param groupCallBackground The background used if the call is group call.
     */
    public fun setParticipants(participants: List<CallUser>, groupCallBackground: Drawable?) {
        if (participants.size == 1) {
            loadImage(participants)
        } else {
            setGroupCallBackground(groupCallBackground)
        }
    }

    /**
     * Sets the default background.
     *
     * @param groupCallBackground The background drawable to be shown.
     */
    private fun setGroupCallBackground(groupCallBackground: Drawable?) {
        background = groupCallBackground
    }

    /**
     * Sets the participants avatar as the background.
     */
    private fun loadImage(participants: List<CallUser>) {
        val firstParticipant = participants.first()
        load(
            firstParticipant.imageUrl,
            io.getstream.video.android.xml.R.drawable.stream_video_bg_call,
        )
    }
}
