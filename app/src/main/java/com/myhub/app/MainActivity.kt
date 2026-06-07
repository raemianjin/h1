package com.myhub.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity(), WebPageFragment.PageHost {

    private lateinit var pager: ViewPager2
    private lateinit var tabs: TabLayout
    private lateinit var progress: ProgressBar
    private lateinit var title: TextView
    private lateinit var empty: View

    private var sites: List<SiteEntry> = emptyList()
    private var loadedSignature: String = "__none__"
    private var mediator: TabLayoutMediator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pager = findViewById(R.id.pager)
        tabs = findViewById(R.id.tabs)
        progress = findViewById(R.id.progress)
        title = findViewById(R.id.title)
        empty = findViewById(R.id.empty)

        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener { openSettings() }
        findViewById<View>(R.id.btnEmptyAdd).setOnClickListener { openSettings() }
        findViewById<ImageButton>(R.id.btnReload).setOnClickListener { currentFragment()?.reload() }

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                UrlStore.setLastPage(this@MainActivity, position)
                updateTitle(position)
                progress.visibility = View.GONE
            }
        })

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val f = currentFragment()
                if (f != null && f.canGoBack()) {
                    f.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        // 설정 화면에서 돌아왔을 때 변경 사항을 반영. 동일하면 재구성하지 않는다.
        val json = UrlStore.loadJson(this)
        if (json != loadedSignature) {
            loadedSignature = json
            rebuild()
        }
    }

    private fun rebuild() {
        sites = UrlStore.load(this)

        if (sites.isEmpty()) {
            empty.visibility = View.VISIBLE
            pager.visibility = View.GONE
            tabs.visibility = View.GONE
            title.text = "myHub"
            pager.adapter = null
            return
        }

        empty.visibility = View.GONE
        pager.visibility = View.VISIBLE

        pager.adapter = PagerAdapter(this, sites)
        pager.offscreenPageLimit = 1

        mediator?.detach()
        if (sites.size > 1) {
            tabs.visibility = View.VISIBLE
            mediator = TabLayoutMediator(tabs, pager) { tab, pos ->
                tab.text = sites[pos].label
            }.also { it.attach() }
        } else {
            tabs.visibility = View.GONE
        }

        val start = UrlStore.lastPage(this).coerceIn(0, sites.size - 1)
        pager.setCurrentItem(start, false)
        updateTitle(start)
    }

    private fun updateTitle(position: Int) {
        title.text = sites.getOrNull(position)?.label ?: "myHub"
    }

    private fun currentFragment(): WebPageFragment? {
        if (sites.isEmpty()) return null
        val adapter = pager.adapter as? PagerAdapter ?: return null
        val itemId = adapter.getItemId(pager.currentItem)
        return supportFragmentManager.findFragmentByTag("f$itemId") as? WebPageFragment
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    // ---- PageHost ----
    override fun onPageProgress(id: String, p: Int) {
        if (sites.getOrNull(pager.currentItem)?.id != id) return
        if (p in 1..99) {
            progress.visibility = View.VISIBLE
            progress.progress = p
        } else {
            progress.visibility = View.GONE
        }
    }

    override fun onPageTitle(id: String, t: String) {
        // 탭/타이틀은 사용자가 지정한 라벨을 우선 사용하므로 별도 처리 없음.
    }
}
