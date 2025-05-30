/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.notifications

import android.app.Application
import android.app.Notification
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.MemberState
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * Handles real-time notification updates during calls
 */
internal class NotificationUpdater(private val application: Application) {

    fun startNotificationUpdates(
        coroutineScope: CoroutineScope,
        call: Call,
        localUser: User,
        onUpdate: (Notification) -> Unit,
        getOngoingNotification: (StreamCallId, String?, Boolean, Int) -> Notification?,
    ) {
        val streamVideoClient = io.getstream.video.android.core.StreamVideo.instanceOrNull() as? io.getstream.video.android.core.StreamVideoClient
        if (streamVideoClient?.enableCallNotificationUpdates != true) return

        coroutineScope.launch {
            var latestRemoteParticipantCount = -1

            // Monitor call state and remote participants
            combine(
                call.state.ringingState,
                call.state.members,
                call.state.remoteParticipants,
            ) { ringingState, members, remoteParticipants ->
                CallStateData(ringingState, members, remoteParticipants)
            }.distinctUntilChanged()
                .filter {
                    it.ringingState is RingingState.Active ||
                        it.ringingState is RingingState.Outgoing
                }.collectLatest { stateData ->
                    when (stateData.ringingState) {
                        is RingingState.Outgoing -> {
                            handleOutgoingCallUpdate(
                                call,
                                stateData,
                                localUser,
                                onUpdate,
                                getOngoingNotification,
                            )
                        }
                        is RingingState.Active -> {
                            latestRemoteParticipantCount = handleActiveCallUpdate(
                                call,
                                stateData,
                                latestRemoteParticipantCount,
                                onUpdate,
                                getOngoingNotification,
                            )
                        }
                        else -> {
                            // Do nothing
                        }
                    }
                }
        }
    }

    private fun handleOutgoingCallUpdate(
        call: Call,
        stateData: CallStateData,
        localUser: User,
        onUpdate: (Notification) -> Unit,
        getOngoingNotification: (StreamCallId, String?, Boolean, Int) -> Notification?,
    ) {
        val remoteMembersCount = stateData.members.size - 1
        val callDisplayName =
            determineOutgoingCallDisplayName(stateData.members, remoteMembersCount, localUser)

        getOngoingNotification(
            StreamCallId.fromCallCid(call.cid),
            callDisplayName,
            true,
            remoteMembersCount,
        )?.let(onUpdate)
    }

    private fun handleActiveCallUpdate(
        call: Call,
        stateData: CallStateData,
        latestRemoteParticipantCount: Int,
        onUpdate: (Notification) -> Unit,
        getOngoingNotification: (StreamCallId, String?, Boolean, Int) -> Notification?,
    ): Int {
        val currentRemoteParticipantCount = stateData.remoteParticipants.size

        if (shouldUpdateActiveCallNotification(
                currentRemoteParticipantCount,
                latestRemoteParticipantCount,
            )
        ) {
            val callDisplayName = determineActiveCallDisplayName(stateData.remoteParticipants)

            getOngoingNotification(
                StreamCallId.fromCallCid(call.cid),
                callDisplayName,
                false,
                currentRemoteParticipantCount,
            )?.let(onUpdate)
        }

        return currentRemoteParticipantCount
    }

    private fun shouldUpdateActiveCallNotification(
        currentCount: Int,
        latestCount: Int,
    ): Boolean {
        if (currentCount == latestCount) return false
        // Don't update if both counts are > 1 (same case - group call)
        return !(currentCount > 1 && latestCount > 1)
    }

    private fun determineOutgoingCallDisplayName(
        members: List<MemberState>,
        remoteMembersCount: Int,
        localUser: User,
    ): String {
        return if (remoteMembersCount != 1) {
            application.getString(
                io.getstream.video.android.core.R.string.stream_video_outgoing_call_notification_title,
            )
        } else {
            members.firstOrNull { member ->
                member.user.id != localUser.id
            }?.user?.name ?: "Unknown"
        }
    }

    private fun determineActiveCallDisplayName(
        remoteParticipants: List<ParticipantState>,
    ): String {
        return if (remoteParticipants.isEmpty()) {
            application.getString(
                io.getstream.video.android.core.R.string.stream_video_ongoing_call_notification_title,
            )
        } else {
            if (remoteParticipants.size > 1) {
                // If more than 1 remote participant, get group call notification title
                application.getString(
                    io.getstream.video.android.core.R.string.stream_video_ongoing_group_call_notification_title,
                )
            } else {
                // If 1 remote participant, get the name of the remote participant
                remoteParticipants.firstOrNull()?.name?.value ?: "Unknown"
            }
        }
    }
}
