package org.dhamma.dipi.staff.desktop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DesktopConfigTest {
    @Test
    fun defaultsToLiveHost() {
        val c = DesktopConfig.fromArgs(emptyArray())
        assertEquals(DesktopConfig.DEFAULT_BASE_URL, c.baseUrl)
        assertFalse(c.useMock)
        assertEquals(1280, DesktopConfig.DECK_WIDTH)
        assertEquals(800, DesktopConfig.DECK_HEIGHT)
    }

    @Test
    fun parsesMockAndBaseUrl() {
        val c = DesktopConfig.fromArgs(arrayOf("--mock", "--base-url", "https://example.test", "--data-dir", "/tmp/dipi-test"))
        assertTrue(c.useMock)
        assertEquals("https://example.test", c.baseUrl)
        assertEquals(File("/tmp/dipi-test"), c.dataDir)
    }

    @Test
    fun fullscreenFlags() {
        assertTrue(DesktopConfig.fromArgs(arrayOf("--deck")).deckFullscreen)
        assertFalse(DesktopConfig.fromArgs(arrayOf("--deck", "--windowed")).deckFullscreen)
    }
}
