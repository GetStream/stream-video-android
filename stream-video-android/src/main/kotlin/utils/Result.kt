package utils

/**
 *  A class which encapsulates a successful outcome with a value of type [T] or a failure with [ChatError].
 */
class Result<T : Any> private constructor(
    private val data: T?,
    private val error: ChatError?,
) {

    @Suppress("DEPRECATION")
    constructor(data: T) : this(data, null)

    @Suppress("DEPRECATION")
    constructor(error: ChatError) : this(null, error)

    /**
     * Returns true if a request of payload response has been successful.
     */
    val isSuccess: Boolean
        get() = data != null

    /**
     * Returns true if a request of payload response has been failed.
     */
    val isError: Boolean
        get() = error != null

    /**
     * Returns the successful data payload.
     */
    fun data(): T {
        return checkNotNull(data) { "Result is not successful. Check result.isSuccess before reading the data." }
    }

    /**
     * Returns the [ChatError] error payload.
     */
    fun error(): ChatError {
        return checkNotNull(error) {
            "Result is successful, not an error. Check result.isSuccess before reading the error."
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Result<*>

        if (data != other.data) return false
        if (error != other.error) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data?.hashCode() ?: 0
        result = 31 * result + (error?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "Result(data=$data, error=$error)"
    }

    companion object {

        /**
         * Creates a [Result] object with [data] payload.
         *
         * @param data successful data payload.
         *
         * @return [Result] of [T] that contains successful data payload.
         */
        @JvmStatic
        fun <T : Any> success(data: T): Result<T> {
            return Result(data)
        }

        /**
         * Creates a [Result] object with error payload.
         *
         * @param t Unexpected [Exception] or [Throwable].
         *
         * @return [Result] of [T] that contains [ChatError] error payload.
         */
        @JvmStatic
        fun <T : Any> error(t: Throwable): Result<T> {
            return Result(null, ChatError(t.message, t))
        }

        /**
         * Creates a [Result] object with error payload.
         *
         * @param error [ChatError] error payload.
         *
         * @return [Result] of [T] that contains [ChatError] error payload.
         */
        @JvmStatic
        fun <T : Any> error(error: ChatError): Result<T> {
            return Result(null, error)
        }
    }
}

/**
 * Runs the [successSideEffect] lambda function if the [Result] contains a successful data payload.
 *
 * @param successSideEffect A lambda that receives the successful data payload.
 *
 * @return The original instance of the [Result].
 */
@JvmSynthetic
inline fun <T : Any> Result<T>.onSuccess(
    crossinline successSideEffect: (T) -> Unit,
): Result<T> = apply {
    if (isSuccess) {
        successSideEffect(data())
    }
}

/**
 * Runs the suspending [successSideEffect] lambda function if the [Result] contains a successful data payload.
 *
 * @param successSideEffect A suspending lambda that receives the successful data payload.
 *
 * @return The original instance of the [Result].
 */
@JvmSynthetic
suspend inline fun <T : Any> Result<T>.onSuccessSuspend(
    crossinline successSideEffect: suspend (T) -> Unit,
): Result<T> = apply {
    if (isSuccess) {
        successSideEffect(data())
    }
}

/**
 * Runs the [errorSideEffect] lambda function if the [Result] contains an error payload.
 *
 * @param errorSideEffect A lambda that receives the [ChatError] payload.
 *
 * @return The original instance of the [Result].
 */
@JvmSynthetic
inline fun <T : Any> Result<T>.onError(
    crossinline errorSideEffect: (ChatError) -> Unit,
): Result<T> = apply {
    if (isError) {
        errorSideEffect(error())
    }
}

/**
 * Runs the suspending [errorSideEffect] lambda function if the [Result] contains an error payload.
 *
 * @param errorSideEffect A suspending lambda that receives the [ChatError] payload.
 *
 * @return The original instance of the [Result].
 */
@JvmSynthetic
suspend inline fun <T : Any> Result<T>.onErrorSuspend(
    crossinline errorSideEffect: suspend (ChatError) -> Unit,
): Result<T> = apply {
    if (isError) {
        errorSideEffect(error())
    }
}

/**
 * Returns a [Result] that contains an instance of [T] as a data payload.
 *
 * @return A [Result] the contains an instance of [T] as a data payload.
 */
fun <T : Any> T.toResult(): Result<T> = Result.success(this)

/**
 * Returns a [Result] of type [T] that contains an they same error as payload.
 *
 * @return A [Result] of type [T] that contains an they same error as payload.
 */
fun <T : Any> ChatError.toResultError(): Result<T> = Result.error(this)
