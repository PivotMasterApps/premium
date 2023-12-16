package com.pivot.premium.billing

import android.app.Activity
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.pivot.premium.Premium
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.restorePurchasesWith
import java.lang.Exception
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class RevenueCatBillingProvider(
    val context: Context,
    val onReady: () -> Unit,
    val onStateChanged: (BillingManager.PremiumState) -> Unit
): BillingProvider(context, onReady, onStateChanged), UpdatedCustomerInfoListener {

    private var packageToPurchase: Package? = null
    override val premiumOffer = MutableLiveData<PremiumOffer?>(null)

    override fun initialize() {
        Purchases.logLevel = LogLevel.DEBUG
        val a = Purchases.configure(PurchasesConfiguration
            .Builder(context,
                Premium.mConfiguration.revenueCatAppId!!
            ).build()
        )
        onReady()
    }

    override suspend fun makePurchase(activity: Activity): PurchaseResponse {
        return suspendCoroutine { cont ->
            Purchases.sharedInstance.purchaseWith(
                PurchaseParams.Builder(activity, packageToPurchase!!).build(),
                onError = {error, userCancled -> cont.resume(PurchaseResponse.FAILURE) },
                onSuccess = {purchase, customerInfo ->
                    if(customerInfo.activeSubscriptions.isNotEmpty()) {
                        cont.resume(PurchaseResponse.SUCCESS)
                        onStateChanged(BillingManager.PremiumState.PREMIUM)
                        return@purchaseWith
                    }
                    cont.resume(PurchaseResponse.FAILURE)
                }
            )
        }
    }

    override suspend fun restorePurchase() {
        Purchases.sharedInstance.restorePurchasesWith { customerInfo ->
            if(customerInfo.activeSubscriptions.isNotEmpty()) {
                onStateChanged(BillingManager.PremiumState.PREMIUM)
            } else {
                onStateChanged(BillingManager.PremiumState.NONE)
            }
        }
    }

    override suspend fun isPremium(): BillingManager.PremiumState {
        return suspendCoroutine { cont ->
            Purchases.sharedInstance.getCustomerInfo(
                CacheFetchPolicy.CACHED_OR_FETCHED,
                object: ReceiveCustomerInfoCallback {
                    override fun onError(error: PurchasesError) {
                        cont.resume(BillingManager.PremiumState.ERROR)
                    }

                    override fun onReceived(customerInfo: CustomerInfo) {
                        if(customerInfo.activeSubscriptions.isNullOrEmpty()) {
                            cont.resume(BillingManager.PremiumState.NONE)
                        } else {
                            cont.resume(BillingManager.PremiumState.PREMIUM)
                        }
                    }
                }
            )
        }
    }

    override suspend fun fetchProduct(): PremiumOffer? {
        return suspendCoroutine { cont ->
            Purchases.sharedInstance.getOfferingsWith(
                onError = { error ->
                    /* Optional error handling */
                    cont.resume(null)
                },
                onSuccess = { offerings ->
                    if(offerings.all.isEmpty()) {
                        cont.resume(null)
                        return@getOfferingsWith
                    }
                    val offer = offerings.all.values.first()
                    offer.availablePackages.forEach { p ->
                        packageToPurchase = p
                        try {
                            val freeTrialPhrase = p.product.subscriptionOptions?.freeTrial?.freePhase
                            val billedPhrase = p.product.subscriptionOptions?.defaultOffer?.billingPeriod
                            val premiumOffer = PremiumOffer(
                                price = p.product.price.formatted,
                                freeTrialPeriodValue = freeTrialPhrase?.billingPeriod?.value ?: -1,
                                freeTrialPeriodUnit = freeTrialPhrase?.billingPeriod?.unit?.name ?: "",
                                billingPeriodValue = billedPhrase?.value!!,
                                billingPeriodUnit = billedPhrase.unit.name)
                            cont.resume(premiumOffer)
                        } catch (e: Exception) {
                            cont.resume(null)
                        }
                    }
                })
        }
    }

    override fun onReceived(customerInfo: CustomerInfo) {
        if(customerInfo.activeSubscriptions.isNotEmpty()) {
            onStateChanged(BillingManager.PremiumState.PREMIUM)
        } else {
            onStateChanged(BillingManager.PremiumState.NONE)
        }
    }
}