package com.pivot.premium.billing

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.Purchase.PurchaseState
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsParams.Product
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.pivot.premium.Premium.PREMIUM_PREFS_NAME
import com.pivot.premium.R
import com.pivot.premium.sendEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "BillingManager"
class BillingManager(
    val context: Context
) {

    var productDetailsResult: ProductDetailsResult? = null
    val preferences: SharedPreferences = context.getSharedPreferences(PREMIUM_PREFS_NAME, Context.MODE_PRIVATE)
    val mIsPremium =  MutableLiveData(PremiumState.NONE)
    val mPrice = MutableLiveData("")

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            // To be implemented in a later section.
            purchases?.forEach { purchase ->
                if(purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    acknowledgePurchases(purchase)
                    setState(PremiumState.PREMIUM)
                    context.sendEvent("trial_started")
                    return@forEach
                } else if(purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                    setState(PremiumState.PENDING)
                    return@forEach
                } else {
                    setState(PremiumState.NONE)
                }
            }
        }

    private var billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    init {
        mIsPremium.postValue(lastCachedValue())
        startConnection()
    }

    private fun setState(state: PremiumState) {
        mIsPremium.postValue(state)
        saveCachedValue(state)
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode ==  BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    CoroutineScope(Dispatchers.IO).launch {
                        fetchPurchases()
                        loadOffer()
                    }
                }
            }
            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
//                startConnection()
            }
        })
    }

    suspend fun launch(activity: Activity) {
        val productDetails = productDetailsResult?.productDetailsList?.firstOrNull() ?: return

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: "")
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        // Launch the billing flow
        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        if(billingResult.responseCode != BillingResponseCode.OK) {
            activity.finish()
            Toast.makeText(activity, "Something went wrong", Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun fetchPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)

        // uses queryPurchasesAsync Kotlin extension function
        withContext(Dispatchers.IO) {
            billingClient.queryPurchasesAsync(params.build()) { result, purchases ->
                purchases.forEach {
                    if(it.purchaseState == PurchaseState.PURCHASED) {
                        acknowledgePurchases(it)
                        return@forEach
                    } else if(it.purchaseState == PurchaseState.PENDING) {
                        setState(PremiumState.PENDING)
                        return@forEach
                    } else {
                        mIsPremium.postValue(PremiumState.NONE)
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
                        Log.d(TAG, "acknowledgePurchases: Acknowleged!")
                        context.sendEvent("trial_acknowledged")
                    } else {
                        Log.d(TAG, "acknowledgePurchases: error")
                        Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                setState(PremiumState.PREMIUM)
            }
        }
    }

    suspend fun loadOffer() {
        val productList = ArrayList<Product>()
        productList.add(
            Product.newBuilder()
                .setProductId(context.getString(R.string.premium_subscription_id))
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
        params.setProductList(productList)

        // leverage queryProductDetails Kotlin extension function
        productDetailsResult = withContext(Dispatchers.IO) {
            billingClient.queryProductDetails(params.build())
        }
        productDetailsResult?.productDetailsList?.firstOrNull()?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.let {
            mPrice.postValue(context.resources.getString(
                R.string.premium_subscription_price_trial,
                "3",
                it.formattedPrice,
                if(it.billingPeriod[2].equals('w', true)) "week" else "month")
            )
        }
    }

    private fun lastCachedValue(): PremiumState {
        return when(preferences.getInt(purchase_state_key, 0)) {
            0 -> PremiumState.NONE
            1 -> PremiumState.PREMIUM
            2 -> PremiumState.PENDING
            3 -> PremiumState.ERROR
            else -> PremiumState.NONE
        }
    }

    private fun saveCachedValue(state: PremiumState) {
        val stateInt = when(state) {
            PremiumState.NONE -> 0
            PremiumState.PREMIUM -> 1
            PremiumState.PENDING -> 2
            PremiumState.ERROR -> 3
            else -> 0
        }
        preferences.edit().putInt(purchase_state_key, stateInt).apply()
    }

    enum class PremiumState {
        NONE, PREMIUM, PENDING, ERROR
    }

    companion object {
        const val purchase_state_key = "last_purchase_state"
    }
}