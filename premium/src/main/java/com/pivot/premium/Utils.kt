package com.pivot.premium.ads

import android.content.Context
import android.os.Bundle
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

fun Context.sendEvent(event: String, bundle: Bundle? = null) {
    FirebaseAnalytics.getInstance(this).logEvent(event, bundle)
}
