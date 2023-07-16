package com.pivot.premium.ads.banners

import android.content.Context
import android.view.ViewGroup
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.pivot.premium.R
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
            mAdView?.setAdSize(if(adSize == PremiumBannerView.PremiumAdSize.BANNER) AdSize.BANNER else AdSize.MEDIUM_RECTANGLE)
            mAdView?.adUnitId = context.getString(R.string.banner_ad_unit)

            val adRequest = AdRequest.Builder().build()

            mAdView?.adListener = object: AdListener() {
                override fun onAdLoaded() {
                    // Code to be executed when an ad finishes loading.
                    if(cont.isActive) {
                        cont.resume(mAdView!!)
                    }
                }

                override fun onAdFailedToLoad(adError : LoadAdError) {
                    // Code to be executed when an ad request fails.
                    if(cont.isActive) {
                        cont.resume(null)
                    }
                }

                override fun onAdOpened() {
                    // Code to be executed when an ad opens an overlay that
                    // covers the screen.
                }

                override fun onAdClicked() {
                    // Code to be executed when the user clicks on an ad.
                }

                override fun onAdClosed() {
                    // Code to be executed when the user is about to return
                    // to the app after tapping on an ad.
                }
            }
            CoroutineScope(Dispatchers.Main).launch {
                mAdView?.loadAd(adRequest)
            }
        }
    }
}