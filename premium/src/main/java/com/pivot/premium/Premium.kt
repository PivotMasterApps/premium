package com.pivot.premium

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.PurchaseInfo
import com.google.android.gms.ads.MobileAds
import com.pivot.premium.ads.AdManager
import com.pivot.premium.purchases.PremiumActivity

@SuppressLint("StaticFieldLeak")
object Premium {

    private var mContext: Context? = null
    var mBillingProcessor: BillingProcessor? = null
    val mIsPremium =  MutableLiveData(false)

    fun initialize(context: Context) {
        if(mContext != null) return

        mContext = context
        AdManager.initialize(context)
        initializeBilling()
    }

    fun showPrivacyActivity() {
        mContext?.startActivity(
            Intent(mContext, OptinActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun optinFinished() {
        //go to next activity
    }

    fun showPremium() {
        mContext?.startActivity(
            Intent(mContext, PremiumActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
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