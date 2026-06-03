package com.lanfund.app.data.model

/**
 * 基金数据结构
 */
data class Fund(
    val code: String,
    val name: String,
    val fundKey: String = "",
    var shares: Double = 0.0,
    var isHold: Boolean = false,
    var sectors: List<String> = emptyList()
)

/**
 * 基金估算数据
 */
data class FundEstimate(
    val code: String,
    val name: String,
    val time: String,           // 估算时间
    val netValue: String,       // 净值 (如 "1.2345(2024-01-01)")
    val estimateGrowth: String,  // 估算涨幅 (如 "1.23%")
    val dayGrowth: String,       // 日涨幅 (如 "1.50%")
    val consecutive: String,    // 连涨/跌 (如 "3天 1.23%")
    val monthly: String,        // 近30天 (如 "15/30 5.67%")
    val isHold: Boolean = false,
    val shares: Double = 0.0
)

/**
 * 基金持仓详情
 */
data class FundHolding(
    val code: String,
    val name: String,
    val shares: Double,
    val positionValue: Double,     // 持仓市值
    val estimatedGain: Double,     // 预估收益
    val estimatedGainPct: Double,  // 预估涨跌百分比
    val actualGain: Double,        // 实际收益
    val actualGainPct: Double      // 实际涨跌百分比
)

/**
 * 持仓统计汇总
 */
data class PositionSummary(
    val totalValue: Double,
    val estimatedGain: Double,
    val estimatedGainPct: Double,
    val actualGain: Double,
    val actualGainPct: Double,
    val settledValue: Double,
    val fundDetails: List<FundHolding>
)

/**
 * 市场指数数据
 */
data class MarketIndex(
    val name: String,
    val value: String,
    val change: String
)

/**
 * 黄金价格数据
 */
data class GoldPrice(
    val name: String,
    val price: String,
    val changeAmount: String,
    val changePercent: String
)
