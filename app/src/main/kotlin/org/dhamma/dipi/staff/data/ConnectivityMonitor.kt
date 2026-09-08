package org.dhamma.dipi.staff.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectivityMonitor @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val cm = context.getSystemService(ConnectivityManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile private var testOverride: Boolean? = null
    private val _online = MutableStateFlow(readOnline())
    val online: StateFlow<Boolean> = _online.asStateFlow()

    private fun readOnline(): Boolean {
        testOverride?.let { return it }
        val mgr = cm ?: return false
        val net = mgr.activeNetwork
        val caps = net?.let { mgr.getNetworkCapabilities(it) }
        return caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    init {
        // Keep the callbackFlow open even when registerNetworkCallback fails.
        // A completed/failed online Flow used to complete combine() after the
        // first emission, so later Simulate-offline writes never reached UI.
        scope.launch {
            callbackFlow {
                fun push() { trySend(readOnline()) }
                val cb = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) = push()
                    override fun onLost(network: Network) = push()
                    override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = push()
                }
                push()
                val mgr = cm
                val registered = mgr != null && runCatching {
                    mgr.registerNetworkCallback(
                        NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
                        cb,
                    )
                }.isSuccess
                awaitClose { if (registered) runCatching { mgr?.unregisterNetworkCallback(cb) } }
            }.distinctUntilChanged().collect { _online.value = it }
        }
    }

    @androidx.annotation.VisibleForTesting
    internal fun setOnlineForTest(value: Boolean) {
        testOverride = value
        _online.value = value
    }
}
