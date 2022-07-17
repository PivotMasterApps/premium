package com.pivot.premium

import android.content.Context
import android.widget.Toast

object Premium {
    fun showToast(context: Context) {
        Toast.makeText(context, "Test", Toast.LENGTH_LONG).show()
    }
}