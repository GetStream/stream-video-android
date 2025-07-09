package io.getstream.video.android.client.internal.common.algebra

/**
 * A minimal Either type, representing a value of one of two possible types: Left<L> or Right<R>.
 *
 * @param L the type of the Left value (commonly used for errors)
 * @param R the type of the Right value (commonly used for success)
 */
internal sealed class Either<out L, out R> {

    /**
     * Represents the Left case of Either, conventionally used to hold an error value.
     *
     * @param value the value of type L
     */
    data class Left<out L>(val value: L) : Either<L, Nothing>()

    /**
     * Represents the Right case of Either, conventionally used to hold a success value.
     *
     * @param value the value of type R
     */
    data class Right<out R>(val value: R) : Either<Nothing, R>()

    /**
     * Returns true if this is a Right.
     */
    fun isRight(): Boolean = this is Right<R>

    /**
     * Returns true if this is a Left.
     */
    fun isLeft(): Boolean = this is Left<L>

    /**
     * Unwraps the Either by applying [leftOp] if it's Left, or [rightOp] if it's Right.
     *
     * @param leftOp  function to handle the Left value
     * @param rightOp function to handle the Right value
     * @return result of applying the appropriate function
     */
    inline fun <T> foldCatching(leftOp: (L) -> T, rightOp: (R) -> T): Result<T> = runCatching {
        when (this) {
            is Left -> leftOp(value)
            is Right -> rightOp(value)
        }
    }

    /**
     * Transforms the Right value using [transform], leaving Left unchanged.
     *
     * @param transform function to apply to R
     * @return either the original Left or a new Right(transform(value))
     */
    inline fun <R2> mapRight(transform: (R) -> R2): Either<L, R2> =
        when (this) {
            is Left -> this
            is Right -> Right(transform(value))
        }

    inline fun <L2> mapLeft(transform: (L) -> L2): Either<L2, R> =
        when (this) {
            is Left -> Left(transform(value))
            is Right -> this
        }

    companion object {
        /**
         * Creates an Either from a nullable [r]. If [r] is null, produces Left(leftIfNull()),
         * otherwise Right(r).
         *
         * @param r the potentially-null value
         * @param leftIfNull lambda to produce a Left value when r is null
         */
        fun <L, R> fromNullable(r: R?, leftIfNull: () -> L): Either<L, R> =
            if (r == null) Left(leftIfNull()) else Right(r)

        /**
         * Lifts a pure function [fn] so it can operate over an Either.
         * Given fn: (A) -> B, returns a function (Either<L, A>) -> Either<L, B>
         * that applies fn inside Right, or propagates Left.
         *
         * @param fn the function to lift
         */
        fun <L, A, B> lift(fn: (A) -> B): (Either<L, A>) -> Either<L, B> = { either ->
            either.mapRight(fn)
        }
    }
}
