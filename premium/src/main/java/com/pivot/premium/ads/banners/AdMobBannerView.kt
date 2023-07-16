package com.pivot.premium.ads.banners

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout

class AdMobBannerView @JvmOverloads constructor (
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
): PremiumBannerView(context, attrs, defStyle) {
    override suspend fun getAdView(adLoadingState: AdLoadingState?): View? {
        return FrameLayout(context).apply {
            setBackgroundColor(Color.RED)
        }
    }
}