package com.pawan.kmpnativeuitrading.domain.model

object MarketCatalog {
    val assets = listOf(
        MarketAsset("RELIANCE", "Reliance Industries", "NSE", 1_284.50, 12.35, 0.97, listOf(34.0, 41.0, 38.0, 52.0, 47.0, 63.0, 58.0, 69.0, 64.0, 82.0, 77.0, 94.0), "Energy & Retail"),
        MarketAsset("TCS", "Tata Consultancy Services", "NSE", 3_482.75, 24.50, 0.71, listOf(30.0, 36.0, 34.0, 48.0, 45.0, 61.0, 55.0, 68.0, 64.0, 78.0, 74.0, 92.0), "Information Technology"),
        MarketAsset("HDFCBANK", "HDFC Bank", "NSE", 1_768.40, -8.20, -0.46, listOf(88.0, 80.0, 84.0, 70.0, 75.0, 61.0, 66.0, 51.0, 57.0, 43.0, 48.0, 35.0), "Banking"),
        MarketAsset("INFY", "Infosys", "NSE", 1_482.60, 5.35, 0.36, listOf(31.0, 39.0, 35.0, 49.0, 46.0, 58.0, 55.0, 67.0, 63.0, 76.0, 73.0, 90.0), "Information Technology"),
        MarketAsset("ICICIBANK", "ICICI Bank", "NSE", 1_298.25, 7.25, 0.56, listOf(27.0, 33.0, 30.0, 45.0, 41.0, 54.0, 51.0, 66.0, 60.0, 75.0, 72.0, 89.0), "Banking"),
        MarketAsset("ITC", "ITC Limited", "NSE", 438.90, -2.10, -0.48, listOf(82.0, 77.0, 80.0, 68.0, 71.0, 61.0, 65.0, 53.0, 58.0, 47.0, 50.0, 39.0), "Consumer Goods"),
        MarketAsset("TATAMOTORS", "Tata Motors", "NSE", 728.15, 10.80, 1.51, listOf(24.0, 31.0, 29.0, 44.0, 41.0, 57.0, 53.0, 66.0, 62.0, 78.0, 73.0, 93.0), "Automotive"),
    )

    val indexes = listOf(
        MarketIndex("NIFTY 50", "24,854.05", "+0.62%", true),
        MarketIndex("SENSEX", "81,330.56", "+0.48%", true),
        MarketIndex("BANK NIFTY", "56,214.30", "-0.21%", false),
    )

    fun asset(symbol: String): MarketAsset? =
        assets.firstOrNull { it.symbol.equals(symbol, ignoreCase = true) }
}
