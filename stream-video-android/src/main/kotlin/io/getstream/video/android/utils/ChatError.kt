package io.getstream.video.android.utils

open class ChatError(
    val message: String? = null,
    val cause: Throwable? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        return (other as? ChatError)?.let {
            message == it.message && cause.equalCause(it.cause)
        } ?: false
    }

    private fun Throwable?.equalCause(other: Throwable?): Boolean {
        if ((this == null && other == null) || this === other) return true
        return this?.message == other?.message && this?.cause.equalCause(other?.cause)
    }

    override fun hashCode(): Int {
        var result = message?.hashCode() ?: 0
        result = 31 * result + (cause?.hashCode() ?: 0)
        return result
    }
}