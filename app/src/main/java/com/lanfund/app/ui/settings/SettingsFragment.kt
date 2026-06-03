package com.lanfund.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.lanfund.app.data.repository.FundRepository
import com.lanfund.app.databinding.FragmentSettingsBinding

/**
 * 设置Fragment
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = FundRepository.getInstance(requireContext())
        val currentInterval = repository.getRefreshIntervalMinutes()

        when (currentInterval) {
            1 -> binding.rb1min.isChecked = true
            5 -> binding.rb5min.isChecked = true
            10 -> binding.rb10min.isChecked = true
            else -> binding.rb1min.isChecked = true
        }

        binding.rgRefreshInterval.setOnCheckedChangeListener { _, checkedId ->
            val minutes = when (checkedId) {
                com.lanfund.app.R.id.rb_5min -> 5
                com.lanfund.app.R.id.rb_10min -> 10
                else -> 1
            }
            repository.setRefreshIntervalMinutes(minutes)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}