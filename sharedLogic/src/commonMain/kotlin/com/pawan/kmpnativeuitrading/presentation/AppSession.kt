package com.pawan.kmpnativeuitrading.presentation

import com.pawan.kmpnativeuitrading.domain.repository.SessionRepository
import com.pawan.kmpnativeuitrading.domain.repository.TradingRepository
import com.pawan.kmpnativeuitrading.network.RemoteAccount

/** A tiny composition root: both native UIs share one authenticated account. */
class AppSession(repository: TradingRepository, sessions: SessionRepository, private val remote: RemoteAccount? = null) {
    val trading = TradingViewModel(repository, remote = remote)
    val auth = AuthViewModel(sessions, remote, trading::refresh, trading::clearRemoteState)
    fun close() { trading.close(); auth.close(); remote?.close() }
}
