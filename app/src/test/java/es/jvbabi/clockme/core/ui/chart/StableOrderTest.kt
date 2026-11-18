package es.jvbabi.clockme.core.ui.chart

import org.junit.Assert.assertEquals
import org.junit.Test

class StableOrderTest {

    @Test
    fun withoutLeavingSlicesTheOrderIsTheCurrentOne() {
        assertEquals(listOf(1L, 2L, 3L), stableOrder(listOf(3L, 2L, 1L), listOf(1L, 2L, 3L), emptySet()))
    }

    @Test
    fun aLeavingSliceKeepsItsPlace() {
        assertEquals(listOf(1L, 2L, 3L), stableOrder(listOf(1L, 2L, 3L), listOf(1L, 3L), setOf(1L, 2L, 3L)))
    }

    @Test
    fun aLeavingFirstSliceStaysFirst() {
        assertEquals(listOf(1L, 2L, 3L), stableOrder(listOf(1L, 2L, 3L), listOf(2L, 3L), setOf(1L, 2L, 3L)))
    }

    @Test
    fun severalLeavingSlicesInARowKeepTheirOrder() {
        assertEquals(
            listOf(1L, 2L, 3L, 4L, 5L),
            stableOrder(listOf(1L, 2L, 3L, 4L, 5L), listOf(1L, 5L), setOf(1L, 2L, 3L, 4L, 5L)),
        )
    }

    @Test
    fun aNewSliceComesInAtItsPlaceWhileAnotherLeaves() {
        assertEquals(listOf(1L, 2L, 4L, 3L), stableOrder(listOf(1L, 2L, 3L), listOf(1L, 4L, 3L), setOf(1L, 2L, 3L, 4L)))
    }

    @Test
    fun aSliceThatComesBackTakesItsCurrentPlace() {
        assertEquals(listOf(1L, 2L, 3L), stableOrder(listOf(1L, 3L, 2L), listOf(1L, 2L, 3L), setOf(1L, 2L, 3L)))
    }
}
