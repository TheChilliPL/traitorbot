package me.patrykanuszczyk.traitorbot.tests.utils

import me.patrykanuszczyk.traitorbot.utils.addAll
import me.patrykanuszczyk.traitorbot.utils.addUnique
import me.patrykanuszczyk.traitorbot.utils.normalize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class CollectionsUtils {
    @ParameterizedTest(name = "{0} to {1}")
    @MethodSource
    fun normalize(numbers: List<Int>, toMax: Int, expect: List<Int>) {
        assertEquals(numbers.normalize(toMax), expect)
    }

    @Test
    fun addAll() {
        val list = mutableListOf(1,2,3)
        assertTrue(list.addAll(4,5,6))
        assertEquals(
            list,
            (1..6).toList()
        )
    }

    @ParameterizedTest(name = "{0} + {1}")
    @MethodSource
    fun addUnique(initial: MutableList<Int>, add: Int, expected: List<Int>) {
        initial.addUnique(add)
        assertEquals(initial, expected)
    }

    @Suppress("unused")
    companion object {
        @JvmStatic
        private fun normalize(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(List(10) { 0 }, 100, List(10) { 0 }),
                Arguments.of(listOf(1), 10, listOf(10)),
                Arguments.of((0..10).toList(), 100, (0..100).step(10).toList())
            )
        }

        @JvmStatic
        private fun addUnique(): Stream<Arguments> {
            return Stream.of(
                Arguments.of((1..3).toMutableList(), 4, (1..4).toMutableList()),
                Arguments.of((1..5).toMutableList(), 4, (1..5).toMutableList())
            )
        }
    }
}