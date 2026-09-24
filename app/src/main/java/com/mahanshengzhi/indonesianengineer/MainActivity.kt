package com.mahanshengzhi.indonesianengineer

import android.os.Bundle
import android.os.SystemClock
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var contentContainer: FrameLayout
    private lateinit var bottomNavigation: BottomNavigationView
    private var lastBackAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        contentContainer = findViewById(R.id.contentContainer)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        bottomNavigation.inflateMenu(R.menu.bottom_nav)
        bottomNavigation.setOnItemSelectedListener {
            renderPage(it.itemId)
            true
        }

        bottomNavigation.selectedItemId = R.id.nav_home

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (bottomNavigation.selectedItemId != R.id.nav_home) {
                    bottomNavigation.selectedItemId = R.id.nav_home
                    return
                }
                val now = SystemClock.elapsedRealtime()
                if (now - lastBackAt < 1800L) {
                    finish()
                } else {
                    lastBackAt = now
                    Toast.makeText(this@MainActivity, "再按一次退出软件", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun renderPage(itemId: Int) {
        contentContainer.removeAllViews()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(18))
        }

        val title = TextView(this).apply {
            text = when (itemId) {
                R.id.nav_home -> "今天学什么？"
                R.id.nav_learn -> "学习"
                R.id.nav_practice -> "练习"
                R.id.nav_translate -> "翻译"
                R.id.nav_profile -> "我的"
                else -> "印尼语工程员学习"
            }
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            textSize = 28f
            includeFontPadding = true
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = when (itemId) {
                R.id.nav_home -> "每天 15 分钟，先听懂，再开口"
                R.id.nav_learn -> "发音 · 词汇 · 句型 · 场景"
                R.id.nav_practice -> "闪卡 · 10 题挑战 · 错题复习"
                R.id.nav_translate -> "中文 ↔ 印尼语 · 离线可用"
                R.id.nav_profile -> "学习进度 · 连续学习 · 徽章"
                else -> ""
            }
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            textSize = 16f
            includeFontPadding = true
            setPadding(0, dp(8), 0, dp(20))
        }
        root.addView(subtitle)

        if (itemId == R.id.nav_home) {
            addHomePlan(root)
        } else {
            val body = TextView(this).apply {
                text = "这一页的完整教学内容将在阶段 2 接入。阶段 1 只建立稳定的数据、音频、进度和导航基础。"
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                textSize = 17f
                includeFontPadding = true
                setLineSpacing(0f, 1.2f)
            }
            root.addView(body)
        }

        contentContainer.addView(root)
    }

    private fun addHomePlan(root: LinearLayout) {
        val plan = TextView(this).apply {
            text = "今日目标\n\n10 个新词\n5 个句型\n1 个真实场景\n10 题挑战"
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            textSize = 18f
            includeFontPadding = true
            setLineSpacing(0f, 1.18f)
            setPadding(dp(20), dp(20), dp(20), dp(20))
            setBackgroundColor(ContextCompat.getColor(context, R.color.teal_light))
        }
        root.addView(plan)

        val progress = TextView(this).apply {
            text = "今日进度\n词汇 0 / 10    句型 0 / 5\n场景 0 / 1    测试 0 / 1"
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            textSize = 16f
            includeFontPadding = true
            setLineSpacing(0f, 1.25f)
            setPadding(dp(20), dp(24), dp(20), dp(24))
        }
        root.addView(progress)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
