package io.getstream.video.android.core

import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.utils.onError
import io.getstream.video.android.core.utils.onSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
public class StreamVideoBuilderTest {

//    @ExperimentalCoroutinesApi
//    val testDispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()

    @Test
    fun build() = runTest {

        assert(true)
        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoidGhpZXJyeSJ9._4aZL6BR0VGKfZsKYdscsBm8yKVgG-2LatYeHRJUq0g"
        val builder = StreamVideoBuilder(
            context = ApplicationProvider.getApplicationContext(),
            user = User(
                id="thierry", role="admin", name="Thierry",
                token=token, imageUrl = "hello",
                teams = emptyList(), extraData = mapOf()
            ),
            apiKey = "hd8szvscpxvd",
        )
        val client = builder.build()

        val result = client.getEdges()

        val result2 = client.getOrCreateCall("develop", "123")
        System.out.println(result2.onError {
            System.out.println("abc")
            System.out.println(it.toString())
        })
        System.out.println(result2.onSuccess {
            System.out.println("123")
            System.out.println(it)
        })
        val result3 = client.joinCall("develop", "123")
        //Log.d("tag" , result.toString())
    }

    @Test
    fun joinCall() {
        assert(true)
    }

}