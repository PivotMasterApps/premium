package com.pivot.premium.purchases

import androidx.lifecycle.ViewModel
import com.pivot.premium.Premium

class PremiumViewModel(
) : ViewModel() {
    fun isPremium() = Premium.mIsPremium
}