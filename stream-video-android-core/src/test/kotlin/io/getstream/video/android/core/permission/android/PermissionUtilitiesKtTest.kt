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

package io.getstream.video.android.core.permission.android

import android.content.pm.PackageManager
import io.getstream.android.video.generated.models.OwnCapability
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PermissionUtilitiesKtTest {

    @Test
    fun `Correctly map the permissions based on the default mapper`() {
        // Given
        val capabilities = listOf(OwnCapability.SendAudio, OwnCapability.SendVideo)
        // When
        val defaultMapped = capabilities.mapPermissions(defaultPermissionMapper)
        // Then
        assertArrayEquals(
            arrayOf(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.CAMERA),
            defaultMapped.toTypedArray(),
        )
    }

    @Test
    fun `Correctly map the permissions based on the custom mapper`() {
        // Given
        val capabilities = listOf(OwnCapability.SendAudio, OwnCapability.SendVideo)
        val expectedPermission = android.Manifest.permission.ACCEPT_HANDOVER
        // When
        val mapped = capabilities.mapPermissions {
            // All capabilities map to this permission, for testing
            expectedPermission
        }
        // Then
        assertArrayEquals(arrayOf(expectedPermission), mapped.toTypedArray())
    }

    @Test
    fun `Check permission expectation base on default mapper and expectations`() {
        // Given
        val capabilities = listOf(OwnCapability.SendAudio, OwnCapability.SendVideo)
        val permissions = capabilities.mapPermissions(defaultPermissionMapper)

        // When
        val actual = permissions.checkAllPermissions(systemPermissionCheck = {
            // return as if the permission is granted
            PackageManager.PERMISSION_GRANTED
        })

        // Then
        assertTrue(actual.first)
    }

    @Test
    fun `Check permission expectation base on default mapper and expectations when permissions are denied`() {
        // Given
        val capabilities = listOf(OwnCapability.SendAudio, OwnCapability.SendVideo)
        val permissions = capabilities.mapPermissions(defaultPermissionMapper)

        // When
        val actual = permissions.checkAllPermissions(systemPermissionCheck = {
            // return as if the permission is granted
            PackageManager.PERMISSION_DENIED
        })

        // Then
        assertFalse(actual.first)
    }

    @Test
    fun `Check permission expectation base on default mapper and custom expectations`() {
        // Given
        val capabilities = listOf(OwnCapability.SendAudio, OwnCapability.SendVideo)
        val permissions = capabilities.mapPermissions(defaultPermissionMapper)

        // When
        val actual = permissions.checkAllPermissions(systemPermissionCheck = {
            // return as if the permission is granted
            PackageManager.PERMISSION_GRANTED
        }, {
            when (it) {
                // Expect granted permission for record audio
                android.Manifest.permission.RECORD_AUDIO -> PackageManager.PERMISSION_GRANTED
                // Denied for everything else.
                else -> PackageManager.PERMISSION_DENIED
            }
        })

        // Then
        // We expect false because the mock system check returns GRANTED for all permissions,
        // where we expect granted only for the RECORD_AUDIO
        assertFalse(actual.first)
    }
}
