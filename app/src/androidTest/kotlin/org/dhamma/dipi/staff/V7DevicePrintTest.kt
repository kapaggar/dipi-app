package org.dhamma.dipi.staff

import android.content.Context
import android.print.PrintManager
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.dhamma.dipi.staff.desk.SheetStylesheet
import org.dhamma.dipi.staff.model.SheetExport
import org.dhamma.dipi.staff.ui.NativePrint
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Opt-in real PrintManager fixture; operator saves the PDF through the system print UI. */
@RunWith(AndroidJUnit4::class)
class V7DevicePrintTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun syntheticPrintJob() {
        val fixture = InstrumentationRegistry.getArguments().getString("printFixture")
        assumeTrue("Supply printFixture=chits or slips for interactive PDF acceptance", fixture in listOf("chits", "slips"))
        val count = if (fixture == "chits") 25 else 5
        val export = if (fixture == "chits") SheetExport.StudentChit else SheetExport.CheckingSlip
        val body = buildString {
            append("<div class=\"main-div\">")
            repeat(count) { i ->
                append("<div class=\"table-student-chit\"><div class=\"seat\">A${i + 1}</div><div class=\"room\">Synthetic ${i + 1}</div>")
                append("<div class=\"name\">Synthetic ${fixture} ${(i + 1).toString().padStart(2, '0')}</div><div class=\"cell\"></div></div>")
            }
            append("</div>")
        }
        rule.setContent { Text("Synthetic print verification - no applicant records") }
        val jobName = "dipi-v7-synthetic-$fixture"
        rule.runOnUiThread { NativePrint.printHtml(rule.activity, jobName, SheetStylesheet.render(body, emptySet(), export)) }
        val manager = rule.activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val deadline = System.currentTimeMillis() + 120_000
        var complete = false
        while (System.currentTimeMillis() < deadline && !complete) {
            complete = manager.printJobs.any { it.info.label == jobName && it.isCompleted }
            if (!complete) Thread.sleep(300)
        }
        assertTrue("Save the synthetic PDF through Android PrintManager before timeout", complete)
    }
}
