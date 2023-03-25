package io.getstream.video.android.core

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.getstream.video.android.core.utils.onError
import io.getstream.video.android.core.utils.onSuccess
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

/**
 * TODO: testing work
 *
 * - Livestream
 * - Audio room
 * - Video call
 *
 *
 * - Client intialization
 * - Call CRUD
 *
 */



@RunWith(AndroidJUnit4::class)
public class CreateCallCrudTest: IntegrationTestBase() {
    /**
     * Alright so what do we need to test here:
     *
     * Success scenarios
     * * Create a call with a specific ID
     * * Create a call between 2 participants (no id)
     * * Setting custom fields on a call
     * * Create a ringing call
     *
     * Errors
     * * Try to create a call of a type that doesn't exist
     * * Not having permission to create a call
     *
     * Get Success
     * * Validate basic stats work
     * * Validate pagination works on participants
     *
     */

    @Test
    fun createACall() = runTest {
        val result = helper.client.getOrCreateCall("default", "123")
        assert(result.isSuccess)
    }

    @Test
    fun failToCreateACall() = runTest {

        val result = helper.client.getOrCreateCall("missing", "123")
        assert(result.isFailure)
        result.onError { System.out.println(it) }
    }

}

@RunWith(AndroidJUnit4::class)
public class UpdateOrDeleteCallCrudTest: IntegrationTestBase() {
    /**
     * Alright so what do we need to test here:
     *
     * Success scenarios
     * * Call was successfully updated
     * * Call was successfully deleted
     *
     * Errors
     * * Call doesn't exist
     * * Not having permission to update/delete a call
     */

    @Test
    fun createACall() = runTest {
        val client = helper.client
        val result2 = client.getOrCreateCall("default", "abc")
        // TODO: update call needs to expose more..
        val result3 = client.updateCall("default", "abc", mutableMapOf("color" to "green"))
        result2.onSuccess { it.kind }
        System.out.println(result3.onError {
            System.out.println("abc")
            System.out.println(it.toString())
        })
        System.out.println(result3.onSuccess {
            System.out.println("123")
            System.out.println(it)
        })
    }

}