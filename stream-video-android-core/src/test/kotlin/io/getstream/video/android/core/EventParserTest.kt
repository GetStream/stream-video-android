package io.getstream.video.android.core

import com.squareup.moshi.Moshi
import io.getstream.video.android.core.events.VideoEvent
import org.junit.Test
import org.openapitools.client.models.WSEvent
import org.openapitools.client.models.WSEventAdapter

class EventParserTest {

    @Test
    fun runTest() {
        val json = """
             {"type":"connection.ok","created_at":"2023-04-07T16:47:20.06311752Z","connection_id":"642d87c6-0a15-2d42-0000-000000002628","me":{"id":"thierry","name":"Thierry","image":"hello","custom":{},"role":"user","created_at":"2023-02-09T10:45:07.080734Z","updated_at":"2023-04-07T16:47:20.001701Z","devices":[]}}
        """.trimIndent()
        val moshi = Moshi.Builder().add(WSEventAdapter()).build()
        val jsonAdapter = moshi.adapter(WSEvent::class.java)
        val processedEvent = jsonAdapter.fromJson(json)
        println(processedEvent)

    }
}