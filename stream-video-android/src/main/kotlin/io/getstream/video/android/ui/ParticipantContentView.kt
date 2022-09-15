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
import io.getstream.video.android.model.CallParticipant
import io.getstream.video.android.model.Room
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

    public fun renderParticipants(room: Room, participants: List<CallParticipant>) {
        val otherParticipants = participants.filter { !it.isLocal }
        Log.d("RoomState", otherParticipants.toString())

        when (otherParticipants.size) {
            1 -> renderSingleParticipant(room, otherParticipants[0])
            2 -> renderTwoParticipants(room, otherParticipants[0], otherParticipants[1])
            3 -> renderThreeParticipants(
                room,
                otherParticipants[0],
                otherParticipants[1],
                otherParticipants[2]
            )
            4 -> renderFourParticipants(
                room,
                otherParticipants[0],
                otherParticipants[1],
                otherParticipants[2],
                otherParticipants[3]
            )
        }
    }

    private fun renderSingleParticipant(room: Room, callParticipant: CallParticipant) {
        Log.d("RoomState", "Rendering single, $callParticipant")

        cleanUpViews()
        setSecondRowVisibility(View.GONE)

        renderTrack(
            findViewById(R.id.firstParticipant),
            room,
            callParticipant.track
        )
    }

    private fun renderTwoParticipants(room: Room, first: CallParticipant, second: CallParticipant) {
        Log.d("RoomState", "Rendering two, $first $second")

        cleanUpViews()
        setSecondRowVisibility(View.VISIBLE)

        renderTrack(
            findViewById(R.id.firstParticipant),
            room,
            first.track
        )

        renderTrack(
            findViewById(R.id.secondParticipant),
            room,
            second.track
        )
    }

    private fun renderThreeParticipants(
        room: Room,
        first: CallParticipant,
        second: CallParticipant,
        third: CallParticipant
    ) {
        cleanUpViews()
        setSecondRowVisibility(View.VISIBLE)

        renderTrack(
            findViewById(R.id.firstParticipant),
            room,
            first.track
        )

        renderTrack(
            findViewById(R.id.secondParticipant),
            room,
            second.track
        )

        renderTrack(
            findViewById(R.id.thirdParticipant),
            room,
            third.track
        )
    }

    private fun renderFourParticipants(
        room: Room,
        first: CallParticipant,
        second: CallParticipant,
        third: CallParticipant,
        fourth: CallParticipant
    ) {
        cleanUpViews()
        setSecondRowVisibility(View.VISIBLE)

        renderTrack(
            findViewById(R.id.firstParticipant),
            room,
            first.track
        )

        renderTrack(
            findViewById(R.id.secondParticipant),
            room,
            second.track
        )

        renderTrack(
            findViewById(R.id.thirdParticipant),
            room,
            third.track
        )

        renderTrack(
            findViewById(R.id.fourthParticipant),
            room,
            fourth.track
        )
    }

    private fun renderTrack(
        participantItemView: ParticipantItemView,
        room: Room,
        track: VideoTrack?
    ) {
        val video = track?.video

        if (video != null) {
            participantItemView.visibility = View.VISIBLE

            if (!participantItemView.isInitialized) {
                participantItemView.initialize(room, track.streamId) {
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
