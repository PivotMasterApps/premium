package com.pivot.premium.billing

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsParams.Product
import com.android.billingclient.api.queryProductDetails
import com.pivot.premium.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BillingManager(
    val context: Context
) {

    var productDetailsResult: ProductDetailsResult? = null
    val mIsPremium =  MutableLiveData(PremiumState.NONE)
    val mPrice = MutableLiveData("")

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            // To be implemented in a later section.
            purchases?.forEach { purchase ->
                if(purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    mIsPremium.postValue(PremiumState.PREMIUM)
                    return@forEach
                } else if(purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                    mIsPremium.postValue(PremiumState.PENDING)
                    return@forEach
                } else {
                    mIsPremium.postValue(PremiumState.NONE)
                }
            }
        }

    private var billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    init {
        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode ==  BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    CoroutineScope(Dispatchers.IO).launch { loadOffer() }
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
        val productDetails = productDetailsResult?.productDetailsList?.get(0) ?: return

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
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
        productDetailsResult?.productDetailsList?.get(0)?.subscriptionOfferDetails?.get(0)?.pricingPhases?.pricingPhaseList?.get(0)?.let {
            mPrice.postValue(context.resources.getString(
                R.string.premium_subscription_price_trial,
                "3",
                it.formattedPrice,
                it.billingPeriod
                )
            )
        }
    }

    enum class PremiumState {
        NONE, PREMIUM, PENDING
    }
}