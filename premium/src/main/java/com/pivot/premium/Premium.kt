package com.pivot.premium

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.MutableLiveData
import com.pivot.premium.ads.AdManager
import com.pivot.premium.billing.BillingManager
import com.pivot.premium.purchases.PremiumActivity
import com.pivot.premium.rating.RatingDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("StaticFieldLeak")
object Premium {

    const val PREMIUM_PREFS_NAME = "premium_shared_preferences"
    const val TAG = "Premium"

    private var mContext: Context? = null
    lateinit  var mMainActivity: Class<out Activity>
    lateinit var mConfiguration: Configuration
    var mBillingManager: BillingManager? = null
    var onDismissed: (() -> Unit)? = null

    fun initialize(
        context: Context,
        mainActivity: Class<out Activity>,
        configuration: Configuration
    ) {
        if(mContext != null) return

        mConfiguration = configuration
        mMainActivity = mainActivity
        mContext = context
        initSdk()
    }

    private fun initSdk() {
        CoroutineScope(Dispatchers.Default).launch {
            initializeLifecycle()
            AdManager.initialize(mContext!!)
            initializeBilling()
        }
    }

    fun onAppOpen(activity: AppCompatActivity) {
        PreferenceManager.getDefaultSharedPreferences(mContext!!).apply {
            val opens = getInt("app_opens", 0)
            if(mBillingManager?.mIsPremium?.value != BillingManager.PremiumState.PREMIUM && listOf(0,2,5).contains(opens)) {
                showPremium()
            } else if( opens % 3 == 0 &&
                RatingDialog.shouldShow(activity)
                ) {
                showRateUs(activity)
            } else {
                showInterstitial(activity)
            }
            edit().putInt("app_opens", opens + 1).apply()
        }
    }

    fun initializeLifecycle() {
        (mContext as Application).registerActivityLifecycleCallbacks(object:
            Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, p1: Bundle?) {
                if(mMainActivity.simpleName == activity::class.java.simpleName) {
                    onAppOpen(activity as AppCompatActivity)
                }
            }
            override fun onActivityStarted(p0: Activity) {}
            override fun onActivityResumed(p0: Activity) {}
            override fun onActivityPaused(p0: Activity) {}
            override fun onActivityStopped(p0: Activity) {}
            override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {}
            override fun onActivityDestroyed(p0: Activity) {}
        })
    }

    fun showPremium() {
        Log.d(TAG, "showPremium: ")
        mContext?.startActivity(
            Intent(mContext, PremiumActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun showPrivacyActivity(onDismissed: (() -> Unit)? = null) {
        this.onDismissed = onDismissed
        mContext?.startActivity(
            Intent(mContext, OptinActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun optinFinished() {
        Log.d(TAG, "optinFinished: ")
        onDismissed?.invoke()
    }

    fun onRatingMoment(activity: Activity) {
        if(RatingDialog.shouldShow(activity)) {
            RatingDialog(
                activity,
                builder = RatingDialog
                    .Builder(activity)
            ).show()
        } else {
            showInterstitial(activity)
        }
    }

    fun showRateUs(activity: AppCompatActivity) {
        Log.d(TAG, "showRateUs: ")
        RatingDialog(
            activity,
            builder = RatingDialog
                .Builder(activity)
        ).show()
    }

    fun splashFinished() {
        mContext?.startActivity(
            Intent(mContext!!, mMainActivity).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun showInterstitial(activity: Activity, onDismissed: (() -> Unit)? = null) {
        if(mBillingManager?.mIsPremium?.value != BillingManager.PremiumState.PREMIUM) {
            AdManager.showInterstitial(activity, onDismissed)
        }
    }

    private fun initializeBilling() {
        mBillingManager = BillingManager(mContext!!)
    }

    fun openUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.no_app_resolved), Toast.LENGTH_SHORT).show()
        }
    }

    fun shareMyApp(context: Context) {

        val intent = Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=${context.packageName}")
            type = "text/plain"
        }, null)

        context.startActivity(intent)
    }

    fun sendSupportEmail(context: Context) {
        val emailIntent = Intent(Intent.ACTION_SENDTO)
        emailIntent.data = Uri.parse("mailto:")
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("pivotmasterapps@gmail.com"))

        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Issue tracker") // Subject (title)

        if (emailIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(emailIntent)
        }
    }

    fun isPremium() = mBillingManager?.mIsPremium?.value == BillingManager.PremiumState.PREMIUM

    data class Configuration(
        val bannerAdUnit: String,
        val interstitialAdUnit: String,
        val showTestAds: Boolean = false,
        val enableRewarded: Boolean = false,
        val debug: Boolean = false
    ){}
}