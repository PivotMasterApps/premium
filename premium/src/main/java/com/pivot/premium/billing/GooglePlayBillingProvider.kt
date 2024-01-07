package com.pivot.premium.billing

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.pivot.premium.Premium
import com.pivot.premium.R
import com.pivot.premium.sendEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class GooglePlayBillingProvider(
    val context: Context,
    val onReady: () -> Unit,
    val onStateChanged: (BillingManager.PremiumState) -> Unit
): BillingProvider(context, onReady, onStateChanged) {

    var productDetails: ProductDetails? = null
    override val premiumOffer = MutableLiveData<PremiumOffer?>(null)

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            // To be implemented in a later section.
            purchases?.forEach { purchase ->
                if(purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    acknowledgePurchases(purchase)
                    setState(BillingManager.PremiumState.PREMIUM)
                    return@forEach
                } else if(purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                    setState(BillingManager.PremiumState.PENDING)
                    return@forEach
                } else {
                    setState(BillingManager.PremiumState.NONE)
                }
            }
        }

    private var billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    private fun setState(state: BillingManager.PremiumState) {
        onStateChanged(state)
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode ==  BillingClient.BillingResponseCode.OK) {
                    onReady()
                }
            }
            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
//                startConnection()
            }
        })
    }
    override fun initialize() {
        startConnection()
    }

    override suspend fun fetchProduct(): PremiumOffer? {
        return suspendCoroutine { cont ->
            val productList = ArrayList<QueryProductDetailsParams.Product>()
            productList.add(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(context.getString(R.string.premium_subscription_id))
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
            val params = QueryProductDetailsParams.newBuilder()
            params.setProductList(productList)

            // leverage queryProductDetails Kotlin extension function
            billingClient.queryProductDetailsAsync(params.build()) {result, details ->
                if(details.isEmpty() || details[0]?.subscriptionOfferDetails?.firstOrNull() == null) {
                    cont.resume(null)
                } else {
                    productDetails = details[0]
                    val subscriptionOfferDetails = productDetails?.subscriptionOfferDetails?.first()!!
                    val phases = subscriptionOfferDetails.pricingPhases.pricingPhaseList
                    val freePhase = phases.firstOrNull { it.priceAmountMicros == 0L }
                    val billedPhase = phases.first { it.priceAmountMicros > 0L }
                    cont.resume(
                        PremiumOffer(
                            price = billedPhase.formattedPrice,
                            freeTrialPeriodValue = getValueFromPeriod(freePhase?.billingPeriod)?.toInt() ?: -1,
                            freeTrialPeriodUnit = getUnitFromPeriod(freePhase?.billingPeriod) ?: "",
                            billingPeriodValue = getValueFromPeriod(billedPhase.billingPeriod)!!.toInt(),
                            billingPeriodUnit = getUnitFromPeriod(billedPhase.billingPeriod)!!
                        )
                    )
                }
            }
        }
    }

    private fun getValueFromPeriod(periodFormat: String?): String? {
        if(periodFormat == null) return null
        return periodFormat[1].toString()
    }

    private fun getUnitFromPeriod(periodFormat: String?): String? {
        if(periodFormat == null) return null
        return when(periodFormat[2]) {
            'D' -> "day"
            'W' -> "week"
            'M' -> "month"
            'Y' -> "year"
            else -> throw IllegalArgumentException()
        }
    }

    override suspend fun makePurchase(activity: Activity): PurchaseResponse {
        return suspendCoroutine { cont ->
            if(productDetails == null) {
                cont.resume(PurchaseResponse.FAILURE)
                return@suspendCoroutine
            }

            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails!!)
                    .setOfferToken(productDetails!!.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: "")
                    .build()
            )

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            // Launch the billing flow
            val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
            if(billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                cont.resume(PurchaseResponse.FAILURE)
            } else {
                cont.resume(PurchaseResponse.SUCCESS)
            }
        }
    }

    override suspend fun isPremium(): BillingManager.PremiumState? {
        return suspendCoroutine { cont ->
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)

            // uses queryPurchasesAsync Kotlin extension function
            billingClient.queryPurchasesAsync(params.build()) { result, purchases ->
                purchases.forEach {
                    if(it.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        cont.resume(BillingManager.PremiumState.PREMIUM)
                        return@forEach
                    } else if(it.purchaseState == Purchase.PurchaseState.PENDING) {
                        cont.resume(BillingManager.PremiumState.PENDING)
                        return@forEach
                    } else {
                        cont.resume(BillingManager.PremiumState.NONE)
                    }
                }
            }
        }
    }

    override suspend fun restorePurchase() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)

        withContext(Dispatchers.IO) {
            billingClient.queryPurchasesAsync(params.build()) { result, purchases ->
                purchases.forEach {
                    if(it.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        acknowledgePurchases(it)
                        return@forEach
                    } else if(it.purchaseState == Purchase.PurchaseState.PENDING) {
                        setState(BillingManager.PremiumState.PENDING)
                        return@forEach
                    } else {
                        setState(BillingManager.PremiumState.NONE)
                    }
                }
            }
        }
    }

    // Perform new subscription purchases' acknowledgement client side.
    private fun acknowledgePurchases(purchase: Purchase?) {
        purchase?.let {
            if (!it.isAcknowledged) {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(it.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(
                    params
                ) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
                        it.purchaseState == Purchase.PurchaseState.PURCHASED
                    ) {
                        context.sendEvent("trial_acknowledged")
                    } else {
                        Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                setState(BillingManager.PremiumState.PREMIUM)
            }
        }
    }

    companion object {
        const val purchase_state_key = "last_purchase_state"
    }

}