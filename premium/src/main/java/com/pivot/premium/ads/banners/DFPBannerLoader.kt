package com.pivot.premium.ads.banners

import android.content.Context
import android.content.res.Resources
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.ads.*

class DFPBannerLoader(
    private val context: Context
) {

    private val adState = MutableLiveData<AdLoadingState?>(AdLoadingState.Loading(this))
    var mAdView: AdView? = null
    // Determine the screen width (less decorations) to use for the ad width.
    // If the ad hasn't been laid out, default to the full screen width.
    companion object {
        fun adaptiveAnchoredAdSize(context: Context) =
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, Resources.getSystem().displayMetrics.widthPixels.dp)

        fun adapiveInlineAdSize(context: Context) =
            AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(context, Resources.getSystem().displayMetrics.widthPixels.dp) //For inline adaptive
    }

    fun getAdView(): MutableLiveData<AdLoadingState?> {
        return adState
    }

    fun loadAd(adSize: AdSize): MutableLiveData<AdLoadingState?> {
        adState.postValue(AdLoadingState.Loading(this))
        mAdView = AdView(context)
        mAdView?.setAdSize(adSize)
        mAdView?.adUnitId = "ca-app-pub-7194429227078261/6473502250"

        val adRequest = AdRequest.Builder().build()

        mAdView?.adListener = adListener
        mAdView?.loadAd(adRequest)

        return adState
    }

    private val adListener = object: AdListener() {
        override fun onAdLoaded() {
            // Code to be executed when an ad finishes loading.
            adState.postValue(AdLoadingState.Success(this@DFPBannerLoader))
        }

        override fun onAdFailedToLoad(adError : LoadAdError) {
            // Code to be executed when an ad request fails.
            adState.postValue(AdLoadingState.Error(adError.message))
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

}

val Int.dp: Int get() = (this / Resources.getSystem().displayMetrics.density).toInt()