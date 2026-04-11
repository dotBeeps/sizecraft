package dev.sizecraft.player

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.pow
import kotlin.math.sqrt

class SizeDataTest {

    @Test
    fun `steps 0 gives scale 1`() {
        assertEquals(1.0, SizeData(steps = 0.0).scale, 1e-9)
    }

    @Test
    fun `steps 1 gives scale 6`() {
        assertEquals(6.0, SizeData(steps = 1.0).scale, 1e-9)
    }

    @Test
    fun `steps negative 1 gives scale one-sixth`() {
        assertEquals(1.0 / 6.0, SizeData(steps = -1.0).scale, 1e-9)
    }

    @Test
    fun `steps 0_5 gives scale sqrt-6`() {
        assertEquals(sqrt(6.0), SizeData(steps = 0.5).scale, 1e-9)
    }

    @Test
    fun `steps 3 gives scale 216`() {
        assertEquals(216.0, SizeData(steps = 3.0).scale, 1e-6)
    }

    @Test
    fun `gridTier 0 for steps 0`() {
        assertEquals(0, SizeData(steps = 0.0).gridTier)
    }

    @Test
    fun `gridTier snaps up for positive fractional steps`() {
        assertEquals(1, SizeData(steps = 0.01).gridTier)
        assertEquals(1, SizeData(steps = 0.5).gridTier)
        assertEquals(1, SizeData(steps = 1.0).gridTier)
        assertEquals(2, SizeData(steps = 1.01).gridTier)
        assertEquals(2, SizeData(steps = 1.5).gridTier)
    }

    @Test
    fun `gridTier rounds toward zero for negative fractional steps`() {
        // ceil(-0.5) = 0, ceil(-1.0) = -1, ceil(-0.01) = 0
        assertEquals(0, SizeData(steps = -0.01).gridTier)
        assertEquals(0, SizeData(steps = -0.5).gridTier)
        assertEquals(-1, SizeData(steps = -1.0).gridTier)
        assertEquals(-1, SizeData(steps = -1.5).gridTier)
        assertEquals(-2, SizeData(steps = -2.0).gridTier)
    }

    @Test
    fun `default SizeData has steps 0`() {
        assertEquals(0.0, SizeData().steps, 0.0)
    }

    @Test
    fun `default SizeData has null min and max steps`() {
        assertNull(SizeData().minSteps)
        assertNull(SizeData().maxSteps)
    }
}
