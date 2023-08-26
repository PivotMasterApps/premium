package com.pivot.premium.ads.banners

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.pivot.premium.Premium
import com.pivot.premium.R
import com.pivot.premium.getConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PremiumBannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ShimmerFrameLayout(context, attrs, defStyle) {

    var adSize: Int = 0
    init {
        setShimmer(
            Shimmer.ColorHighlightBuilder()
            .setBaseColor(Color.DKGRAY)
            .setHighlightColor(Color.LTGRAY)
            .build())
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.PremiumBannerView)
        adSize = typedArray.getInt(R.styleable.PremiumBannerView_ad_size, 0)
        typedArray.recycle()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if(context !is AppCompatActivity) return
        updateLayoutParams {
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            minimumHeight = getPremiumMinHeight()
        }
        Premium.mIsPremium.observe((context as AppCompatActivity)) {
            if(it == true) {
                isVisible = false
            } else if (it == false) {
                updateLayoutParams { height = ViewGroup.LayoutParams.WRAP_CONTENT }
                isVisible = true
                startLoading()
            }
        }
    }

    private fun shimmer() : View {
        val shimmerView = View(context)
        shimmerView.background = ColorDrawable(Color.LTGRAY)
        addView(shimmerView, LayoutParams(LayoutParams.MATCH_PARENT, getPremiumMinHeight()))
        return shimmerView
    }

    fun getPremiumMinHeight(): Int {
        return if(PremiumAdSize.values().get(adSize) == PremiumAdSize.MRECT) 250.px else 70.px
    }

    private fun startLoading() {
        startShimmer()
        val shimmer = shimmer()
        CoroutineScope(Dispatchers.IO).launch {
            val adView = getAdView()
            withContext(Dispatchers.Main) {
                if(adView == null) {
                    isVisible = false
                    return@withContext
                }
                isVisible = true
                removeView(shimmer)
                addView(adView)
                stopShimmer()
                hideShimmer()
            }
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

    suspend fun getAdView(): ViewGroup? {
        if(getConfig("ad_provider", "admob") == "admob") {
            return AdMobAdLoader(context).loadAd(PremiumAdSize.values().get(adSize))
        } else {
            return null
        }
    }

    enum class PremiumAdSize {
        BANNER, MRECT
    }

}