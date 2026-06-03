package com.lanfund.app.ui.fund

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lanfund.app.R
import com.lanfund.app.data.model.FundEstimate
import com.lanfund.app.databinding.ItemFundBinding

/**
 * 基金列表适配器
 */
class FundAdapter(
    private val onItemClick: (FundEstimate) -> Unit,
    private val onItemLongClick: (FundEstimate) -> Unit
) : ListAdapter<FundEstimate, FundAdapter.FundViewHolder>(FundDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FundViewHolder {
        val binding = ItemFundBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FundViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FundViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FundViewHolder(
        private val binding: ItemFundBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(fund: FundEstimate) {
            binding.apply {
                tvFundCode.text = fund.code
                tvFundName.text = if (fund.isHold) "⭐ ${fund.name}" else fund.name
                tvEstimateTime.text = fund.time
                tvNetValue.text = fund.netValue

                // 估算涨幅 - 根据正负设置颜色
                tvEstimateGrowth.text = fund.estimateGrowth
                val estimateColor = getGrowthColor(fund.estimateGrowth)
                tvEstimateGrowth.setTextColor(estimateColor)

                // 日涨幅
                tvDayGrowth.text = fund.dayGrowth
                val dayColor = getGrowthColor(fund.dayGrowth)
                tvDayGrowth.setTextColor(dayColor)

                // 连涨/跌
                tvConsecutive.text = fund.consecutive

                // 近30天
                tvMonthly.text = fund.monthly

                // 点击事件
                root.setOnClickListener { onItemClick(fund) }
                root.setOnLongClickListener {
                    onItemLongClick(fund)
                    true
                }
            }
        }

        private fun getGrowthColor(value: String): Int {
            val context = binding.root.context
            return when {
                value.contains("-") -> ContextCompat.getColor(context, R.color.decline)
                value == "N/A" -> ContextCompat.getColor(context, R.color.text_secondary)
                else -> ContextCompat.getColor(context, R.color.rise)
            }
        }
    }

    class FundDiffCallback : DiffUtil.ItemCallback<FundEstimate>() {
        override fun areItemsTheSame(oldItem: FundEstimate, newItem: FundEstimate): Boolean {
            return oldItem.code == newItem.code
        }

        override fun areContentsTheSame(oldItem: FundEstimate, newItem: FundEstimate): Boolean {
            return oldItem == newItem
        }
    }
}
