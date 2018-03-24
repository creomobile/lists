package com.creomobile.lists.sample

import android.app.Activity
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v7.app.AppCompatActivity
import android.view.inputmethod.InputMethodManager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val adapter = ViewPagerAdapter(supportFragmentManager)
        adapter.addFragment(ListFragment.newInstance(), getString(R.string.list))
        adapter.addFragment(ExpandableFragment.newInstance(), getString(R.string.expandable))
        viewPager.adapter = adapter
        tabLayout.setupWithViewPager(viewPager)

        val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            private fun unfocus() {
                val view = currentFocus
                if (view != null)
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                dummyFocus.requestFocus()
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                unfocus()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabSelected(tab: TabLayout.Tab?) {
                unfocus()
            }
        })
    }
}
