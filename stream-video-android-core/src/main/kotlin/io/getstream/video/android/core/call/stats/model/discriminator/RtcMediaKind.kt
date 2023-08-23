package io.getstream.video.android.core.call.stats.model.discriminator

enum class RtcMediaKind(
    private val alias: String,
) {
    AUDIO("audio"),
    VIDEO("video"),
    UNKNOWN("unknown");

    override fun toString(): String = alias

    companion object {
        fun fromAlias(alias: String): RtcMediaKind {
            return RtcMediaKind.values().firstOrNull {
                it.alias == alias
            } ?: UNKNOWN
        }
    }

}