package org.dhamma.dipi.staff

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.dhamma.dipi.staff.ui.DeskViewModel
import org.dhamma.dipi.staff.ui.DipiAppUi
import org.dhamma.dipi.staff.whatsapp.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val vm: DeskViewModel by viewModels()
    @Inject lateinit var whatsapp: WhatsAppController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        vm.onWhatsAppSessionExit = whatsapp::endSession
        vm.onWhatsAppErase = whatsapp::erase
        lifecycleScope.launch { vm.state.collect { whatsapp.bind(it) } }
        setContent {
            CompositionLocalProvider(LocalWhatsAppController provides whatsapp) {
                DipiAppUi(vm)
                WhatsAppDialogs(whatsapp)
            }
        }
    }
    override fun onDestroy() { whatsapp.endSession(); super.onDestroy() }
}
