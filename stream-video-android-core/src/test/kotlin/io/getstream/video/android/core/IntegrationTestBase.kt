package io.getstream.video.android.core

import androidx.test.core.app.ApplicationProvider
import io.getstream.video.android.core.model.User


public class IntegrationTestHelper() {
    val users = mutableMapOf<String, User>()
    val client: StreamVideo
    val builder: StreamVideoBuilder

    init {
        // TODO: generate token from build vars
        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoidGhpZXJyeSJ9._4aZL6BR0VGKfZsKYdscsBm8yKVgG-2LatYeHRJUq0g"

        val thierry = User(
            id="thierry", role="admin", name="Thierry",
            token=token, imageUrl = "hello",
            teams = emptyList(), extraData = mapOf()
        )
        users["thierry"] = thierry
        builder = StreamVideoBuilder(
            context = ApplicationProvider.getApplicationContext(),
            thierry,
            apiKey = "hd8szvscpxvd",
        )
        client = builder.build()
    }
}

open class IntegrationTestBase() {
    val helper = IntegrationTestHelper()

}