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

package io.getstream.video.android.core.permission.android

import android.content.Context
import io.getstream.video.android.core.Call

/**
 * Android permission check for [Call]
 * Depending on the call capabilities, [checkAndroidPermissions] will return true if all the android permissions are granted required by the call.
 * By default having a capability to stream audio for an audio call will require the [android.Manifest.permission.RECORD_AUDIO] permission.
 * This check ensures that the required permission based on the call configuration are granted.
 *
 * The default implementation checks for two permissions:
 *
 * [android.Manifest.permission.RECORD_AUDIO] if the call has [io.getstream.android.video.generated.models.OwnCapability.SendAudio] capability
 *
 * and
 *
 * [android.Manifest.permission.CAMERA] if the call has [io.getstream.android.video.generated.models.OwnCapability.SendVideo] capability.
 *
 * This means that editing the configuration of the call has effect on which permissions are checked.
 * It is possible to provide a separate implementation to this interface to override this behavior.
 *
 * NOTE:
 * If the [io.getstream.video.android.core.StreamVideoBuilder.runForegroundServiceForCalls] is true
 * the foreground service that starts will crash if [checkAndroidPermissions] returns false.
 *
 * @see [io.getstream.video.android.core.StreamVideoBuilder]
 */
interface StreamPermissionCheck {

    /**
     * Return true if the user granted all the permissions so the [Call] can run.
     *
     * e.g. if the user granted the android.Manifest.permission.RECORD_AUDIO and the Call.state.ownCapability has "SendAudio"
     * then we are safe to join and use the [call] since the permission for recording audio is granted and the user can stream the audio track.
     */
    @Deprecated("Use checkAndroidPermissionsGroup instead")
    fun checkAndroidPermissions(
        context: Context,
        call: Call,
    ): Boolean

    /**
     *  @return A [Pair] where:
     *  - `first` indicates whether all required permissions are granted (`true` if granted, `false` otherwise).
     *  - `second` contains a [Set] of missing permissions (empty if all permissions are granted).
     *
     * e.g. if the user granted the android.Manifest.permission.RECORD_AUDIO and the Call.state.ownCapability has "SendAudio"
     * then we are safe to join and use the [call] since the permission for recording audio is granted and the user can stream the audio track.
     */
    fun checkAndroidPermissionsGroup(
        context: Context,
        call: Call,
    ): Pair<Boolean, Set<String>>
}
