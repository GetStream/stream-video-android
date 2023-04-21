package io.getstream.video.android.core

import com.google.common.truth.Truth
import io.getstream.log.taggedLogger
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * A little example for our customer success team of how to replicate
 * problems when customers report them
 */
class SupportTest: IntegrationTestBase(connectCoordinatorWS = false) {
    private val logger by taggedLogger("Test:SupportTest")

    @Test
    fun customerTest() = runTest {
        // little demo of creating a call
        val call = client.call("default", randomUUID())
        val result = call.create()
        assertSuccess(result)

    }
}