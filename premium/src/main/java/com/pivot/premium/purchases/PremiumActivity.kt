package com.pivot.premium.purchases

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.SkuDetails
import com.pivot.premium.BuildConfig
import com.pivot.premium.Premium
import com.pivot.premium.R

class PremiumActivity : AppCompatActivity() {

    companion object {
        val IN_APP_PRODUCT_ID = "one_time_premium"
        val SUBSCRIPTION_PRODUCT_ID = "premium_subscription"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.custom_primium_dialog)

        if(BuildConfig.DEBUG) {
            findViewById<ProgressBar>(R.id.premium_pb_one_time).visibility = View.GONE
            findViewById<TextView>(R.id.pricing_text).text =
                resources.getString(R.string.premium_subscription_price_trial, "3", "KR 20", "month")
        }


        Premium.mBillingProcessor?.getSubscriptionListingDetailsAsync(
            SUBSCRIPTION_PRODUCT_ID, object : BillingProcessor.ISkuDetailsResponseListener {
                override fun onSkuDetailsResponse(products: MutableList<SkuDetails>?) {
                    findViewById<View>(R.id.premium_pb_one_time).visibility = View.GONE
                    products?.getOrNull(0)?.let {
                        if(it.haveTrialPeriod) {
                            findViewById<TextView>(R.id.pricing_text).text =
                                resources.getString(
                                    R.string.premium_subscription_price_trial,
                                    it.subscriptionFreeTrialPeriod[1],
                                    it.priceText,
                                    if(it.subscriptionPeriod[2].equals('w', true)) "week" else "month"
                                )
                        } else {
                            findViewById<TextView>(R.id.pricing_text).text =
                                resources.getString(
                                    R.string.premium_subscription_price_no_trial,
                                    it.priceText,
                                    if(it.subscriptionPeriod[2].equals('w', true)) "week" else "month"
                                )
                        }
                    } ?: if(!BuildConfig.DEBUG) onError() else {}
                }

                override fun onSkuDetailsError(error: String?) {
                    if(!BuildConfig.DEBUG) onError() else {}
                }
            })

        findViewById<AppCompatImageView>(R.id.premium_close).setOnClickListener {
            Premium.showInterstitial(this)
            endActivity()
        }

        findViewById<View>(R.id.premium_one_time).setOnClickListener {
            Premium.mBillingProcessor?.subscribe(this, SUBSCRIPTION_PRODUCT_ID)
        }

        Premium.mIsPremium.observe(this) {
            if(it == true) {
                Toast.makeText(this, getString(R.string.premium_toast_success), Toast.LENGTH_SHORT).show()
                endActivity()
            }
        }

        findViewById<TextView>(R.id.optin_eula).apply {
            movementMethod = LinkMovementMethod.getInstance()
            text = Html.fromHtml(getString(R.string.eule_text, getString(R.string.pp_link), getString(R.string.terms_link)))
        }
    }

    override fun onBackPressed() {
        if(isAccepted(this)) {
            super.onBackPressed()
        }
    }

    fun endActivity() {
        PreferenceManager.getDefaultSharedPreferences(this)
            .edit().putBoolean("optin_accepted", true).apply()
        finish()
    }

    fun onError() {
        Toast.makeText(this, getString(R.string.premium_something_wrong), Toast.LENGTH_LONG).show()
        if(!BuildConfig.DEBUG) {
            endActivity()
        }
    }

    fun isAccepted(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean("optin_accepted", false)
    }
}