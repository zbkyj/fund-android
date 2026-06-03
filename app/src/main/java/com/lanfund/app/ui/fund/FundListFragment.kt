package com.lanfund.app.ui.fund

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.lanfund.app.R
import com.lanfund.app.data.repository.FundRepository
import com.lanfund.app.databinding.FragmentFundListBinding

/**
 * 基金列表Fragment
 */
class FundListFragment : Fragment() {

    private var _binding: FragmentFundListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FundViewModel by viewModels {
        FundViewModelFactory(FundRepository.getInstance(requireContext()))
    }

    private lateinit var adapter: FundAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFundListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeRefresh()
        setupFab()
        observeViewModel()

        // 初始加载数据
        viewModel.refreshEstimates()
    }

    private fun setupRecyclerView() {
        adapter = FundAdapter(
            onItemClick = { fund ->
                showFundDetailDialog(fund)
            },
            onItemLongClick = { fund ->
                showFundOptionsDialog(fund.code, fund.name)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@FundListFragment.adapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshEstimates()
        }

        binding.swipeRefresh.setColorSchemeResources(
            R.color.purple_500,
            R.color.teal_700
        )
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            showAddFundDialog()
        }
    }

    private fun observeViewModel() {
        viewModel.estimates.observe(viewLifecycleOwner) { estimates ->
            adapter.submitList(estimates)
            binding.emptyView.visibility = if (estimates.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerView.visibility = if (estimates.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
            binding.progressBar.visibility = if (isLoading && adapter.itemCount == 0) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun showAddFundDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "请输入基金代码（多个代码用逗号或空格分隔）"
            setPadding(48, 32, 48, 32)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }

        AlertDialog.Builder(requireContext())
            .setTitle("添加基金")
            .setView(editText)
            .setPositiveButton("添加") { _, _ ->
                val input = editText.text.toString().trim()
                if (input.isNotEmpty()) {
                    val codes = input.split(Regex("[,，\\s]+")).filter { it.isNotEmpty() }
                    if (codes.size == 1) {
                        viewModel.addFund(codes[0]) { success, message ->
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        viewModel.addFunds(codes) { success, message ->
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showFundDetailDialog(fund: com.lanfund.app.data.model.FundEstimate) {
        val message = buildString {
            append("基金代码: ${fund.code}\n")
            append("基金名称: ${fund.name}\n")
            append("估算时间: ${fund.time}\n")
            append("净值: ${fund.netValue}\n")
            append("估算涨幅: ${fund.estimateGrowth}\n")
            append("日涨幅: ${fund.dayGrowth}\n")
            append("连涨/跌: ${fund.consecutive}\n")
            append("近30天: ${fund.monthly}\n")
            if (fund.isHold) {
                append("持仓份额: ${fund.shares}\n")
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle(fund.name)
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show()
    }

    private fun showFundOptionsDialog(code: String, name: String) {
        val options = arrayOf("设置份额", "标记持有", "删除基金")

        AlertDialog.Builder(requireContext())
            .setTitle(name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showSetSharesDialog(code)
                    1 -> {
                        viewModel.setHold(code, true)
                        Toast.makeText(requireContext(), "已标记持有", Toast.LENGTH_SHORT).show()
                    }
                    2 -> showDeleteConfirmDialog(code, name)
                }
            }
            .show()
    }

    private fun showSetSharesDialog(code: String) {
        val editText = EditText(requireContext()).apply {
            hint = "请输入持仓份额"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("设置持仓份额")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val shares = editText.text.toString().toDoubleOrNull() ?: 0.0
                viewModel.updateShares(code, shares)
                Toast.makeText(requireContext(), "份额已更新", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showDeleteConfirmDialog(code: String, name: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除基金")
            .setMessage("确定要删除 $name 吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.removeFund(code)
                Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
