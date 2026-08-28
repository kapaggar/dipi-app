package org.dhamma.dipi.staff.applicants

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.dhamma.dipi.staff.ui.theme.Industry

/**
 * Name-bar widths as a fraction of the row. The design binds these to absent
 * data; fixed here so screenshot tests stay stable. See
 * docs/specs/2026-08-26-v3-conformance-spec.md S2.3.
 */
private val SKELETON_WIDTHS = listOf(0.52f, 0.66f, 0.44f, 0.60f, 0.72f, 0.48f, 0.58f, 0.64f)

/** The eight-row Today loading skeleton (design: `3 · Today — loading skeleton`). */
@Composable
fun TodaySkeleton(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().testTag("today-skeleton")) {
        SKELETON_WIDTHS.forEach { width -> SkeletonRow(width) }
    }
}

@Composable
private fun SkeletonRow(nameWidth: Float) {
    Column(Modifier.fillMaxWidth().testTag("skeleton-row")) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Bar(Modifier.weight(nameWidth).height(14.dp), Industry.neutral300)
                Spacer(Modifier.weight(1f - nameWidth))
                Bar(Modifier.width(46.dp).height(14.dp), Industry.neutral200)
            }
            Bar(Modifier.fillMaxWidth(0.62f).height(11.dp), Industry.neutral200)
        }
        HorizontalDivider(thickness = 1.dp, color = Industry.neutral300)
    }
}

@Composable
private fun Bar(modifier: Modifier, color: Color) {
    Box(modifier.clip(RoundedCornerShape(2.dp)).background(color))
}
