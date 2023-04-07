package io.getstream.video.android.core

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.getstream.video.android.core.events.VideoEvent
import org.junit.Test
import org.openapitools.client.infrastructure.*
import org.openapitools.client.models.WSConnectedEvent
import org.openapitools.client.models.WSEvent
import org.openapitools.client.models.WSEventAdapter

class EventParserTest {

    @Test
    fun runTest() {
        val moshiBuilder: Moshi.Builder = Moshi.Builder()
            .add(OffsetDateTimeAdapter())
            .add(LocalDateTimeAdapter())
            .add(LocalDateAdapter())
            .add(UUIDAdapter())
            .add(ByteArrayAdapter())
            .add(URIAdapter())
            .add(BigDecimalAdapter())
            .add(WSEventAdapter())
            .add(BigIntegerAdapter())
            .addLast(KotlinJsonAdapterFactory())
        val moshi = moshiBuilder.build()

        val json = """{"type":"connection.ok","created_at":"2023-04-07T16:47:20.06311752Z","connection_id":"642d87c6-0a15-2d42-0000-000000002628","me":{"id":"thierry","name":"Thierry","image":"hello","custom":{},"role":"user","created_at":"2023-02-09T10:45:07.080734Z","updated_at":"2023-04-07T16:47:20.001701Z","devices":[]}}""".trimIndent()

        val jsonAdapter: JsonAdapter<WSEvent> = moshi.adapter(WSEvent::class.java)
        val jsonAdapterConnected: JsonAdapter<WSConnectedEvent> = moshi.adapter(WSConnectedEvent::class.java)

        // verify we can parse the underlying subclass
        val processedEvent = jsonAdapterConnected.fromJson(json)
        assertThat(processedEvent).isNotNull()
        assertThat(processedEvent?.me?.id).isEqualTo("thierry")

        // now go for the harder one
        val processedEvent2 = jsonAdapter.fromJson(json)
        val connected = processedEvent2 as WSConnectedEvent
        assertThat(processedEvent2).isNotNull()
        assertThat(processedEvent2?.me?.id).isEqualTo("thierry")

    }
}