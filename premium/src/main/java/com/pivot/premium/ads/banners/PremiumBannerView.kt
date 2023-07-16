package com.pivot.premium.ads.banners

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.core.view.isVisible
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.AdView
import com.pivot.premium.Premium
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class PremiumBannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ShimmerFrameLayout(context, attrs, defStyle) {

    var adLoadingState: AdLoadingState? = null

    init {
        setShimmer(
            Shimmer.ColorHighlightBuilder()
            .setBaseColor(Color.DKGRAY)
            .setHighlightColor(Color.LTGRAY)
            .build())
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if(context !is AppCompatActivity) return
        Premium.mIsPremium.observe((context as AppCompatActivity)) {
            if(it == true) {
                isVisible = false
            } else if (it == false) {
                isVisible = true
                startLoading()
            }
        }
    }

    private fun startLoading() {
        startShimmer()
        CoroutineScope(Dispatchers.IO).launch {
            val adView = getAdView(adLoadingState)
            addView(adView)
            withContext(Dispatchers.Main) { stopShimmer() }
            hideShimmer()
        }
    }

    override fun onDetachedFromWindow() {
        children.forEach {
            if(it is AdView) {
                it.destroy()
            }
        }
        super.onDetachedFromWindow()
    }

    abstract suspend fun getAdView(adLoadingState: AdLoadingState?): View?

}