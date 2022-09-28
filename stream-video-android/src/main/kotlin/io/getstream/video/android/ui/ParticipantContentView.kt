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

package io.getstream.video.android.ui

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import io.getstream.video.android.R
import io.getstream.video.android.dispatchers.DispatcherProvider
import io.getstream.video.android.model.Call
import io.getstream.video.android.model.CallParticipant
import io.getstream.video.android.model.VideoTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

public class ParticipantContentView : LinearLayout {

    public constructor(context: Context?) : super(context) {
        init(context)
    }

    public constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    public constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    public constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context)
    }

    public fun init(context: Context?) {
        LayoutInflater.from(context).inflate(R.layout.content_participant, this, true)
    }

    public fun renderParticipants(call: Call, participants: List<CallParticipant>) {
        val otherParticipants = participants.filter { !it.isLocal }
        Log.d("RoomState", otherParticipants.toString())

        when (otherParticipants.size) {
            1 -> renderSingleParticipant(call, otherParticipants[0])
            2 -> renderTwoParticipants(call, otherParticipants[0], otherParticipants[1])
            3 -> renderThreeParticipants(
                call,
                otherParticipants[0],
                otherParticipants[1],
                otherParticipants[2]
            )
            4 -> renderFourParticipants(
                call,
                otherParticipants[0],
                otherParticipants[1],
                otherParticipants[2],
                otherParticipants[3]
            )
        }
    }

    private fun renderSingleParticipant(call: Call, callParticipant: CallParticipant) {
        Log.d("RoomState", "Rendering single, $callParticipant")

        cleanUpViews()
        setSecondRowVisibility(View.GONE)

        renderTrack(
            findViewById(R.id.firstParticipant),
            call,
            callParticipant.track
        )
    }

    private fun renderTwoParticipants(call: Call, first: CallParticipant, second: CallParticipant) {
        Log.d("RoomState", "Rendering two, $first $second")

        cleanUpViews()
        setSecondRowVisibility(View.VISIBLE)

        renderTrack(
            findViewById(R.id.firstParticipant),
            call,
            first.track
        )

        renderTrack(
            findViewById(R.id.secondParticipant),
            call,
            second.track
        )
    }

    private fun renderThreeParticipants(
        call: Call,
        first: CallParticipant,
        second: CallParticipant,
        third: CallParticipant
    ) {
        cleanUpViews()
        setSecondRowVisibility(View.VISIBLE)

        renderTrack(
            findViewById(R.id.firstParticipant),
            call,
            first.track
        )

        renderTrack(
            findViewById(R.id.secondParticipant),
            call,
            second.track
        )

        renderTrack(
            findViewById(R.id.thirdParticipant),
            call,
            third.track
        )
    }

    private fun renderFourParticipants(
        call: Call,
        first: CallParticipant,
        second: CallParticipant,
        third: CallParticipant,
        fourth: CallParticipant
    ) {
        cleanUpViews()
        setSecondRowVisibility(View.VISIBLE)

        renderTrack(
            findViewById(R.id.firstParticipant),
            call,
            first.track
        )

        renderTrack(
            findViewById(R.id.secondParticipant),
            call,
            second.track
        )

        renderTrack(
            findViewById(R.id.thirdParticipant),
            call,
            third.track
        )

        renderTrack(
            findViewById(R.id.fourthParticipant),
            call,
            fourth.track
        )
    }

    private fun renderTrack(
        participantItemView: ParticipantItemView,
        call: Call,
        track: VideoTrack?
    ) {
        val video = track?.video

        if (video != null) {
            participantItemView.visibility = View.VISIBLE

            if (!participantItemView.isInitialized) {
                participantItemView.initialize(call, track.streamId) {
                    CoroutineScope(DispatcherProvider.Main).launch {
                        participantItemView.z = -10f
                        participantItemView.elevation = 0f
                    }
                }
            }

            participantItemView.cleanUp()
            participantItemView.bindTrack(video)
        }
    }

    private fun setSecondRowVisibility(visibility: Int) {
        findViewById<LinearLayout>(R.id.secondParticipantRow).apply {
            this.visibility = visibility
        }
    }

    private fun cleanUpViews() {
        listOf<ParticipantItemView>(
            findViewById(R.id.firstParticipant),
            findViewById(R.id.secondParticipant),
            findViewById(R.id.thirdParticipant),
            findViewById(R.id.fourthParticipant)
        ).forEach {
            it.cleanUp()
            it.visibility = View.GONE
        }
    }
}
