# Northstar KMP Trading Demo

Northstar is a Kotlin Multiplatform learning demo for the Indian market. It brings Discover, asset details, Portfolio, and Activity flows from the [native iOS sample](https://github.com/pk-debug/stocktradingios) to Android and iOS. Quotes and index values are illustrative; orders only affect an on-device demo account. Nothing connects to a broker, exchange, or live market-data service.

## App structure

- `androidApp` contains the native Android UI, written with Jetpack Compose.
- `iosApp/iosApp` contains the native iOS UI, written with SwiftUI.
- `sharedLogic/src/commonMain` contains the shared domain models, repository contract, order/search/portfolio use cases, and MVVM state holder.
- `sharedLogic/src/androidMain` and `sharedLogic/src/iosMain` provide device-local ledger persistence.

Both native UIs observe one shared `TradingViewModel`. Order validation and ledger updates happen in shared Kotlin. Market data is bundled locally; screen entry and button taps do not make network requests or refresh quotes. The demo account and order history are saved on-device (Android SharedPreferences and iOS UserDefaults), so they remain available offline and across app restarts.

## Run and test

- Android: `./gradlew :androidApp:assembleDebug`
- Shared Kotlin tests: `./gradlew :sharedLogic:allTests`
- iOS: open `iosApp` in Xcode and run the iOS target on a simulator or device.

The starting account has ₹10,00,000 in sample cash and three illustrative holdings. Prices are fixed examples, not live or historical data. Orders are simulations only; this app is not investment advice.
