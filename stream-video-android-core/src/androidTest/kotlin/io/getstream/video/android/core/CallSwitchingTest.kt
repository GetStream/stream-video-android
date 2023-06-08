package io.getstream.video.android.core

import com.google.common.truth.Truth
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.utils.Timer
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Test

class CallSwitchingTest : IntegrationTestBase(connectCoordinatorWS = false) {

    private val logger by taggedLogger("Test:AndroidDeviceTest")

    @Test
    fun switch() = runTest {


        val numberOfCalls = 10
        // create 3 calls
        val calls = (0 until numberOfCalls).map {
            val call = client.call("audio_room", "switch-test-$it")
            val result = call.create(custom = mapOf("switchtest" to "1"))
            assertSuccess(result)
            call
        }

        // loop over the calls, join them and leave
        val t = Timer("switch")
        (0 until numberOfCalls).map {
            val call = client.call("audio_room", "switch-test-$it")
            val result = call.join()
            assertSuccess(result)
            assertSuccess(result)
            t.split("iteration $it")
            call.leave()
        }
        t.finish()

        t?.let {
            logger.i { "${it.name} took ${it.duration}" }
            it.durations.forEach { (s, t) ->
                logger.i { " - ${it.name}:$s took $t" }
            }
        }

    }


}