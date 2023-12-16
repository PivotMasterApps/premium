package com.pivot.premium.billing

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import com.pivot.premium.Premium

abstract class BillingProvider(
    context: Context,
    onReady: () -> Unit,
    onStateChanged: (BillingManager.PremiumState) -> Unit
) {
    abstract val premiumOffer: MutableLiveData<PremiumOffer?>
    private val preferences: SharedPreferences = context.getSharedPreferences(Premium.PREMIUM_PREFS_NAME, Context.MODE_PRIVATE)

    abstract fun initialize()
    abstract suspend fun fetchProduct(): PremiumOffer?
    abstract suspend fun makePurchase(activity: Activity): PurchaseResponse
    abstract suspend fun isPremium(): BillingManager.PremiumState?
    abstract suspend fun restorePurchase()

    fun lastCachedValue(): BillingManager.PremiumState {
        return when(preferences.getInt(GooglePlayBillingProvider.purchase_state_key, 0)) {
            0 -> BillingManager.PremiumState.NONE
            1 -> BillingManager.PremiumState.PREMIUM
            2 -> BillingManager.PremiumState.PENDING
            3 -> BillingManager.PremiumState.ERROR
            else -> BillingManager.PremiumState.NONE
        }
    }

    fun saveCachedValue(state: BillingManager.PremiumState) {
        val stateInt = when(state) {
            BillingManager.PremiumState.NONE -> 0
            BillingManager.PremiumState.PREMIUM -> 1
            BillingManager.PremiumState.PENDING -> 2
            BillingManager.PremiumState.ERROR -> 3
            else -> 0
        }
        preferences.edit().putInt(GooglePlayBillingProvider.purchase_state_key, stateInt).apply()
    }
}