package org.dhamma.dipi.staff.desktop.data

import kotlinx.coroutines.runBlocking
import org.dhamma.dipi.staff.desktop.DesktopConfig
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.network.MockFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DesktopRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun repo(): DesktopRepository {
        val dir = tmp.newFolder()
        val config = DesktopConfig(
            baseUrl = DesktopConfig.DEFAULT_BASE_URL,
            useMock = true,
            dataDir = dir,
        )
        val store = DesktopStore(dir)
        val clients = DesktopNetwork.create(config, store)
        return DesktopRepository(config, store, clients)
    }

    @Test
    fun mockLoginLoadsSudhaCentreAndWorklist() = runBlocking {
        val repo = repo()
        val session = repo.login("sudha.user", "ok")
        assertEquals("sudha.user", session.name)
        assertTrue(session.centres.any { it.name.contains("Sudha") })
        val courses = repo.loadCourses(session.centres.first().id)
        assertTrue(courses.upcoming.isNotEmpty())
        val (rows, counts) = repo.refreshApplicants(CourseId(MockFixtures.COURSE_10D), centreId = session.centres.first().id)
        assertTrue(rows.isNotEmpty())
        assertTrue((counts["All"] ?: 0) > 0)
        assertTrue(rows.any { it.displayName.contains("Meera") })
        assertTrue(rows.none { "aadhaar" in it.displayName.lowercase() })
    }

    @Test
    fun neverSendsApproved() = runBlocking {
        val repo = repo()
        repo.login("sudha.user", "ok")
        val snack = repo.changeStatus(ApplicantId(MockFixtures.MEERA_ID), "Approved", "", offline = false)
        assertTrue(snack.error)
        assertTrue(snack.text.contains("never sends Approved"))
    }

    @Test
    fun badPasswordSurfacesVerbatim() = runBlocking {
        val repo = repo()
        val err = runCatching { repo.login("sudha.user", "bad") }.exceptionOrNull()
        assertTrue(err is ApiException)
        assertTrue(err!!.message.orEmpty().contains("unrecognized", ignoreCase = true))
    }

    @Test
    fun factoryResetWipesWorklist() = runBlocking {
        val repo = repo()
        repo.login("sudha.user", "ok")
        repo.refreshApplicants(CourseId(MockFixtures.COURSE_10D))
        assertTrue(repo.cachedApplicants().isNotEmpty())
        repo.factoryReset()
        assertTrue(repo.cachedApplicants().isEmpty())
    }
}
