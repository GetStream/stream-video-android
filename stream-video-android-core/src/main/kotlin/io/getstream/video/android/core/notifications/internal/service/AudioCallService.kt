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

package io.getstream.video.android.core.notifications.internal.service

import android.content.pm.ServiceInfo
import io.getstream.log.TaggedLogger
import io.getstream.log.taggedLogger

/**
 * This Service restricts to only earpiece and mic usage.
 * We cannot use speaker phone unlike other apps WhatsApp which allows speakerphone
 * usage even in audio call
 *
 * Current solution
 * 1. Replace AudioCallService with Callservice ~ Risk ~ CallService allows camera access as well.
 * This might tell the users that the sdk is accessing the camera ~ Privacy issue
 * 2. Create new Service AudioCallServiceV2 with corrected service type
 */
internal class AudioCallService : CallService() {
    override val logger: TaggedLogger by taggedLogger("AudioCallService")
    override val serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
}
