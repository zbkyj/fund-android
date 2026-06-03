package com.lanfund.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.lanfund.app.R
import com.lanfund.app.databinding.ActivityMainBinding
import com.lanfund.app.ui.fund.FundListFragment
import com.lanfund.app.ui.market.MarketFragment
import com.lanfund.app.ui.settings.SettingsFragment

/**
 * 主Activity
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "基金净值估算"

        if (savedInstanceState == null) {
            loadFragment(FundListFragment())
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_fund -> {
                    loadFragment(FundListFragment())
                    supportActionBar?.title = "基金净值估算"
                    true
                }
                R.id.nav_market -> {
                    loadFragment(MarketFragment())
                    supportActionBar?.title = "市场行情"
                    true
                }
                R.id.nav_settings -> {
                    loadFragment(SettingsFragment())
                    supportActionBar?.title = "设置"
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            replace(R.id.fragment_container, fragment)
        }
    }
}
