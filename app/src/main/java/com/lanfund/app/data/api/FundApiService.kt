package com.lanfund.app.data.api

import com.lanfund.app.data.model.FundEstimate
import com.lanfund.app.data.model.MarketIndex
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * 基金API服务
 * 基于原Python项目fund123.cn的API实现
 */
class FundApiService {

    private val client: OkHttpClient
    private val gson = Gson()

    // CSRF token (从fund123.cn获取)
    private var csrfToken: String = ""

    // fund123.cn的base URL
    private val fund123BaseUrl = "https://www.fund123.cn"

    init {
        // 创建忽略SSL验证的OkHttpClient (用于测试环境)
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())

        client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    /**
     * 初始化CSRF token
     */
    suspend fun init(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$fund123BaseUrl/fund")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "text/html,application/xhtml+xml")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext false

            // 从HTML中提取CSRF token
            val csrfRegex = """"csrf":"(.*?)"""".toRegex()
            val match = csrfRegex.find(body)
            csrfToken = match?.groupValues?.get(1) ?: ""

            csrfToken.isNotEmpty()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 搜索基金
     */
    suspend fun searchFund(fundCode: String): FundInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$fund123BaseUrl/api/fund/searchFund?_csrf=$csrfToken")
                .post(
                    okhttp3.MediaType.parse("application/json"),
                    """{"fundCode":"$fundCode"}"""
                )
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Origin", fund123BaseUrl)
                .header("Referer", "$fund123BaseUrl/fund")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "{}")

