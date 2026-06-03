package com.lanfund.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lanfund.app.data.api.FundApiService
import com.lanfund.app.data.model.Fund
import com.lanfund.app.data.model.FundEstimate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 基金数据仓库
 * 负责基金数据的获取和本地存储
 */
class FundRepository(private val context: Context) {

    private val apiService = FundApiService()
    private val gson = Gson()
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // CSRF token是否已初始化
    private var isInitialized = false

    /**
     * 初始化API服务
     */
    suspend fun initialize(): Boolean {
        if (isInitialized) return true
        val result = apiService.init()
        isInitialized = result
        return result
    }

    /**
     * 获取用户保存的基金列表
     */
    fun getSavedFunds(): List<Fund> {
        val json = prefs.getString(KEY_FUNDS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Fund>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 保存基金列表
     */
    fun saveFunds(funds: List<Fund>) {
        prefs.edit().putString(KEY_FUNDS, gson.toJson(funds)).apply()
    }

    /**
     * 添加基金
     */
    suspend fun addFund(fundCode: String): Fund? {
        if (!isInitialized && !initialize()) {
            return null
        }

        val fundInfo = apiService.searchFund(fundCode) ?: return null

        val fund = Fund(
            code = fundCode,
            name = fundInfo.name,
            fundKey = fundInfo.key
        )

        val currentFunds = getSavedFunds().toMutableList()
        // 检查是否已存在
        if (currentFunds.any { it.code == fundCode }) {
            return null
        }
        currentFunds.add(fund)
        saveFunds(currentFunds)

        return fund
    }

    /**
     * 批量添加基金
     */
    suspend fun addFunds(fundCodes: List<String>): Pair<Int, Int> {
        if (!isInitialized && !initialize()) {
            return Pair(0, fundCodes.size)
        }

        val currentFunds = getSavedFunds().toMutableList()
        var successCount = 0
        var failCount = 0

        for (fundCode in fundCodes) {
            val trimmedCode = fundCode.trim()
            if (trimmedCode.length != 6) {
                failCount++
                continue
            }

            if (currentFunds.any { it.code == trimmedCode }) {
                failCount++
                continue
            }

            val fundInfo = apiService.searchFund(trimmedCode)
            if (fundInfo != null) {
                val fund = Fund(
                    code = trimmedCode,
                    name = fundInfo.name,
                    fundKey = fundInfo.key
                )
                currentFunds.add(fund)
                successCount++
            } else {
                failCount++
            }
        }

        if (successCount > 0) {
            saveFunds(currentFunds)
        }

        return Pair(successCount, failCount)
    }

    /**
     * 删除基金
     */
    fun removeFund(fundCode: String) {
        val currentFunds = getSavedFunds().toMutableList()
        currentFunds.removeAll { it.code == fundCode }
        saveFunds(currentFunds)
    }

    /**
     * 更新基金份额
     */
    fun updateFundShares(fundCode: String, shares: Double) {
        val currentFunds = getSavedFunds().toMutableList()
        val index = currentFunds.indexOfFirst { it.code == fundCode }
        if (index >= 0) {
            currentFunds[index] = currentFunds[index].copy(shares = shares, isHold = shares > 0)
            saveFunds(currentFunds)
        }
    }

    /**
     * 设置基金为持有状态
     */
    fun setFundHold(fundCode: String, isHold: Boolean) {
        val currentFunds = getSavedFunds().toMutableList()
        val index = currentFunds.indexOfFirst { it.code == fundCode }
        if (index >= 0) {
            currentFunds[index] = currentFunds[index].copy(isHold = isHold)
            saveFunds(currentFunds)
        }
    }

    /**
     * 获取基金估算数据列表
     */
    suspend fun getFundEstimates(): List<FundEstimate> {
        val funds = getSavedFunds()
        if (funds.isEmpty()) return emptyList()

        if (!isInitialized) {
            initialize()
        }

        val estimates = mutableListOf<FundEstimate>()

        for (fund in funds) {
            val estimate = apiService.getFundEstimate(fund.code, fund.fundKey)
            if (estimate != null) {
                estimates.add(
                    estimate.copy(
                        name = fund.name,
                        isHold = fund.isHold,
                        shares = fund.shares
                    )
                )
            } else {
                estimates.add(
                    FundEstimate(
                        code = fund.code,
                        name = fund.name,
                        time = "N/A",
                        netValue = "N/A",
                        estimateGrowth = "N/A",
                        dayGrowth = "N/A",
                        consecutive = "N/A",
                        monthly = "N/A",
                        isHold = fund.isHold,
                        shares = fund.shares
                    )
                )
            }
        }

        return estimates.sortedByDescending {
            it.estimateGrowth.replace("%", "").toDoubleOrNull() ?: 0.0
        }
    }

    /**
     * 获取单个基金估算数据
     */
    suspend fun getFundEstimate(fund: Fund): FundEstimate? {
        if (!isInitialized) {
            initialize()
        }

        val estimate = apiService.getFundEstimate(fund.code, fund.fundKey)
        return estimate?.copy(
            name = fund.name,
            isHold = fund.isHold,
            shares = fund.shares
        )
    }

    companion object {
        private const val PREFS_NAME = "lanfund_prefs"
        private const val KEY_FUNDS = "saved_funds"

        @Volatile
        private var instance: FundRepository? = null

        fun getInstance(context: Context): FundRepository {
            return instance ?: synchronized(this) {
                instance ?: FundRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
