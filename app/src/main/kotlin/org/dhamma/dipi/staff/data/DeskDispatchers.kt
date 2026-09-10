package org.dhamma.dipi.staff.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

/** Separate CPU work from blocking storage; injectable for deterministic tests. */
class DeskDispatchers(val computation: CoroutineDispatcher, val io: CoroutineDispatcher) {
    @Inject constructor() : this(Dispatchers.Default, Dispatchers.IO)
}
