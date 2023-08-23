package io.getstream.video.android.core.call.stats.model.discriminator

enum class RtcQualityLimitationReason(
    private val alias: String,
) {

    NONE("none"),
    CPU("cpu"),
    BANDWIDTH("bandwidth"),
    OTHER("other"),
    UNKNOWN("unknown");

    override fun toString(): String = alias

    companion object {
        fun fromAlias(alias: String): RtcQualityLimitationReason {
            return RtcQualityLimitationReason.values().firstOrNull {
                it.alias == alias
            } ?: UNKNOWN
        }
    }
}