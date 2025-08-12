package com.twilio.audioswitch.wired

import android.content.Context
import android.content.Intent
import com.twilio.audioswitch.android.Logger
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.isA
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class WiredHeadsetReceiverTest {

    private val context = mock<Context>()
    private val logger = mock<Logger>()
    private val wiredDeviceConnectionListener = mock<WiredDeviceConnectionListener>()
    private val wiredHeadsetReceiver = WiredHeadsetReceiver(
        context,
        logger,
    )

    @Test
    fun `onReceive should notify listener when a wired headset has been plugged in`() {
        val intent = mock<Intent> {
            whenever(mock.getIntExtra("state", STATE_UNPLUGGED))
                .thenReturn(STATE_PLUGGED)
        }
        wiredHeadsetReceiver.start(wiredDeviceConnectionListener)

        wiredHeadsetReceiver.onReceive(context, intent)

        verify(wiredDeviceConnectionListener).onDeviceConnected()
    }

    @Test
    fun `onReceive should not notify listener when a wired headset has been plugged in but the listener is null`() {
        wiredHeadsetReceiver.deviceListener = null
        val intent = mock<Intent> {
            whenever(mock.getIntExtra("state", STATE_UNPLUGGED))
                .thenReturn(STATE_PLUGGED)
        }

        try {
            wiredHeadsetReceiver.onReceive(context, intent)
        } catch (e: NullPointerException) {
            fail("NullPointerException should not have been thrown")
        }
    }

    @Test
    fun `onReceive should notify listener when a wired headset has been unplugged`() {
        val intent = mock<Intent> {
            whenever(mock.getIntExtra("state", STATE_UNPLUGGED))
                .thenReturn(STATE_UNPLUGGED)
        }
        wiredHeadsetReceiver.start(wiredDeviceConnectionListener)

        wiredHeadsetReceiver.onReceive(context, intent)

        verify(wiredDeviceConnectionListener).onDeviceDisconnected()
    }

    @Test
    fun `onReceive should not notify listener when a wired headset has been unplugged but the listener is null`() {
        wiredHeadsetReceiver.deviceListener = null
        val intent = mock<Intent> {
            whenever(mock.getIntExtra("state", STATE_UNPLUGGED))
                .thenReturn(STATE_UNPLUGGED)
        }

        try {
            wiredHeadsetReceiver.onReceive(context, intent)
        } catch (e: NullPointerException) {
            fail("NullPointerException should not have been thrown")
        }
    }

    @Test
    fun `start should register the device listener`() {
        wiredHeadsetReceiver.start(wiredDeviceConnectionListener)

        assertThat(wiredHeadsetReceiver.deviceListener, equalTo(wiredDeviceConnectionListener))
    }

    @Test
    fun `start should register the broadcast receiver`() {
        wiredHeadsetReceiver.start(wiredDeviceConnectionListener)

        verify(context).registerReceiver(eq(wiredHeadsetReceiver), isA())
    }

    @Test
    fun `stop should close resources successfully`() {
        wiredHeadsetReceiver.start(wiredDeviceConnectionListener)

        wiredHeadsetReceiver.stop()

        assertThat(wiredHeadsetReceiver.deviceListener, `is`(nullValue()))
        verify(context).unregisterReceiver(wiredHeadsetReceiver)
    }
}
