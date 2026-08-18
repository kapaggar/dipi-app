package org.dhamma.dipi.staff.desktop

import java.io.File

/**
 * Linux / Steam Deck runtime config. Login has no URL field — the live
 * host is fixed unless a debug override is supplied on the command line
 * or via `DIPI_BASE_URL`.
 */
data class DesktopConfig(
    val baseUrl: String = DEFAULT_BASE_URL,
    val useMock: Boolean = false,
    val dataDir: File = defaultDataDir(),
    val deckFullscreen: Boolean = false,
    val versionName: String = VERSION_NAME,
) {
    val sheetsDir: File get() = File(dataDir, "cache/sheets")

    companion object {
        const val VERSION_NAME = "2.0.0"
        const val DEFAULT_BASE_URL = "https://dipi.vridhamma.org"
        const val USER_AGENT = "DIPI-Staff/2.0 (Linux; Steam Deck OLED; registrar desk)"

        /** Steam Deck OLED native panel. */
        const val DECK_WIDTH = 1280
        const val DECK_HEIGHT = 800

        fun defaultDataDir(): File {
            val xdg = System.getenv("XDG_DATA_HOME")
            val root = if (!xdg.isNullOrBlank()) {
                File(xdg)
            } else {
                File(System.getProperty("user.home"), ".local/share")
            }
            return File(root, "dipi-staff")
        }

        fun fromArgs(args: Array<String>): DesktopConfig {
            var base = System.getenv("DIPI_BASE_URL")?.trim()?.ifBlank { null } ?: DEFAULT_BASE_URL
            var mock = System.getenv("DIPI_USE_MOCK").equals("true", ignoreCase = true)
            var data = System.getenv("DIPI_DATA_DIR")?.let { File(it) } ?: defaultDataDir()
            var fullscreen = System.getenv("DIPI_FULLSCREEN").equals("true", ignoreCase = true) ||
                System.getenv("SteamDeck") == "1"
            val it = args.iterator()
            while (it.hasNext()) {
                when (val a = it.next()) {
                    "--mock" -> mock = true
                    "--fullscreen", "--deck" -> fullscreen = true
                    "--windowed" -> fullscreen = false
                    "--base-url" -> if (it.hasNext()) base = it.next().trimEnd('/')
                    "--data-dir" -> if (it.hasNext()) data = File(it.next())
                    else -> if (a.startsWith("--base-url=")) base = a.substringAfter("=").trimEnd('/')
                }
            }
            return DesktopConfig(
                baseUrl = base.trimEnd('/'),
                useMock = mock,
                dataDir = data,
                deckFullscreen = fullscreen,
            )
        }
    }
}
