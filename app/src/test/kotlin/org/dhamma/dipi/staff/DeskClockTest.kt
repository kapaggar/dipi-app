package org.dhamma.dipi.staff

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.dhamma.dipi.staff.ui.deskClockTicks
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeskClockTest {
    @Test fun clockStopsForScreenOffAndBackgroundThenResumesAtCurrentTime() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val owner = object : LifecycleOwner {
                override val lifecycle = LifecycleRegistry.createUnsafe(this)
            }
            owner.lifecycle.currentState = Lifecycle.State.STARTED
            val interactive = MutableStateFlow(true)
            val ticks = mutableListOf<Long>()
            var clockReads = 0
            val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                deskClockTicks(owner.lifecycle, interactive) {
                    clockReads++
                    testScheduler.currentTime
                }.collect { ticks += it }
            }
            runCurrent()
            assertEquals(listOf(0L), ticks)
            advanceTimeBy(60_000); runCurrent()
            assertEquals(listOf(0L, 60_000L), ticks)

            interactive.value = false
            runCurrent()
            val offReads = clockReads
            advanceTimeBy(120_000); runCurrent()
            assertEquals("screen-off must not even read the clock", offReads, clockReads)
            assertEquals(2, ticks.size)
            interactive.value = true
            runCurrent()
            assertEquals(180_000L, ticks.last())

            owner.lifecycle.currentState = Lifecycle.State.CREATED
            runCurrent()
            val stoppedReads = clockReads
            assertEquals("screen receiver upstream must be unsubscribed", 0, interactive.subscriptionCount.value)
            advanceTimeBy(125_000); runCurrent()
            assertEquals(stoppedReads, clockReads)
            owner.lifecycle.currentState = Lifecycle.State.STARTED
            runCurrent()
            assertEquals(305_000L, ticks.last())
            advanceTimeBy(55_000); runCurrent()
            assertEquals(360_000L, ticks.last())

            job.cancel()
            runCurrent()
            assertEquals(0, interactive.subscriptionCount.value)
            owner.lifecycle.currentState = Lifecycle.State.DESTROYED
        } finally {
            Dispatchers.resetMain()
        }
    }
}
