package com.pivot.premium.ads.banners

import android.content.Context
import android.content.res.Resources
import android.view.ViewGroup
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.pivot.premium.R
import com.pivot.premium.getBannersAdUnit
import com.pivot.premium.sendEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class AdMobAdLoader(val context: Context) {

    var mAdView: AdView? = null

    suspend fun loadAd(adSize: PremiumBannerView.PremiumAdSize): ViewGroup? {
        return suspendCancellableCoroutine { cont ->
            mAdView = AdView(context)
            mAdView?.setAdSize(if(adSize == PremiumBannerView.PremiumAdSize.BANNER) getAnchoredSize() else AdSize.MEDIUM_RECTANGLE)
            mAdView?.adUnitId = getBannersAdUnit()

            val adRequest = AdRequest.Builder().build()

            mAdView?.adListener = object: AdListener() {
                override fun onAdLoaded() {
                    if(cont.isActive) {
                        cont.resume(mAdView!!)
                    }
                }

                override fun onAdFailedToLoad(adError : LoadAdError) {
                    if(cont.isActive) {
                        cont.resume(null)
                    }
                }

                override fun onAdImpression() {
                    context.sendEvent("paid_ad_impression")
                }

                override fun onAdOpened() {
                }

                override fun onAdClicked() {
                }

                override fun onAdClosed() {
                }
            }
            CoroutineScope(Dispatchers.Main).launch {
                mAdView?.loadAd(adRequest)
            }
        }
    }

    private fun getAnchoredSize(): AdSize {
        val density = Resources.getSystem().displayMetrics.density
        val adWidth = (Resources.getSystem().displayMetrics.widthPixels / density).toInt()

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
    }
}