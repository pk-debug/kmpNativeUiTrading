/** Owns TradingComponents presentation responsibilities; shared domain rules remain in Kotlin. */
import SwiftUI
import SharedLogic
import Foundation

enum TradingPalette {
    static let ink = Color(red: 0.08, green: 0.17, blue: 0.16)
    static let muted = Color(red: 0.43, green: 0.51, blue: 0.49)
    static let green = Color(red: 0.08, green: 0.54, blue: 0.39)
    static let red = Color(red: 0.82, green: 0.30, blue: 0.30)
    static let canvas = Color(red: 0.96, green: 0.97, blue: 0.96)
}

struct AccountSummaryCard: View {
    let title: String
    let value: String
    let subtitle: String

    var body: some View {
        VStack(alignment: .leading, spacing: 7) {
            Text(title).font(.subheadline).foregroundStyle(.white.opacity(0.75))
            Text(value).font(.system(size: 30, weight: .bold)).foregroundStyle(.white)
            Text(subtitle).font(.caption).foregroundStyle(.white.opacity(0.75))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(22)
        .background(TradingPalette.ink, in: RoundedRectangle(cornerRadius: 22))
    }
}

struct MarketIndexCard: View {
    let index: MarketIndex

    var body: some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(index.name).font(.caption2.weight(.semibold)).foregroundStyle(TradingPalette.muted)
            Text(index.value).font(.subheadline.bold()).foregroundStyle(TradingPalette.ink)
            Text(index.change)
                .font(.caption)
                .foregroundStyle(index.isPositive ? TradingPalette.green : TradingPalette.red)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(.white, in: RoundedRectangle(cornerRadius: 16))
    }
}

struct AssetRow: View {
    let asset: MarketAsset

    var body: some View {
        HStack(spacing: 12) {
            SymbolBadge(symbol: asset.symbol)
            VStack(alignment: .leading, spacing: 3) {
                Text(asset.symbol).font(.subheadline.bold()).foregroundStyle(TradingPalette.ink)
                Text(asset.companyName).font(.caption).foregroundStyle(TradingPalette.muted).lineLimit(1)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 3) {
                Text(formatMoney(asset.price)).font(.subheadline.weight(.semibold)).foregroundStyle(TradingPalette.ink)
                Text(formatPercent(asset.changePercent))
                    .font(.caption)
                    .foregroundStyle(asset.changePercent >= 0 ? TradingPalette.green : TradingPalette.red)
            }
        }
        .padding(15)
        .background(.white, in: RoundedRectangle(cornerRadius: 18))
    }
}

struct PositionRow: View {
    let position: Position
    let asset: MarketAsset
    private var gainLoss: Double { (asset.price - position.averageCost) * Double(position.quantity) }

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(position.symbol).font(.subheadline.bold()).foregroundStyle(TradingPalette.ink)
                Text("\(position.quantity) shares · Avg \(formatMoney(position.averageCost))")
                    .font(.caption)
                    .foregroundStyle(TradingPalette.muted)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 4) {
                Text(formatMoney(asset.price * Double(position.quantity)))
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(TradingPalette.ink)
                Text(formatMoney(gainLoss))
                    .font(.caption)
                    .foregroundStyle(gainLoss >= 0 ? TradingPalette.green : TradingPalette.red)
            }
        }
        .padding(15)
        .background(.white, in: RoundedRectangle(cornerRadius: 18))
    }
}

struct OrderRow: View {
    let order: SimulatedOrder

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("\(order.side.name) \(order.symbol)")
                    .font(.subheadline.bold())
                    .foregroundStyle(TradingPalette.ink)
                Text("\(order.quantity) shares · \(formatMoney(order.price)) each")
                    .font(.caption)
                    .foregroundStyle(TradingPalette.muted)
            }
            Spacer()
            Text(formatMoney(order.total))
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(TradingPalette.ink)
        }
        .padding(15)
        .background(.white, in: RoundedRectangle(cornerRadius: 18))
    }
}

struct SummaryValue: View {
    let title: String
    let value: String
    var positive: Bool?

    var body: some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(title).font(.caption).foregroundStyle(TradingPalette.muted)
            Text(value)
                .font(.subheadline.bold())
                .foregroundStyle(positive.map { $0 ? TradingPalette.green : TradingPalette.red } ?? TradingPalette.ink)
        }
    }
}

struct SymbolBadge: View {
    let symbol: String

    var body: some View {
        Text(String(symbol.prefix(1)))
            .font(.headline.bold())
            .foregroundStyle(TradingPalette.green)
            .frame(width: 42, height: 42)
            .background(Color(red: 0.91, green: 0.96, blue: 0.93), in: Circle())
    }
}

struct Sparkline: View {
    let values: [Double]
    let positive: Bool

    var body: some View {
        GeometryReader { geometry in
            Path { path in
                guard values.count > 1, let minimum = values.min(), let maximum = values.max() else { return }
                let range = max(maximum - minimum, 1)
                for (index, value) in values.enumerated() {
                    let point = CGPoint(
                        x: geometry.size.width * CGFloat(index) / CGFloat(values.count - 1),
                        y: geometry.size.height * (1 - CGFloat((value - minimum) / range))
                    )
                    if index == 0 { path.move(to: point) } else { path.addLine(to: point) }
                }
            }
            .stroke(positive ? TradingPalette.green : TradingPalette.red, style: StrokeStyle(lineWidth: 3, lineCap: .round, lineJoin: .round))
        }
    }
}

struct EmptyMessage: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.subheadline)
            .foregroundStyle(TradingPalette.muted)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(18)
            .background(.white, in: RoundedRectangle(cornerRadius: 18))
    }
}

struct DemoDisclaimer: View {
    var body: some View {
        Text("Demo only. Prices are illustrative; no real orders or money are involved.")
            .font(.caption2)
            .foregroundStyle(TradingPalette.muted)
            .padding(.vertical, 8)
    }
}

/// Currency formatting is reused on the main UI thread instead of allocating per row.

let moneyFormatter: NumberFormatter = {
    let formatter = NumberFormatter()
    formatter.numberStyle = .currency
    formatter.locale = Locale(identifier: "en_IN")
    formatter.currencyCode = "INR"
    formatter.minimumFractionDigits = 2
    formatter.maximumFractionDigits = 2
    return formatter
}()

func formatMoney(_ amount: Double) -> String {
    return moneyFormatter.string(from: NSNumber(value: amount)) ?? "₹\(amount)"
}

func formatPercent(_ change: Double) -> String {
    let sign = change >= 0 ? "+" : ""
    return "\(sign)\(String(format: "%.2f", change))%"
}
