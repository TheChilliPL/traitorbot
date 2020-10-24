package me.patrykanuszczyk.traitorbot.utils

abstract class Result<S, F> {
    //private abstract fun isSuccessful(): Boolean
    abstract val successful: Boolean
    val failed get() = !successful

    val successValue get() = if(this is Success) value else null
    val failValue get() = if(this is Failure) value else null

    inline fun ifSuccess(func: (Success<S, F>) -> Unit): Result<S, F> {
        if(successful) func(this as Success<S, F>)
        return this
    }

    inline fun ifFailed(func: (Failure<S, F>) -> Unit): Result<S, F> {
        if(!successful) func(this as Failure<S, F>)
        return this
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