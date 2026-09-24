package com.mahanshengzhi.indonesianengineer

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val home = TextView(this).apply {
            text = "印尼语工程员学习\n\n每天 15 分钟，学会真正能在印尼现场说出来的话。"
            setTextColor(getColor(R.color.text_primary))
            textSize = 22f
            setPadding(48, 72, 48, 24)
        }
        findViewById<android.widget.FrameLayout>(R.id.contentContainer).addView(home)
    }
}
