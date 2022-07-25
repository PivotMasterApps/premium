package com.pivot.premium.ads

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.pivot.premium.ads.banners.DFPBannerLoader

private const val TAG = "AdManager"
@SuppressLint("StaticFieldLeak")
object AdManager {

    private var mContext: Context? = null
    lateinit var interstitials: AdmobInterstitial
    lateinit var rewardedAds: RewardedLoader

    fun initialize(context: Context) {
        mContext = context
        MobileAds.initialize(context)
        interstitials = AdmobInterstitial(context)
        rewardedAds = RewardedLoader(context)
        interstitials.loadAd()
        rewardedAds.loadAd()
    }

    fun showRewarded(activity: Activity, onDismissed: (Boolean) -> Unit) {
        rewardedAds.showAd(activity, onDismissed)
    }

    fun showInterstitial(activity: Activity, onDismissed: (() -> Unit)? = null) {
        interstitials.showAd(activity, onDismissed)
    }

    fun loadBanner(context: Context, adSize: AdSize) {
        DFPBannerLoader(context).loadAd(adSize)
    }
}