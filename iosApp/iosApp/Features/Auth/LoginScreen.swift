/** Owns LoginScreen presentation responsibilities; shared domain rules remain in Kotlin. */
import SwiftUI
import SharedLogic
import Foundation

struct LoginScreen: View {
    @ObservedObject var model: AuthObservableModel
    @State private var register = false
    @State private var phoneNumber = ""
    @State private var password = ""
    @State private var errorMessage: String?

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 18) {
                Text("NORTHSTAR")
                    .font(.caption.weight(.bold))
                    .tracking(1.6)
                    .foregroundStyle(TradingPalette.green)
                Text("Welcome back")
                    .font(.largeTitle.bold())
                    .foregroundStyle(TradingPalette.ink)
                Text(model.state.isRemote ? "Your private paper-trading account" : "Offline practice · no real money")
                    .font(.subheadline)
                    .foregroundStyle(TradingPalette.muted)

                if model.state.isRemote {
                TextField(model.state.isRemote ? "Username" : "Demo phone number", text: $phoneNumber)
                    .keyboardType(model.state.isRemote ? .asciiCapable : .phonePad)
                    .textContentType(.telephoneNumber)
                    .textInputAutocapitalization(.never)
                    .padding(14)
                    .background(.white, in: RoundedRectangle(cornerRadius: 14))

                SecureField("Password", text: $password)
                    .textContentType(.password)
                    .padding(14)
                    .background(.white, in: RoundedRectangle(cornerRadius: 14))

                }
                if let message = model.state.errorMessage ?? errorMessage {
                    Text(message)
                        .font(.caption)
                        .foregroundStyle(TradingPalette.red)
                }

                Button {
                    let result = !model.state.isRemote
                        ? model.viewModel.login(phoneNumber: "0000000000", password: "offline-demo-only")
                        : register
                        ? model.viewModel.register(username: phoneNumber, password: password)
                        : model.viewModel.login(phoneNumber: phoneNumber, password: password)
                    errorMessage = result.success ? nil : result.message
                } label: {
                    Text(model.state.isBusy ? "Connecting…" : (register ? "Create paper account" : (model.state.isRemote ? "Sign in" : "Enter offline demo")))
                        .font(.headline)
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 15)
                        .background(TradingPalette.green, in: RoundedRectangle(cornerRadius: 14))
                }

                .disabled(model.state.isBusy)

                if model.state.isRemote {
                    Button(register ? "Already have an account? Sign in" : "Create an account") { register.toggle(); errorMessage = nil }
                        .disabled(model.state.isBusy)
                    Text("Use a unique username and a password of at least 12 characters. This is a practice account, not a brokerage account.")
                        .font(.caption)
                }
                Text(model.state.isRemote ? "Passwords are verified by your server; they are never stored on this phone." : "No account or password needed. Practice with sample money on this device. Prices are illustrative.")
                    .font(.caption)
                    .foregroundStyle(TradingPalette.muted)
            }
            .padding(24)
            .frame(maxWidth: 480)
        }
        .background(TradingPalette.canvas.ignoresSafeArea())
    }
}
