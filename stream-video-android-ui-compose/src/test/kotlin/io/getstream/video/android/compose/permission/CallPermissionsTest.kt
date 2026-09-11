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

package io.getstream.video.android.compose.permission

import android.Manifest
import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
internal class CallPermissionsTest {

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `audio calls request bluetooth connect permission on Android 12`() {
        val permissions = getDefaultPermissionList(
            context = RuntimeEnvironment.getApplication(),
            isVideoCall = false,
        )

        assertThat(permissions).containsExactly(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.BLUETOOTH_CONNECT,
        ).inOrder()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `audio calls do not request bluetooth connect permission before Android 12`() {
        val permissions = getDefaultPermissionList(
            context = RuntimeEnvironment.getApplication(),
            isVideoCall = false,
        )

        assertThat(permissions).containsExactly(Manifest.permission.RECORD_AUDIO)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `video calls retain camera and audio permissions`() {
        val permissions = getDefaultPermissionList(
            context = RuntimeEnvironment.getApplication(),
            isVideoCall = true,
        )

        assertThat(permissions).containsExactly(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.BLUETOOTH_CONNECT,
        ).inOrder()
    }
}
