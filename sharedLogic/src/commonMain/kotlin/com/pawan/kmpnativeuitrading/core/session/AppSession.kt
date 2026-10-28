/**
 * Wires platform dependencies to feature view models. This composition root is the only place that assembles the whole application.
 */
package com.pawan.kmpnativeuitrading.core.session

import com.pawan.kmpnativeuitrading.features.auth.presentation.AuthViewModel
import com.pawan.kmpnativeuitrading.features.trading.presentation.TradingViewModel
import com.pawan.kmpnativeuitrading.features.auth.domain.repository.SessionRepository
import com.pawan.kmpnativeuitrading.features.trading.domain.repository.TradingRepository
import com.pawan.kmpnativeuitrading.features.trading.data.remote.RemoteAccount

/** A tiny composition root: both native UIs share one authenticated account. */
class AppSession(repository: TradingRepository, sessions: SessionRepository, private val remote: RemoteAccount? = null) {
    val trading = TradingViewModel(repository, remote = remote)
    val auth = AuthViewModel(sessions, remote, trading::refresh, trading::clearRemoteState)
    fun close() { trading.close(); auth.close(); remote?.close() }
}
