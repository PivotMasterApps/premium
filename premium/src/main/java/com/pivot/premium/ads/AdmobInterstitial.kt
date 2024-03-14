package com.pivot.premium.ads

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.pivot.premium.R
import com.pivot.premium.getConfig
import com.pivot.premium.getInterstitialAdUnit
import com.pivot.premium.sendEvent

private const val TAG = "AdmobInterstitial"
class AdmobInterstitial (
    val context: Context
) {
    private var mInterstitialAd: InterstitialAd? = null
    private var mIsLoading = false
    private var sharedPreferences: SharedPreferences =
        context.getSharedPreferences(context.getString(R.string.premium_prefs_name), Context.MODE_PRIVATE)
    private var onDismissed: (() -> Unit)? = null

    fun loadAd() {
        if(mIsLoading || mInterstitialAd != null) return

        Log.d(TAG, "Loading interstitial")
        val adRequest = AdRequest.Builder().build()
        mIsLoading = true
        InterstitialAd.load(context, getInterstitialAdUnit(), adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, "onAdFailedToLoad:${adError.message} ")
                context.sendEvent("interstitial_failed")
                mIsLoading = false
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.d(TAG, "onAdLoaded")
                context.sendEvent("interstitial_loaded")
                mIsLoading = false
                mInterstitialAd = interstitialAd
                mInterstitialAd?.fullScreenContentCallback = fullScreenContentCallback
            }
        })
    }

    val fullScreenContentCallback = object: FullScreenContentCallback() {
        override fun onAdClicked() {
            // Called when a click is recorded for an ad.
        }

        override fun onAdDismissedFullScreenContent() {
            // Called when ad is dismissed.
            mInterstitialAd = null
            loadAd()
            onDismissed?.invoke()
        }

        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
            // Called when ad fails to show.
            mInterstitialAd = null
            loadAd()
            onDismissed?.invoke()
        }

        override fun onAdImpression() {
            // Called when an impression is recorded for an ad.
            sharedPreferences.edit().putLong(PREF_LAST_SHOWN_MS, System.currentTimeMillis()).apply()
            context.sendEvent("interstitial_impression")
        }

        override fun onAdShowedFullScreenContent() {
            // Called when ad is shown.
        }
    }

    fun showAd(activity: Activity, onDismissed: (() -> Unit)? = null) {
        Log.d(TAG, "showAd called")
        this.onDismissed = onDismissed
        if(System.currentTimeMillis() - sharedPreferences.getLong(PREF_LAST_SHOWN_MS, 0) >
            getConfig(CONFIG_INTERSTITIAL_CAPPING, 30) * 1000) {
            Log.d(TAG, "showAd: Capping reached")
            if (mInterstitialAd != null) {
                Log.d(TAG, "showAd: trying to show")
                mInterstitialAd?.show(activity)
            } else {
                if(!mIsLoading) { loadAd() }
                onDismissed?.invoke()
            }
        } else {
            if(mInterstitialAd == null && !mIsLoading) { loadAd() }
            onDismissed?.invoke()
        }

    }

    companion object {
        val PREF_LAST_SHOWN_MS = "interstitial_last_shown"
        val CONFIG_INTERSTITIAL_CAPPING = "interstitial_capping_sec"
    }
}