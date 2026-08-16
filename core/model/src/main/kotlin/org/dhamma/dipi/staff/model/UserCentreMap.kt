package org.dhamma.dipi.staff.model

/** Maps a desk username to the centre name Drupal would attach via dh_user_center. */
object UserCentreMap {
    fun name(username: String): String {
        val slug = username.trim().substringBefore('@').lowercase()
        val token = when {
            slug.endsWith(".user") -> slug.removeSuffix(".user")
            slug.contains('.') -> slug.substringAfterLast('.')
            else -> slug
        }.replace('_', ' ').replace('-', ' ')
        val titled = token.split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part -> part.replaceFirstChar { ch -> ch.uppercase() } }
        return if (titled.isBlank()) "Centre" else "Dhamma $titled"
    }
}
