package com.pivot.premium.ads

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener
import com.google.firebase.FirebaseApp
import com.pivot.premium.Premium
import com.pivot.premium.ads.banners.DFPBannerLoader

private const val TAG = "AdManager"
@SuppressLint("StaticFieldLeak")
object AdManager {

    private var mContext: Context? = null
    lateinit var interstitials: AdmobInterstitial
    lateinit var rewardedAds: RewardedLoader
    private var initialized = false

    suspend fun initialize(context: Context) {
        mContext = context
        MobileAds.initialize(context) {
            interstitials = AdmobInterstitial(context)
            interstitials.loadAd()
            if (Premium.mConfiguration.enableRewarded) {
                rewardedAds = RewardedLoader(context)
                rewardedAds.loadAd()
            }
            initialized = true
        }
    }

    fun showRewarded(activity: Activity, onDismissed: (Boolean) -> Unit) {
        if(!initialized) return

        if(Premium.mConfiguration.enableRewarded) {
            rewardedAds.showAd(activity, onDismissed)
        } else {
            Log.w(TAG, "showRewarded: You need to enable rewarded ads first", )
        }
    }

    fun showInterstitial(activity: Activity, onDismissed: (() -> Unit)? = null) {
        if(!initialized) return
        interstitials.showAd(activity, onDismissed)
    }

    fun loadBanner(context: Context, adSize: AdSize) {
        if(!initialized) return
        DFPBannerLoader(context).loadAd(adSize)
    }
}