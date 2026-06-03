package com.lanfund.app.ui.fund

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lanfund.app.data.model.Fund
import com.lanfund.app.data.model.FundEstimate
import com.lanfund.app.data.repository.FundRepository
import kotlinx.coroutines.launch

/**
 * 基金列表ViewModel
 */
class FundViewModel(private val repository: FundRepository) : ViewModel() {

    private val _funds = MutableLiveData<List<Fund>>()
    val funds: LiveData<List<Fund>> = _funds

    private val _estimates = MutableLiveData<List<FundEstimate>>()
    val estimates: LiveData<List<FundEstimate>> = _estimates

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isInitialized = MutableLiveData<Boolean>()
    val isInitialized: LiveData<Boolean> = _isInitialized

    init {
        loadFunds()
        initialize()
    }

    /**
     * 初始化API
     */
    fun initialize() {
        viewModelScope.launch {
            val result = repository.initialize()
            _isInitialized.value = result
            if (!result) {
                _error.value = "初始化失败，请检查网络连接"
            }
        }
    }

    /**
     * 加载保存的基金列表
     */
    fun loadFunds() {
        _funds.value = repository.getSavedFunds()
    }

    /**
     * 刷新基金估算数据（受限频控制）
     */
    fun refreshEstimates() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val estimateList = repository.getFundEstimates()
                _estimates.value = estimateList
            } catch (e: Exception) {
                _error.value = e.message ?: "获取数据失败"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 强制刷新（忽略限频，用于下拉刷新）
     */
    fun forceRefreshEstimates() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val estimateList = repository.forceRefreshEstimates()
                _estimates.value = estimateList
            } catch (e: Exception) {
                _error.value = e.message ?: "获取数据失败"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 添加基金
     */
    fun addFund(fundCode: String, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val fund = repository.addFund(fundCode)
                if (fund != null) {
                    loadFunds()
                    callback(true, "添加成功")
                    forceRefreshEstimates()
                } else {
                    callback(false, "添加失败，基金代码可能不存在")
                }
            } catch (e: Exception) {
                callback(false, e.message ?: "添加失败")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 批量添加基金
     */
    fun addFunds(fundCodes: List<String>, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val (successCount, failCount) = repository.addFunds(fundCodes)
                loadFunds()
                forceRefreshEstimates()
                
                if (successCount > 0 && failCount == 0) {
                    callback(true, "全部添加成功，共 $successCount 只基金")
                } else if (successCount > 0 && failCount > 0) {
                    callback(true, "成功添加 $successCount 只，失败 $failCount 只")
                } else {
                    callback(false, "添加失败，所有基金代码都无效或已存在")
                }
            } catch (e: Exception) {
                callback(false, e.message ?: "添加失败")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 删除基金
     */
    fun removeFund(fundCode: String) {
        viewModelScope.launch {
            repository.removeFund(fundCode)
            loadFunds()
            forceRefreshEstimates()
        }
    }

    /**
     * 更新基金份额
     */
    fun updateShares(fundCode: String, shares: Double) {
        repository.updateFundShares(fundCode, shares)
        loadFunds()
        forceRefreshEstimates()
    }

    /**
     * 设置持有状态
     */
    fun setHold(fundCode: String, isHold: Boolean) {
        repository.setFundHold(fundCode, isHold)
        loadFunds()
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _error.value = null
    }
}
