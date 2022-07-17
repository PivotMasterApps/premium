package com.pivot.premium

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.SkuDetails

class PremiumActivity : AppCompatActivity() {

    companion object {
        val IN_APP_PRODUCT_ID = "one_time_premium"
        val SUBSCRIPTION_PRODUCT_ID = "premium_subscription"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.custom_primium_dialog)

        Premium.mBillingProcessor?.getPurchaseListingDetailsAsync(
            IN_APP_PRODUCT_ID, object : BillingProcessor.ISkuDetailsResponseListener {
                override fun onSkuDetailsResponse(products: MutableList<SkuDetails>?) {
                    findViewById<View>(R.id.premium_pb_one_time).visibility = View.GONE
                    products?.getOrNull(0)?.let {
                        findViewById<TextView>(R.id.premium_one_time).text =
                            resources.getString(R.string.premium_one_time_price, it.priceText)
                    } ?: onError()
                }

                override fun onSkuDetailsError(error: String?) {
                    onError()
                }
            })


/*        Premium.mBillingProcessor?.getSubscriptionListingDetailsAsync(
            SUBSCRIPTION_PRODUCT_ID, object : BillingProcessor.ISkuDetailsResponseListener {
                override fun onSkuDetailsResponse(products: MutableList<SkuDetails>?) {
                    findViewById<View>(R.id.premium_pb_subscription).visibility = View.GONE
                    products?.getOrNull(0)?.let {
                        if(it.haveTrialPeriod) {
                            findViewById<TextView>(R.id.premium_subscription).text =
                                resources.getString(
                                    R.string.subscription_price_trial,
                                    it.subscriptionFreeTrialPeriod[1],
                                    it.priceText,
                                    it.subscriptionPeriod[2]
                                )
                        } else {
                            findViewById<TextView>(R.id.premium_subscription).text =
                                resources.getString(
                                    R.string.subscription_price_no_trial,
                                    it.priceText,
                                    it.subscriptionPeriod[2]
                                )
                        }
                    } ?: onError()
                }

                override fun onSkuDetailsError(error: String?) {
                    onError()
                }
            })*/

        findViewById<AppCompatImageView>(R.id.premium_close).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.premium_one_time).setOnClickListener {
            Premium.mBillingProcessor?.purchase(this, IN_APP_PRODUCT_ID)
        }

//        findViewById<View>(R.id.premium_subscription).setOnClickListener {
//            Premium.mBillingProcessor?.subscribe(this, SUBSCRIPTION_PRODUCT_ID)
//        }

        findViewById<View>(R.id.continue_free_btn).setOnClickListener {
            finish()
        }

        Premium.mIsPremium.observe(this) {
            if(it == true) {
                Toast.makeText(this, getString(R.string.premium_toast_success), Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    fun onError() {
        Toast.makeText(this, getString(R.string.premium_something_wrong), Toast.LENGTH_LONG).show()
        finish()
    }
}