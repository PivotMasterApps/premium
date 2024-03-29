package com.pivot.premium.purchases

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import com.android.billingclient.api.Purchase.PurchaseState
import com.pivot.premium.BuildConfig
import com.pivot.premium.Premium
import com.pivot.premium.R
import com.pivot.premium.billing.BillingManager
import com.pivot.premium.sendEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PremiumActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.custom_primium_dialog)

        Premium.mBillingManager?.premiumOffer()?.observe(this) { offer ->
            findViewById<View>(R.id.premium_pb_one_time).isVisible = (offer == null)
            if(offer == null) return@observe

            var pricingTxt = ""

            if(offer.freeTrialPeriodUnit.isNotEmpty()) {
                pricingTxt += "${offer.freeTrialPeriodValue} ${offer.formattedFreeUnit()} free, then "
            } else {
                findViewById<Button>(R.id.premium_one_time).text = "TRY NOW!"
            }

            pricingTxt += "${offer.price} / ${offer.formattedBilledUnit()}"
            findViewById<TextView>(R.id.pricing_text).text = pricingTxt
        }

        findViewById<AppCompatImageView>(R.id.premium_close).setOnClickListener {
            Premium.showInterstitial(this)
            endActivity()
        }

        findViewById<View>(R.id.premium_one_time).setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val result = Premium.mBillingManager?.launch(this@PremiumActivity)
                sendEvent("trial_clicked")
            }
        }

        Premium.mBillingManager?.mIsPremium?.observe(this) {
            if(it == BillingManager.PremiumState.PREMIUM) {
                runOnUiThread {
                    Toast.makeText(this, getString(R.string.premium_toast_success), Toast.LENGTH_SHORT).show()
                }
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