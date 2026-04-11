package dev.sizecraft.player

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SizeEventsTest {

    @Test
    fun `clampSteps returns value unchanged when within per-player bounds`() {
        val data = SizeData(steps = 0.0, minSteps = -2.0, maxSteps = 2.0)
        assertEquals(1.0, SizeEvents.clampSteps(1.0, data), 1e-9)
        assertEquals(-1.0, SizeEvents.clampSteps(-1.0, data), 1e-9)
        assertEquals(0.0, SizeEvents.clampSteps(0.0, data), 1e-9)
    }

    @Test
    fun `clampSteps clamps to max when over per-player bound`() {
        val data = SizeData(steps = 0.0, minSteps = -2.0, maxSteps = 2.0)
        assertEquals(2.0, SizeEvents.clampSteps(5.0, data), 1e-9)
        assertEquals(2.0, SizeEvents.clampSteps(2.0001, data), 1e-9)
    }

    @Test
    fun `clampSteps clamps to min when under per-player bound`() {
        val data = SizeData(steps = 0.0, minSteps = -2.0, maxSteps = 2.0)
        assertEquals(-2.0, SizeEvents.clampSteps(-5.0, data), 1e-9)
        assertEquals(-2.0, SizeEvents.clampSteps(-2.0001, data), 1e-9)
    }

    @Test
    fun `getEffectiveMinSteps returns per-player min when set`() {
        val data = SizeData(minSteps = -1.5)
        assertEquals(-1.5, SizeEvents.getEffectiveMinSteps(data), 1e-9)
    }

    @Test
    fun `getEffectiveMaxSteps returns per-player max when set`() {
        val data = SizeData(maxSteps = 1.5)
        assertEquals(1.5, SizeEvents.getEffectiveMaxSteps(data), 1e-9)
    }
}
