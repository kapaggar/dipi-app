package org.dhamma.dipi.staff.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import org.dhamma.dipi.staff.BuildConfig

sealed interface DeskSiteDestination {
    data class EditApplication(val applicantId: Int) : DeskSiteDestination
    data class AdvancedSearch(val centreId: Int) : DeskSiteDestination
    data class AddApplication(val centreId: Int, val courseId: Int) : DeskSiteDestination
}

fun DeskSiteDestination.url(baseUrl: String = BuildConfig.BASE_URL): String {
    val root = baseUrl.trimEnd('/')
    return when (this) {
        // Both anonymous login and Drupal’s already-signed-in redirect honour destination.
        is DeskSiteDestination.EditApplication ->
            "$root/user/login?destination=app%2F$applicantId%2Fedit"
        is DeskSiteDestination.AdvancedSearch -> "$root/search-app/$centreId"
        is DeskSiteDestination.AddApplication -> "$root/app/add/$centreId/$courseId"
    }
}

fun interface DeskSiteLauncher {
    fun launch(destination: DeskSiteDestination): Boolean
}

fun Context.openDeskSite(destination: DeskSiteDestination): Boolean = try {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(destination.url())))
    true
} catch (_: ActivityNotFoundException) {
    false
}