            if (json.optBoolean("success", false)) {
                val fundInfo = json.optJSONObject("fundInfo")
                fundInfo?.let {
                    FundInfo(
                        key = it.optString("key"),
                        code = fundCode,
                        name = it.optString("fundName")
                    )
                }
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取基金实时估算数据
     */
    suspend fun getFundEstimate(fundCode: String, fundKey: String): FundEstimate? = withContext(Dispatchers.IO) {
        try {
            // 获取基金基本信息
            val detailRequest = Request.Builder()
                .url("$fund123BaseUrl/matiaria?fundCode=$fundCode")
                .header("Accept", "application/json")
                .header("Referer", "$fund123BaseUrl/fund")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val detailResponse = client.newCall(detailRequest).execute()
            val detailBody = detailResponse.body?.string() ?: ""

            val dayOfGrowth = extractJsonValue(detailBody, "dayOfGrowth")
            val netValue = extractJsonValue(detailBody, "netValue")
            val netValueDate = extractJsonValue(detailBody, "netValueDate")

            // 获取估算涨幅数据
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val tomorrow = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                Date(System.currentTimeMillis() + 86400000)
            )

            val estimateRequest = Request.Builder()
                .url("$fund123BaseUrl/api/fund/queryFundEstimateIntraday?_csrf=$csrfToken")
                .post(
                    okhttp3.MediaType.parse("application/json"),
                    """
                    {
                        "startTime": "$today",
                        "endTime": "$tomorrow",
                        "limit": 200,
                        "productId": "$fundKey",
                        "format": true,
                        "source": "WEALTHBFFWEB"
                    }
                    """.trimIndent()
                )
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Origin", fund123BaseUrl)
                .header("Referer", "$fund123BaseUrl/fund")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val estimateResponse = client.newCall(estimateRequest).execute()
            val estimateJson = JSONObject(estimateResponse.body?.string() ?: "{}")

            if (!estimateJson.optBoolean("success", false)) {
                return@withContext null
            }

            val list = estimateJson.optJSONArray("list")
            if (list == null || list.length() == 0) {
                return@withContext FundEstimate(
                    code = fundCode,
                    name = "",
                    time = "N/A",
                    netValue = "$netValue($netValueDate)",
                    estimateGrowth = "N/A",
                    dayGrowth = formatGrowth(dayOfGrowth),
                    consecutive = "N/A",
                    monthly = "N/A"
                )
            }

            val lastData = list.getJSONObject(list.length() - 1)
            val time = SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(Date(lastData.optLong("time")))
            val forecastGrowth = formatGrowth(lastData.optDouble("forecastGrowth").toString())

            // 获取近30天数据
            val monthlyData = getMonthlyData(fundKey)

            FundEstimate(
                code = fundCode,
                name = "",
                time = time,
                netValue = "$netValue($netValueDate)",
                estimateGrowth = forecastGrowth,
                dayGrowth = formatGrowth(dayOfGrowth),
                consecutive = monthlyData.first,
                monthly = monthlyData.second
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取近30天趋势数据
     */
    private suspend fun getMonthlyData(fundKey: String): Pair<String, String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$fund123BaseUrl/api/fund/queryFundQuotationCurves?_csrf=$csrfToken")
                .post(
                    okhttp3.MediaType.parse("application/json"),
                    """{"productId": "$fundKey", "dateInterval": "ONE_MONTH"}"""
                )
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Origin", fund123BaseUrl)
                .header("Referer", "$fund123BaseUrl/fund")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "{}")

            if (!json.optBoolean("success", false)) {
                return@withContext Pair("N/A", "N/A")
            }

            val points = json.optJSONArray("points") ?: return@withContext Pair("N/A", "N/A")
            val fundPoints = mutableListOf<JSONObject>()

            for (i in 0 until points.length()) {
                val point = points.getJSONObject(i)
                if (point.optString("type") == "fund") {
                    fundPoints.add(point)
                }
            }

            if (fundPoints.size < 2) {
                return@withContext Pair("N/A", "N/A")
            }

            // 计算涨/跌天数
            var riseDays = 0
            var lastRate = fundPoints[0].optDouble("rate")
            val growthList = mutableListOf<String>()

            for (i in 1 until fundPoints.size) {
                val nowRate = fundPoints[i].optDouble("rate")
                if (nowRate >= lastRate) {
                    growthList.add("涨,$nowRate")
                    riseDays++
                } else {
                    growthList.add("跌,$nowRate")
                }
                lastRate = nowRate
            }

            // 计算连涨/跌
            val growthListReversed = growthList.reversed()
            var consecutiveCount = 1
            var startRate = growthListReversed[0].split(",")[1].toDoubleOrNull() ?: 0.0
            var endRate = 0.0

            for (i in 1 until growthListReversed.size) {
                if (growthListReversed[i][0] == growthListReversed[0][0]) {
                    consecutiveCount++
                } else {
                    endRate = growthListReversed[i].split(",")[1].toDoubleOrNull() ?: 0.0
                    break
                }
            }

            val startRatePercent = String.format("%.2f", startRate * 100)
            val consecutiveGrowth = String.format("%.2f", (startRate - endRate) * 100)
            val monthlyTotal = growthList.size
            val monthlyPercent = String.format("%.2f", startRate * 100)

            Pair(
                "$consecutiveCount天 $consecutiveGrowth%",
                "$riseDays/$monthlyTotal $monthlyPercent%"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Pair("N/A", "N/A")
        }
    }

    /**
     * 从HTML中提取JSON值
     */
    private fun extractJsonValue(html: String, key: String): String {
        val regex = """"$key":"(.*?)"""".toRegex()
        val match = regex.find(html)
        return match?.groupValues?.get(1) ?: "N/A"
    }

    /**
     * 格式化涨幅
     */
    private fun formatGrowth(value: Any?): String {
        if (value == null || value.toString() == "N/A") return "N/A"
        return try {
            String.format("%.2f%%", (value as Double))
        } catch (e: Exception) {
            try {
                String.format("%.2f%%", value.toString().toDouble())
            } catch (e2: Exception) {
                "N/A"
            }
        }
    }

    /**
     * 基金信息
     */
    data class FundInfo(
        val key: String,
        val code: String,
        val name: String
    )
}
