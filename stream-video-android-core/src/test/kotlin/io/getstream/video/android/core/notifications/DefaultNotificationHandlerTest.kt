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
import io.getstream.android.push.permissions.NotificationPermissionHandler
import io.getstream.video.android.model.StreamCallId
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Simplified tests for the legacy DefaultNotificationHandler class.
 * Even though it's deprecated, we test basic functionality and interface compliance.
 * Complex integration tests are avoided due to the legacy nature and extensive mocking requirements.
 */
class DefaultNotificationHandlerTest {

    @MockK
    lateinit var mockApplication: Application

    @MockK
    lateinit var mockNotificationPermissionHandler: NotificationPermissionHandler

    private lateinit var testCallId: StreamCallId

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        testCallId = StreamCallId(type = "default", id = "test-call-123")
    }

    @Test
    fun `constructor initializes with correct default values`() {
        // When
        val handler = DefaultNotificationHandler(mockApplication)

        // Then
        assertNotNull(handler)
        assertFalse(handler.hideRingingNotificationInForeground)
    }

    @Test
    fun `constructor initializes with custom hideRingingNotificationInForeground value`() {
        // When
        val handler = DefaultNotificationHandler(
            application = mockApplication,
            hideRingingNotificationInForeground = true
        )

        // Then
        assertTrue(handler.hideRingingNotificationInForeground)
    }

    @Test
    fun `constructor initializes with custom notification permission handler`() {
        // When
        val handler = DefaultNotificationHandler(
            application = mockApplication,
            notificationPermissionHandler = mockNotificationPermissionHandler,
            hideRingingNotificationInForeground = false
        )

        // Then
        assertNotNull(handler)
        assertFalse(handler.hideRingingNotificationInForeground)
    }

    @Test
    fun `notification handler implements NotificationPermissionHandler interface`() {
        // Given
        val handler = DefaultNotificationHandler(mockApplication)

        // Then
        assertTrue("DefaultNotificationHandler should implement NotificationPermissionHandler", 
                  handler is NotificationPermissionHandler)
    }

    @Test
    fun `notification handler is deprecated but still functional`() {
        // Given - This test documents that the class is deprecated but still works
        @Suppress("DEPRECATION")
        val handler = DefaultNotificationHandler(mockApplication)

        // Then
        assertNotNull("Deprecated DefaultNotificationHandler should still be instantiable", handler)
        assertTrue("Should implement the permission handler interface", 
                  handler is NotificationPermissionHandler)
    }

    @Test
    fun `hideRingingNotificationInForeground property is accessible`() {
        // Given
        val handlerWithHidden = DefaultNotificationHandler(
            application = mockApplication,
            hideRingingNotificationInForeground = true
        )
        val handlerWithoutHidden = DefaultNotificationHandler(
            application = mockApplication,
            hideRingingNotificationInForeground = false
        )

        // Then
        assertTrue("Handler with hidden notifications should have property set to true", 
                  handlerWithHidden.hideRingingNotificationInForeground)
        assertFalse("Handler without hidden notifications should have property set to false", 
                   handlerWithoutHidden.hideRingingNotificationInForeground)
    }

    @Test
    fun `notification handler can be created with minimal parameters`() {
        // When - Create with just the required application parameter
        val handler = DefaultNotificationHandler(mockApplication)

        // Then
        assertNotNull("Handler should be created successfully", handler)
        assertFalse("Default hideRingingNotificationInForeground should be false", 
                   handler.hideRingingNotificationInForeground)
    }

    @Test
    fun `notification handler maintains interface compatibility`() {
        // Given
        val handler = DefaultNotificationHandler(mockApplication)

        // When - Cast to interface
        val permissionHandler: NotificationPermissionHandler = handler

        // Then
        assertNotNull("Should be castable to NotificationPermissionHandler", permissionHandler)
        assertTrue("Should maintain instance relationship", permissionHandler === handler)
    }

    @Test
    fun `different constructor overloads work correctly`() {
        // Given - Test all constructor variations
        val handler1 = DefaultNotificationHandler(mockApplication)
        val handler2 = DefaultNotificationHandler(
            application = mockApplication,
            hideRingingNotificationInForeground = true
        )
        val handler3 = DefaultNotificationHandler(
            application = mockApplication,
            notificationPermissionHandler = mockNotificationPermissionHandler
        )
        val handler4 = DefaultNotificationHandler(
            application = mockApplication,
            notificationPermissionHandler = mockNotificationPermissionHandler,
            hideRingingNotificationInForeground = true
        )

        // Then - All should be created successfully
        assertNotNull("Handler1 should be created", handler1)
        assertNotNull("Handler2 should be created", handler2)
        assertNotNull("Handler3 should be created", handler3)
        assertNotNull("Handler4 should be created", handler4)

        // Verify properties
        assertFalse("Handler1 should have default hideRingingNotificationInForeground", 
                   handler1.hideRingingNotificationInForeground)
        assertTrue("Handler2 should have hideRingingNotificationInForeground set to true", 
                  handler2.hideRingingNotificationInForeground)
        assertFalse("Handler3 should have default hideRingingNotificationInForeground", 
                   handler3.hideRingingNotificationInForeground)
        assertTrue("Handler4 should have hideRingingNotificationInForeground set to true", 
                  handler4.hideRingingNotificationInForeground)
    }
}
