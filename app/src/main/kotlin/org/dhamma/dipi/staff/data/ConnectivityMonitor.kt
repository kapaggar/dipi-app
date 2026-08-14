package org.dhamma.dipi.staff.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectivityMonitor @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val cm = context.getSystemService(ConnectivityManager::class.java)

    val online: Flow<Boolean> = callbackFlow {
        fun push() {
            val net = cm.activeNetwork
            val caps = net?.let { cm.getNetworkCapabilities(it) }
            trySend(caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true)
        }
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = push()
            override fun onLost(network: Network) = push()
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = push()
        }
        push()
        cm.registerNetworkCallback(
            NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
            cb,
        )
        awaitClose { runCatching { cm.unregisterNetworkCallback(cb) } }
    }.distinctUntilChanged()
}
