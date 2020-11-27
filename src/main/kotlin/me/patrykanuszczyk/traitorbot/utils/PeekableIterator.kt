package me.patrykanuszczyk.traitorbot.utils

interface PeekableIterator<out T> : Iterator<T> {
    override fun next(): T
    fun peek(): T

    override operator fun hasNext(): Boolean

    companion object {
        operator fun <T> invoke(iterable: Iterable<T>): PeekableIterator<T> {
            return PeekableWrapperIterator(iterable.iterator())
        }

        operator fun <T> invoke(sequence: Sequence<T>): PeekableIterator<T> {
            return PeekableWrapperIterator(sequence.iterator())
        }

        fun <T> Iterable<T>.peekableIterator() = PeekableIterator(this)
        fun <T> Sequence<T>.peekableIterator() = PeekableIterator(this)
    }
}


class PeekableWrapperIterator<out T> internal constructor(private val innerIterator: Iterator<T>) : PeekableIterator<T> {
    private var has: Boolean = innerIterator.hasNext()
    private var next: T? = if(has) innerIterator.next() else null

    override fun next(): T {
        val peek = peek()
        has = innerIterator.hasNext()
        if (has) next = innerIterator.next()
        return peek
    }

    override fun peek() = if (has) next.asType<T>() else throw NoSuchElementException()

    override fun hasNext() = has
}