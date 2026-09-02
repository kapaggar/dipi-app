package org.dhamma.dipi.staff.model

/**
 * The tablet's device mode (spec 2a). A mode, not a login role: one tablet,
 * one account, one running course. `DESK` is the registrar's desk build;
 * `COURSE_OPS` hands the tablet to the assistant teacher — two read-only
 * destinations, every desk surface hidden until the mode is switched back.
 */
enum class TabletMode(val key: String) {
    DESK("desk"),
    COURSE_OPS("course_ops"),
    ;

    companion object {
        /** Unknown/absent keys fall back to DESK — the desk build is the default. */
        fun fromKey(key: String?): TabletMode = entries.firstOrNull { it.key == key } ?: DESK
    }
}
