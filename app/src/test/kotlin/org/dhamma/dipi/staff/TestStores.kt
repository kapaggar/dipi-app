package org.dhamma.dipi.staff

import android.content.Context
import org.dhamma.dipi.staff.datastore.CourseOpsStore
import org.robolectric.RuntimeEnvironment

/**
 * Course-ops PIN store over a plain prefs file: Robolectric has no keystore,
 * so tests use the store's prefs seam (the SessionStore lazy-`secure` pattern).
 */
fun testCourseOpsStore(name: String = "test_course_ops"): CourseOpsStore =
    CourseOpsStore {
        RuntimeEnvironment.getApplication()
            .getSharedPreferences(name, Context.MODE_PRIVATE)
    }
