package io.getstream.video.android.utils


public fun <T : Any> Result<T>.stringify(toString: (T) -> String): String = when (this) {
    is Success -> "Success(data=${toString(data)})"
    is Failure -> toString()
}