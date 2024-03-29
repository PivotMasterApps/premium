package com.pivot.premium.ads.banners

sealed class AdLoadingState {
    data class Loading(val data: DFPBannerLoader) : AdLoadingState()
    data class Success(val data: DFPBannerLoader) : AdLoadingState()
    data class Error(val error: String?) : AdLoadingState()
}