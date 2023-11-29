package com.pivot.premium

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@SuppressLint("CustomSplashScreen")
open class PremiumSplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(0, 0)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_premium_splash)
        setFullScreen()
        startAnimation()

        findViewById<TextView>(R.id.premium_splash_name).text =
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName,0))

        findViewById<ImageView>(R.id.premium_splash_ic).background = applicationInfo.loadIcon(packageManager)

        CoroutineScope(Dispatchers.Main).launch {
            delay(getConfig("splash_delay", 3000))
            Premium.splashFinished()
            finish()
        }
    }

    private fun startAnimation() {
        val animation1 = AlphaAnimation(0.0f, 1.0f)
        animation1.setDuration(1500)
        animation1.setFillAfter(true)
        findViewById<TextView>(R.id.premium_splash_name).startAnimation(animation1);
        findViewById<ProgressBar>(R.id.splash_pb).startAnimation(animation1);
    }

    private fun setFullScreen() {
//Set full screen after setting layout content
        @Suppress("DEPRECATION")
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController

            if(controller != null) {
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
            }
        } else {
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }
}