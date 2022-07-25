package com.pivot.premium

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.prefs.Preferences

class OptinActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(isAccepted(this)) endActivity()

        setContentView(R.layout.activity_optin)

        findViewById<TextView>(R.id.optin_eula).apply {
            movementMethod = LinkMovementMethod.getInstance()
            text = Html.fromHtml(getString(R.string.eule_text))
        }

        findViewById<Button>(R.id.optin_button).setOnClickListener {
            PreferenceManager.getDefaultSharedPreferences(this)
                .edit().putBoolean("optin_accepted", true).apply()
            endActivity()
        }
    }

    fun endActivity() {
        Premium.optinFinished()
    }

    override fun onBackPressed() {

    }

    companion object {
        fun isAccepted(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("optin_accepted", false)
        }
    }
}