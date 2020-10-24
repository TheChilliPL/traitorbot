package me.patrykanuszczyk.traitorbot.utils

abstract class Result<S, F> {
    //private abstract fun isSuccessful(): Boolean
    abstract val successful: Boolean
    val failed get() = !successful

    val successValue get() = if(this is Success) value else null
    val failValue get() = if(this is Failure) value else null

    inline fun ifSuccess(func: (S) -> Unit): Result<S, F> {
        if(successful) func(this.successValue!!)
        return this
    }

    inline fun ifFailed(func: (F) -> Unit): Result<S, F> {
        if(!successful) func(this.failValue!!)
        return this
    }

    /**
     * Inline function made for convenience.
     *
     * If the result is failure, calls the inlined lambda passing the failed value.
     * This lambda may not return anything.
     *
     * If the result is success, the inlined lambda does not get called,
     * and the success value gets returned from this function instead.
     *
     * @param [func] function that gets called if it's a failure
     *
     * @return success value if it's a success;
     *
     * otherwise does not return
     */
    inline fun fail(func: (F) -> Nothing): S {
        if(failed) func(this.failValue!!)
        return this.successValue!!
    }

    class Success<S, F>(val value: S): Result<S, F>() {
        override val successful: Boolean
            get() = true
    }

    class Failure<S, F>(val value: F): Result<S, F>() {
        override val successful: Boolean
            get() = false
    }
}