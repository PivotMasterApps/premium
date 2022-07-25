package com.pivot.premium.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.pivot.premium.ads.banners.DFPBannerLoader

private const val TAG = "AdManager"

class AdManager private constructor(
    val context: Context
) {
    private var interstitials: AdmobInterstitial = AdmobInterstitial(context)
    private var rewardedAds: RewardedLoader = RewardedLoader(context)

    fun initialize() {
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

    companion object : SingletonHolder<AdManager, Context>(::AdManager)
}