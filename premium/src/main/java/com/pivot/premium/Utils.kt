package com.pivot.premium

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig

fun <T> getConfig(key: String, default: T): T {
    val value = FirebaseRemoteConfig.getInstance().getValue(key)
    if(value.source == FirebaseRemoteConfig.VALUE_SOURCE_STATIC) {
        return default
    }

    return try {
        return when(default) {
            is Int -> FirebaseRemoteConfig.getInstance().getLong(key).toInt() as T
            is String -> FirebaseRemoteConfig.getInstance().getString(key) as T
            is Boolean -> FirebaseRemoteConfig.getInstance().getBoolean(key) as T
            is Long -> FirebaseRemoteConfig.getInstance().getLong(key) as T
            else -> default
        }
    } catch (e: Exception) {
        default
    }
}

fun <T> Context.getPref(key: String, default: T): T {
    val prefs = getSharedPreferences(Premium.PREMIUM_PREFS_NAME, 0)

    return try {
        return when(default) {
            is Int -> prefs.getInt(key, default) as T
            is String -> prefs.getString(key, default) as T
            is Boolean -> prefs.getBoolean(key, default) as T
            is Long -> prefs.getLong(key, default) as T
            is Float -> prefs.getFloat(key, default) as T
            else -> throw IllegalArgumentException("Type is not supported")
        }
    } catch (e: Exception) {
        default
    }
}

fun <T> Context.putPref(key: String, value: T) {
    val prefs = getSharedPreferences(Premium.PREMIUM_PREFS_NAME, 0)
    when(value) {
        is Int -> prefs.edit().putInt(key, value).apply()
        is String -> prefs.edit().putString(key, value).apply()
        is Boolean -> prefs.edit().putBoolean(key, value).apply()
        is Long -> prefs.edit().putLong(key, value).apply()
        is Float -> prefs.edit().putFloat(key, value).apply()
        else -> throw IllegalArgumentException("Type is not supported")
    }
}

internal fun log(msg: String) {
    if(Premium.mConfiguration.debug) {
        Log.d(Premium.TAG, msg)
    }
}

fun Context.sendEvent(event: String, bundle: Bundle? = null) {
    FirebaseAnalytics.getInstance(this).logEvent(event, bundle)
}

fun getInterstitialAdUnit(): String {
    return if(Premium.mConfiguration.showTestAds) "ca-app-pub-3940256099942544/1033173712" else Premium.mConfiguration.interstitialAdUnit
}

fun getBannersAdUnit(): String {
    return if(Premium.mConfiguration.showTestAds) "ca-app-pub-3940256099942544/6300978111" else Premium.mConfiguration.bannerAdUnit
}

val Int.dp: Int get() = (this / Resources.getSystem().displayMetrics.density).toInt()
