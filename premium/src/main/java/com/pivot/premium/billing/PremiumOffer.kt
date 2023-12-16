package com.pivot.premium.billing

data class PremiumOffer(
    val price: String,
    val freeTrialPeriodUnit: String,
    val freeTrialPeriodValue: Int,
    val billingPeriodUnit: String,
    val billingPeriodValue: Int
) {
    fun formattedFreeUnit(): String {
        return when(freeTrialPeriodUnit) {
            "DAY", "day" -> "days"
            "MONTH", "month" -> "months"
            "WEEK", "week" -> "weeks"
            "YEAR", "year" -> "year"
            else -> throw IllegalArgumentException("Invalid unit format")
        }
    }
    fun formattedBilledUnit(): String {
        return when(billingPeriodUnit) {
            "DAY", "day" -> "day"
            "MONTH", "month" -> "month"
            "WEEK", "week" -> "week"
            "YEAR", "year" -> "year"
            else -> throw IllegalArgumentException("Invalid unit format")
        }
    }
}
