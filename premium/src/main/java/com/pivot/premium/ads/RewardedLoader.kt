package com.pivot.premium.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

private const val TAG = "RewardedLoader"
class RewardedLoader(
    val mContext: Context
) {

    private var mIsLoading = false
    private var mRewardedAd: RewardedAd? = null
    private var mOnDismissed: (() -> Unit)? = null

    fun loadAd() {
        var adRequest = AdRequest.Builder().build()

        RewardedAd.load(mContext,"ca-app-pub-7194429227078261/9122382132", adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, adError.toString())
                mIsLoading = false
                mRewardedAd = null
            }

            override fun onAdLoaded(rewardedAd: RewardedAd) {
                Log.d(TAG, "Ad was loaded.")
                mRewardedAd = rewardedAd
                mIsLoading = false
                mRewardedAd?.fullScreenContentCallback = fullScreenContentCallback
            }
        })
    }

    fun showAd(activity: Activity, onDismissed: (Boolean) -> Unit) {
        if (mRewardedAd != null) {
            mRewardedAd?.show(activity) {
                onDismissed.invoke(true)
            } ?: onDismissed.invoke(false)
        } else {
            Log.d(TAG, "The rewarded ad wasn't ready yet.")
            onDismissed.invoke(false)
        }
    }


    val fullScreenContentCallback = object: FullScreenContentCallback() {
        override fun onAdClicked() {
            // Called when a click is recorded for an ad.
            Log.d(TAG, "Ad was clicked.")
        }

        override fun onAdDismissedFullScreenContent() {
            // Called when ad is dismissed.
            // Set the ad reference to null so you don't show the ad a second time.
            Log.d(TAG, "Ad dismissed fullscreen content.")
            mRewardedAd = null
            loadAd()
        }

        override fun onAdFailedToShowFullScreenContent(p0: AdError) {
            super.onAdFailedToShowFullScreenContent(p0)
            mRewardedAd = null
            loadAd()
        }

        override fun onAdImpression() {
            // Called when an impression is recorded for an ad.
            Log.d(TAG, "Ad recorded an impression.")
        }

        override fun onAdShowedFullScreenContent() {
            // Called when ad is shown.
            Log.d(TAG, "Ad showed fullscreen content.")
        }
    }

}