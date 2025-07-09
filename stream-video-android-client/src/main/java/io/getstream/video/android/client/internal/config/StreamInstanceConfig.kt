package io.getstream.video.android.client.internal.config

internal enum class SameIdInstanceBehaviour {
    REPLACE,
    RETURN_EXISTING,
    ERROR,
}

internal class StreamInstanceConfig(
    val numOfInstances: Int = Int.MAX_VALUE,
    val sameIdInstanceBehaviour: SameIdInstanceBehaviour = SameIdInstanceBehaviour.RETURN_EXISTING,
)