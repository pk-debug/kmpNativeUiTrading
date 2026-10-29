/** Owns ObservableModels presentation responsibilities; shared domain rules remain in Kotlin. */
import SwiftUI
import SharedLogic
import Foundation

final class TradingObservableModel: NSObject, ObservableObject, TradingStateObserver {
    @Published private(set) var state: TradingState
    let viewModel: TradingViewModel
    private var observerId: Int64 = 0

    init(viewModel: TradingViewModel) {
        self.viewModel = viewModel
        state = viewModel.state
        super.init()
        observerId = viewModel.addObserver(observer: TradingObserverProxy(owner: self))
    }

    func onStateChanged(state: TradingState) {
        DispatchQueue.main.async {
            self.state = state
        }
    }

    deinit {
        viewModel.removeObserver(id: observerId)
    }
}

final class AuthObservableModel: NSObject, ObservableObject, AuthStateObserver {
    @Published private(set) var state: AuthState
    let viewModel: AuthViewModel
    private var observerId: Int64 = 0

    init(viewModel: AuthViewModel) {
        self.viewModel = viewModel
        state = viewModel.state
        super.init()
        observerId = viewModel.addObserver(observer: AuthObserverProxy(owner: self))
    }

    func onAuthStateChanged(state: AuthState) {
        DispatchQueue.main.async {
            self.state = state
        }
    }

    deinit {
        viewModel.removeObserver(id: observerId)
    }
}

// Kotlin owns its observer. A weak proxy avoids a model -> VM -> model retain cycle.

final class TradingObserverProxy: NSObject, TradingStateObserver {
    weak var owner: TradingObservableModel?
    init(owner: TradingObservableModel) { self.owner = owner; super.init() }
    func onStateChanged(state: TradingState) { owner?.onStateChanged(state: state) }
}

final class AuthObserverProxy: NSObject, AuthStateObserver {
    weak var owner: AuthObservableModel?
    init(owner: AuthObservableModel) { self.owner = owner; super.init() }
    func onAuthStateChanged(state: AuthState) { owner?.onAuthStateChanged(state: state) }
}
