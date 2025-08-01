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

package io.getstream.video.android.compose.ui.components.participants.internal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.MemberState
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.model.CallStatus
import io.getstream.video.android.core.utils.toCallUser
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewMemberListState
import io.getstream.video.android.mock.previewThreeMembers
import io.getstream.video.android.mock.previewTwoMembers
import io.getstream.video.android.ui.common.util.buildLargeCallText
import io.getstream.video.android.ui.common.util.buildSmallCallText

@Deprecated(
    message = "This version of ParticipantInformation is deprecated. Use the newer overload.",
    replaceWith = ReplaceWith("ParticipantInformation"),
)
@Composable
public fun ParticipantInformation(
    callStatus: CallStatus,
    participants: List<MemberState>,
    isVideoType: Boolean = true,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val context = LocalContext.current
        val callUsers by remember { derivedStateOf { participants.map { it.toCallUser() } } }
        val text = if (participants.size <= 3) {
            buildSmallCallText(context, callUsers)
        } else {
            buildLargeCallText(context, callUsers)
        }

        val fontSize = if (participants.size == 1) {
            VideoTheme.dimens.textSizeL
        } else {
            VideoTheme.dimens.textSizeM
        }

        Text(
            modifier = Modifier.padding(horizontal = VideoTheme.dimens.spacingM),
            text = text,
            fontSize = fontSize,
            color = VideoTheme.colors.basePrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(VideoTheme.dimens.spacingM))

        val callType = if (isVideoType) {
            "video"
        } else {
            "audio"
        }

        Text(
            text = when (callStatus) {
                CallStatus.Incoming -> stringResource(
                    id = io.getstream.video.android.ui.common.R.string.stream_video_call_status_incoming,
                    callType.capitalize(Locale.current),
                )

                CallStatus.Outgoing -> stringResource(
                    id = io.getstream.video.android.ui.common.R.string.stream_video_call_status_outgoing,
                )

                is CallStatus.Calling -> callStatus.duration
            },
            style = VideoTheme.typography.bodyM,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Component that renders user names for a call.
 *
 * @param members The list of call members to render names for. If `null`, [participants] will be used instead. Takes precedence over `participants` if both are not `null`.
 * @param participants The list of call participants to render names for. If `null`, [members] will be used instead.
 */
@Composable
public fun ParticipantInformation(
    callStatus: CallStatus,
    members: List<MemberState>? = null,
    participants: List<ParticipantState>? = null,
    isVideoType: Boolean = true,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val context = LocalContext.current
        val callUsers by remember(members, participants) {
            derivedStateOf {
                members?.map { it.toCallUser() }
                    ?: participants?.map { it.toCallUser() }
                    ?: emptyList()
            }
        }

        val text = if (callUsers.size <= 3) {
            buildSmallCallText(context, callUsers)
        } else {
            buildLargeCallText(context, callUsers)
        }

        val fontSize = if (callUsers.size == 1) {
            VideoTheme.dimens.textSizeL
        } else {
            VideoTheme.dimens.textSizeM
        }

        Text(
            modifier = Modifier
                .padding(horizontal = VideoTheme.dimens.spacingM)
                .testTag("Stream_ParticipantInformation"),
            text = text,
            fontSize = fontSize,
            color = VideoTheme.colors.basePrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(VideoTheme.dimens.spacingM))

        val callType = if (isVideoType) {
            "video"
        } else {
            "audio"
        }

        Text(
            modifier = when (callStatus) {
                CallStatus.Incoming -> Modifier.testTag("Stream_IncomingCallLabel")
                CallStatus.Outgoing -> Modifier.testTag("Stream_OutgoingCallLabel")
                is CallStatus.Calling -> Modifier.testTag("Stream_OngoingCallDurationLabel")
            },
            text = when (callStatus) {
                CallStatus.Incoming -> stringResource(
                    id = io.getstream.video.android.ui.common.R.string.stream_video_call_status_incoming,
                    callType,
                )

                CallStatus.Outgoing -> stringResource(
                    id = io.getstream.video.android.ui.common.R.string.stream_video_call_status_outgoing,
                )

                is CallStatus.Calling -> callStatus.duration
            },
            style = VideoTheme.typography.bodyM,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview("2 users")
@Composable
private fun ParticipantInformationTwoUsersPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        ParticipantInformation(
            isVideoType = true,
            callStatus = CallStatus.Incoming,
            members = previewTwoMembers,
        )
    }
}

@Preview("3 users")
@Composable
private fun ParticipantInformationThreeUsersPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        ParticipantInformation(
            isVideoType = true,
            callStatus = CallStatus.Incoming,
            members = previewThreeMembers,
        )
    }
}

@Preview("More than 3 users")
@Composable
private fun ParticipantInformationPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        ParticipantInformation(
            isVideoType = true,
            callStatus = CallStatus.Incoming,
            members = previewMemberListState,
        )
    }
}
