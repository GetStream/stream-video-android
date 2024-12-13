package io.getstream.video.android.data.state

/**
 * Global state for codec choice.
 *
 * Note: Temporary solution.
 */
object GlobalCodecChoiceState {

    /**
     * Preferred codec for publishing.
     */
    var preferredPublishCodec: String? = null

    /**
     * Preferred codec for subscribing.
     */
    var preferredSubscribeCodec: String? = null

    /**
     * Clear the state.
     */
    fun clear() {
        preferredPublishCodec = null
        preferredSubscribeCodec = null
    }
}