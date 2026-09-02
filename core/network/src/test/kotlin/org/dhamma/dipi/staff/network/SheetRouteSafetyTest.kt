package org.dhamma.dipi.staff.network

import org.dhamma.dipi.staff.model.SheetExport
import org.dhamma.dipi.staff.model.SheetSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.Query
import retrofit2.http.QueryMap
import retrofit2.http.QueryName

/**
 * The `r` guard, as a test rather than a comment.
 *
 * `GET /seating|group-seating|cell-list/{cid}/{courseId}?r=1` runs **bulk
 * seat auto-allocation server-side** (`inc/zero-day.inc:17-46`): the mere
 * presence of the parameter silently reshuffles every student's seat, and
 * the backend is immutable so we cannot make it safe. The app's defence is
 * that the parameter surface on a sheet GET is a closed set of two names.
 *
 * This test reflects over the real Retrofit interface rather than trusting
 * a comment, so widening the surface — a third `@Query`, a `@QueryMap`, a
 * `@QueryName` — fails the build instead of shipping.
 *
 * If you are here because this test went red: you almost certainly do not
 * want to relax it. Get a HAR line proving the new parameter is inert on
 * every sheet slug, then add it to [SheetSort.ALLOWED_QUERY_NAMES] and to
 * the expectation below in the same commit.
 */
class SheetRouteSafetyTest {

    /** The methods that can reach a `/{sheet}/{cid}/{courseId}` path. */
    private val sheetMethods = listOf("sheetPage", "sheetDocument")

    @Test
    fun noSheetGetCanCarryAnRParam() {
        val methods = StaffApi::class.java.methods.filter { it.name in sheetMethods }
        assertTrue("expected to find $sheetMethods on StaffApi", methods.size >= sheetMethods.size)

        val queryNames = mutableSetOf<String>()
        methods.forEach { method ->
            method.parameterAnnotations.forEach { annotations ->
                annotations.forEach { annotation ->
                    when (annotation) {
                        is Query -> queryNames += annotation.value
                        is QueryMap, is QueryName -> throw AssertionError(
                            "${method.name} declares ${annotation.annotationClass.simpleName}: a " +
                                "free-form query surface on a sheet GET is one typo away from " +
                                "?r=1 and server-side bulk seat auto-allocation. Use named, " +
                                "nullable @Query parameters from SheetSort.ALLOWED_QUERY_NAMES.",
                        )
                    }
                }
            }
        }

        assertTrue(
            "a sheet GET must never be able to send `r`: found $queryNames",
            "r" !in queryNames,
        )
        assertEquals(
            "the sheet-GET query allowlist is conf + seating and nothing else",
            SheetSort.ALLOWED_QUERY_NAMES,
            queryNames,
        )
    }

    @Test
    fun theAllowlistIsExactlyConfAndSeating() {
        assertEquals(setOf("conf", "seating"), SheetSort.ALLOWED_QUERY_NAMES)
        // Every non-default sort must map onto an allowlisted name — a sort
        // with an unlisted queryName could never be sent, and a sort whose
        // name is not in the allowlist means the two drifted apart.
        SheetSort.entries.filter { it != SheetSort.Default }.forEach { sort ->
            assertTrue(
                "${sort.name} sends ?${sort.queryName}= which is not allowlisted",
                sort.queryName in SheetSort.ALLOWED_QUERY_NAMES,
            )
        }
        assertEquals("", SheetSort.Default.queryName)
    }

    /**
     * The sort options an export offers must be ones the desk actually
     * exposes. HAR-ROUTES.md § "Query params seen" is the source: `conf` on
     * `day0-list`, `seating` on `teacher-list` and `student-chit`. Everything
     * else is default-only — including the seating plan, whose only
     * parameter on the live desk is the forbidden `r`.
     */
    @Test
    fun onlyTheThreeVerifiedSheetsOfferAnAlternateOrder() {
        val withOptions = SheetExport.entries
            .filter { SheetSort.optionsFor(it).size > 1 }
            .toSet()
        assertEquals(
            setOf(SheetExport.Day0List, SheetExport.TeacherList, SheetExport.StudentChit),
            withOptions,
        )
        assertEquals(
            listOf(SheetSort.Default, SheetSort.ConfirmationNo),
            SheetSort.optionsFor(SheetExport.Day0List),
        )
        assertEquals(
            listOf(SheetSort.Default, SheetSort.SeatingOrder),
            SheetSort.optionsFor(SheetExport.TeacherList),
        )
        assertEquals(
            listOf(SheetSort.Default, SheetSort.SeatingOrder),
            SheetSort.optionsFor(SheetExport.StudentChit),
        )
        assertEquals(listOf(SheetSort.Default), SheetSort.optionsFor(SheetExport.SeatingPlan))
    }
}
