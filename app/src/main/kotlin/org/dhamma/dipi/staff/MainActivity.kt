package org.dhamma.dipi.staff

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.dhamma.dipi.staff.ui.DeskViewModel
import org.dhamma.dipi.staff.ui.DipiAppUi

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val vm: DeskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DipiAppUi(vm) }
    }
}
