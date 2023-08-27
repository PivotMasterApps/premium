package com.pivot.premium.purchases

import androidx.lifecycle.ViewModel
import com.pivot.premium.Premium
import com.pivot.premium.billing.BillingManager

class PremiumViewModel(
) : ViewModel() {
    fun isPremium() = Premium.mBillingManager?.mIsPremium?.value == BillingManager.PremiumState.PREMIUM
}