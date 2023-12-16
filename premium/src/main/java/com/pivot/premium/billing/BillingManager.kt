package com.pivot.premium.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.pivot.premium.Premium
import com.pivot.premium.sendEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BillingManager(
    val context: Context
) {

    val mIsPremium = MutableLiveData(PremiumState.NONE)
    private lateinit var provider: BillingProvider

    private val onProviderReady: () -> Unit = {
        CoroutineScope(Dispatchers.IO).launch {
            provider.restorePurchase()
            provider.premiumOffer.postValue(provider.fetchProduct())
            mIsPremium.postValue(provider.isPremium())
        }
    }

    private val onStateChanged = { state: PremiumState ->
        mIsPremium.postValue(state)
        provider.saveCachedValue(state)
    }

    init {
        provider = if (Premium.mConfiguration.revenueCatAppId.isNullOrEmpty()) {
            GooglePlayBillingProvider(context, onProviderReady, onStateChanged)
        } else {
            RevenueCatBillingProvider(context, onProviderReady, onStateChanged)
        }
        mIsPremium.postValue(provider.lastCachedValue())
        provider.initialize()
    }

    fun premiumOffer() = provider.premiumOffer

    suspend fun launch(activity: Activity): PurchaseResponse {
        val result = provider.makePurchase(activity)
        if(result == PurchaseResponse.SUCCESS) {
            context.sendEvent("trial_started")
        }
        return result
    }

    enum class PremiumState {
        NONE, PREMIUM, PENDING, ERROR
    }
}