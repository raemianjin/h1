package com.myhub.app

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class PagerAdapter(
    activity: FragmentActivity,
    private val sites: List<SiteEntry>
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = sites.size

    override fun createFragment(position: Int): Fragment {
        val s = sites[position]
        return WebPageFragment.create(s.id, s.url)
    }

    override fun getItemId(position: Int): Long = sites[position].id.hashCode().toLong()

    override fun containsItem(itemId: Long): Boolean =
        sites.any { it.id.hashCode().toLong() == itemId }
}
