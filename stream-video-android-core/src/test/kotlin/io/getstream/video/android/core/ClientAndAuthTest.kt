package io.getstream.video.android.core

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ClientAndAuthTest: IntegrationTestBase() {
    /**
     * So what do we need to test on the client..
     *
     * Auth User types
     * - Normal user, guest user, not-authenticated
     *
     * Joining call auth
     * - Token auth
     *
     * Token refreshing
     * - Tokens can expire, so there needs to be a token refresh handler
     *
     * Client setup
     * ** Geofencing policy (also a good moment to show our edge network)
     *
     * StreamVideoConfig. I'm not sure any of this belongs here.
     * This should be on the call type....
     *
     * Things I didn't expect on the client
     * ** AndroidInputs (not sure what it does)
     * ** InputLauncher (again unsure)
     * ** PushDevice Generators..
     *
     * Missing..
     * * Filters
     *
     * Client setup errors
     * ** Invalid API key
     *
    */
    @Test
    fun clientBuilder() = runTest {
        val builder = StreamVideoBuilder(
            context = ApplicationProvider.getApplicationContext(),
            helper.users["thierry"]!!,
            apiKey = "hd8szvscpxvd",
        )
        val client = builder.build()
    }
}