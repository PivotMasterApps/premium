package com.pivot.premium

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.PurchaseInfo
import com.google.android.gms.ads.MobileAds
import com.pivot.premium.ads.AdManager
import com.pivot.premium.purchases.PremiumActivity
import com.pivot.premium.rating.RatingDialog
import com.suddenh4x.ratingdialog.AppRating
import com.suddenh4x.ratingdialog.preferences.MailSettings
import com.suddenh4x.ratingdialog.preferences.RatingThreshold

@SuppressLint("StaticFieldLeak")
object Premium {

    private var mContext: Context? = null
    lateinit  var mMainActivity: Class<out Activity>
    var mBillingProcessor: BillingProcessor? = null
    val mIsPremium =  MutableLiveData(false)
    private var mIsInAppOpenFlow = true
    private var whatToShowOnMainActivity = WhatToShow.NONE

    enum class WhatToShow {NONE, INTERSTITIAL, RATING}

    fun initialize(
        context: Context,
        mainActivity: Class<out Activity>
    ) {
        if(mContext != null) return

        mMainActivity = mainActivity
        mContext = context
        initializeLifecycle()
        AdManager.initialize(context)
        initializeBilling()
    }

    fun test() {

    }

    fun initializeLifecycle() {
        (mContext as Application).registerActivityLifecycleCallbacks(object:
            Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, p1: Bundle?) {
                if(mMainActivity.simpleName == activity::class.java.simpleName) {
                    if(whatToShowOnMainActivity == WhatToShow.RATING) {
                        showRateUs(activity as AppCompatActivity)
                    } else if(whatToShowOnMainActivity == WhatToShow.INTERSTITIAL) {
                        showInterstitial(activity)
                    }
                    whatToShowOnMainActivity = WhatToShow.NONE
                }
                mIsInAppOpenFlow = false
            }
            override fun onActivityStarted(p0: Activity) {}
            override fun onActivityResumed(p0: Activity) {}
            override fun onActivityPaused(p0: Activity) {}
            override fun onActivityStopped(p0: Activity) {}
            override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {}
            override fun onActivityDestroyed(p0: Activity) {}
        })
    }

    fun showPremium() {
        mContext?.startActivity(
            Intent(mContext, PremiumActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun premiumFinished() {
        //Go to apps main activity
        if(mIsInAppOpenFlow) {
            mContext?.startActivity(
                Intent(mContext!!, mMainActivity).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }

        mIsInAppOpenFlow = false
    }

    fun showPrivacyActivity() {
        mContext?.startActivity(
            Intent(mContext, OptinActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun optinFinished() {
        if(mIsPremium.value == true) {
            premiumFinished()
            return
        }

        PreferenceManager.getDefaultSharedPreferences(mContext!!).apply {
            val opens = getInt("app_opens", 0)
            if(listOf(0,2,5).contains(opens)) {
                showPremium()
            } else if( opens % 3 == 0 &&
                !AppRating.isDialogAgreed(mContext!!) &&
                !AppRating.wasNeverButtonClicked(mContext!!)) {
                whatToShowOnMainActivity = WhatToShow.RATING
                premiumFinished()
            } else {
                whatToShowOnMainActivity = WhatToShow.INTERSTITIAL
                premiumFinished()
            }
            edit().putInt("app_opens", opens + 1).apply()
        }
    }

    fun showRateUs(activity: AppCompatActivity) {
        AppRating
                .Builder(activity)
                .setRatingThreshold(RatingThreshold.FOUR_AND_A_HALF)
                .showRateNeverButtonAfterNTimes(R.string.rate_never, null, 2)
                .setCancelable(true)
                .setMailSettingsForFeedbackDialog(
                    MailSettings(
                        mailAddress = "pivotmasterapps@gmail.com",
                        subject = "Issue tracker",
                        text = "Describe your issue..",
                        errorToastMessage = "No email address found."
                    )
                )
                .showNow()
    }

    fun onShowRateUsFinished(activity: Activity) {
        showInterstitial(activity) {
        }
    }

    fun splashFinished() {
        if(OptinActivity.isAccepted(mContext!!)) {
            optinFinished()
        } else if(!OptinActivity.isAccepted(mContext!!)){
            showPrivacyActivity()
        } else {
            optinFinished()
        }
    }

    fun showRewarded(activity: Activity, onDismissed: (Boolean) -> Unit) {
        AdManager.showRewarded(activity, onDismissed)
    }

    fun showInterstitial(activity: Activity, onDismissed: (() -> Unit)? = null) {
        AdManager.showInterstitial(activity, onDismissed)
    }

    private fun updateBillingState() {
        val subscribed = mBillingProcessor?.isSubscribed(PremiumActivity.SUBSCRIPTION_PRODUCT_ID) == true
        val purchased = mBillingProcessor?.isPurchased(PremiumActivity.IN_APP_PRODUCT_ID) == true
        val premium = subscribed || purchased
        mIsPremium.postValue(premium)
    }

    private fun initializeBilling() {
        mBillingProcessor =
            BillingProcessor(
                mContext,
                mContext?.getString(R.string.premium_gpc_key),
                billingProcessorHandler
            )
    }

    private val billingProcessorHandler: BillingProcessor.IBillingHandler = object :
        BillingProcessor.IBillingHandler {
        override fun onBillingInitialized() {
            mBillingProcessor?.loadOwnedPurchasesFromGoogleAsync(object : BillingProcessor.IPurchasesResponseListener {
                override fun onPurchasesSuccess() {
                    updateBillingState()
                }

                override fun onPurchasesError() {
                    updateBillingState()
                }
            })
        }

        override fun onProductPurchased(productId: String, details: PurchaseInfo?) {
            mIsPremium.postValue(true)
        }

        override fun onPurchaseHistoryRestored() {}

        override fun onBillingError(errorCode: Int, error: Throwable?) {
            mIsPremium.postValue(false)
        }
    }
}