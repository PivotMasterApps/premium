package com.pivot.premium.rating

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.RatingBar.OnRatingBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.model.ReviewErrorCode
import com.pivot.premium.BuildConfig
import com.pivot.premium.R
import com.pivot.premium.ads.banners.px
import com.pivot.premium.getConfig
import com.pivot.premium.getPref
import com.pivot.premium.log
import com.pivot.premium.putPref
import kotlin.Exception


/**
 * Created by ahulr on 24-10-2016.
 */

class RatingDialog(
    val activity: Activity,
    private val builder: Builder
) : Dialog(activity),
    OnRatingBarChangeListener, View.OnClickListener {

    companion object {
        private const val DEFAULT_THRESHOLD = 5

        fun shouldShow(context: Context): Boolean {
            if(context.getPref("user_rating", -1) != -1) {
                log("User already rated")
                return false
            }

            val lastTimeShown = context.getPref("last_time_rating_shown", 0f)
            log("Last time rating shown = $lastTimeShown")
            if(System.currentTimeMillis() - lastTimeShown > (getConfig("rating_capping", 120) * 1000)) {
                context.putPref("last_time_rating_shown", System.currentTimeMillis())
                return true
            }
            log("Rating capped")

            return false
        }
    }

    private val session: Int = 0

    private val rateStarColor = Color.parseColor("#FEE109")

    //views
    private var textViewDialogTitle: TextView? = null
    private var textViewDialogButtonPositive: Button? = null
    private var textViewDialogButtonNegative: TextView? = null
    private var textViewFeedbackTitle: TextView? = null
    private var textViewFeedbackSubmit: TextView? = null
    private var textViewFeedbackCancel: TextView? = null
    private var editTextFeedback: EditText? = null
    private var ratingBar: RatingBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setContentView(R.layout.dialog_rating)

        initViews()
        setValues()
        setTheme()
        setSize()
    }

    private fun setSize() {
        // Get the window parameters
        val window = window
        if (window != null) {
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(window.attributes)

            // Set the width to MATCH_PARENT
            layoutParams.width = Resources.getSystem().displayMetrics.widthPixels - 48.px
            window.attributes = layoutParams

            val view = window.decorView

            view.background = ColorDrawable(Color.TRANSPARENT)
            view.setBackgroundResource(R.drawable.rounded_corners_background)
        }
    }

    private fun initViews() {
        textViewDialogTitle = findViewById(R.id.dialog_rating_title)
        textViewDialogButtonPositive = findViewById(R.id.dialog_rating_button_positive)
        textViewFeedbackTitle = findViewById(R.id.dialog_rating_feedback_title)
        textViewFeedbackSubmit = findViewById(R.id.dialog_rating_button_feedback_submit)
        textViewFeedbackCancel = findViewById(R.id.dialog_rating_button_feedback_cancel)
        ratingBar = findViewById(R.id.dialog_rating_rating_bar)
        editTextFeedback = findViewById(R.id.dialog_rating_feedback)
    }

    private fun setValues() {

        textViewDialogTitle?.text = context.getString(R.string.rating_dialog_experience)

        textViewDialogButtonNegative?.apply {
            setOnClickListener(this@RatingDialog)
            text = context.getString(R.string.rating_dialog_never)
            isVisible = session != 1
        }

        textViewDialogTitle?.apply {
            setText(context.getString(R.string.rating_dialog_experience, context.getString(R.string.app_name)))
        }

        textViewDialogButtonPositive?.apply {
            setOnClickListener(this@RatingDialog)
        }

        textViewFeedbackSubmit?.apply {
            setOnClickListener(this@RatingDialog)
        }

        textViewFeedbackCancel?.apply {
            setOnClickListener(this@RatingDialog)
        }

        ratingBar?.apply {
            onRatingBarChangeListener = this@RatingDialog
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                    val stars = progressDrawable as LayerDrawable
                    stars.getDrawable(2).setColorFilter(rateStarColor, PorterDuff.Mode.SRC_ATOP)
                    stars.getDrawable(1).setColorFilter(rateStarColor, PorterDuff.Mode.SRC_ATOP)
                    val ratingBarBackgroundColor = R.color.secondaryTextColor
                    stars.getDrawable(0).setColorFilter(ContextCompat.getColor(context, ratingBarBackgroundColor), PorterDuff.Mode.SRC_ATOP)
                } else {
                    val stars = progressDrawable
                    DrawableCompat.setTint(stars, rateStarColor)
                }
        }

        if (builder.ratingThresholdClearedListener == null) setRatingThresholdClearedListener()
        if (builder.ratingThresholdFailedListener == null) setRatingThresholdFailedListener()
    }

    private fun setTheme() {
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.dialog_rating_button_positive -> onPositiveButtonClick()
            R.id.dialog_rating_button_feedback_cancel -> dismiss()
        }
    }

    private fun onPositiveButtonClick() {
        log("Rating submitted with ${ratingBar?.rating}")
        context.putPref("user_rating", (ratingBar?.rating ?: -1f).toInt())
        if((ratingBar?.rating ?: 0f) >= DEFAULT_THRESHOLD) {
            dismiss()
            showInAppReview {  }
        } else {
            Toast.makeText(context, "Thank you for the feedback!", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    override fun onRatingChanged(ratingBar: RatingBar, v: Float, b: Boolean) {
        textViewDialogButtonPositive?.isEnabled = true
    }

    private fun setRatingThresholdClearedListener() {
        builder.ratingThresholdClearedListener = { _, _, _ ->
            dismiss()
        }
    }

    private fun setRatingThresholdFailedListener() {
        builder.ratingThresholdFailedListener = { _, _, _ ->

        }
    }

    private fun showInAppReview(onCompleted: (Boolean) -> Unit) {
        try {
            val manager = ReviewManagerFactory.create(context)
            val request = manager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                try {
                    if (task.isSuccessful) {
                        // We got the ReviewInfo object
                        val reviewInfo = task.result
                        val flow = manager.launchReviewFlow(activity, reviewInfo)
                        flow.addOnCompleteListener { _ ->
                            onCompleted(true)
                        }
                    } else {
                        // There was some problem, log or handle the error code.
                        @ReviewErrorCode val reviewErrorCode =
                            (task.getException() as ReviewException).errorCode
                        onCompleted(false)
                    }
                } catch (e: Exception) {
                    onCompleted(false)
                }
            }
        } catch (e: Exception) {
            onCompleted(false)
        }
    }

    private fun openGooglePlay(context: Context) {
        val marketUrl = context.getString(R.string.market_prefix) + context.packageName
        val marketUri = Uri.parse(marketUrl)
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, marketUri))
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(context, context.getString(R.string.error_no_google_play), Toast.LENGTH_SHORT).show()
        }
    }

    class Builder(private val activity: Activity) {

        // listeners
        internal var ratingThresholdClearedListener: ((dialog: RatingDialog?, rating: Float, thresholdCleared: Boolean) -> Unit)? = null
        internal var ratingThresholdFailedListener: ((dialog: RatingDialog?, rating: Float, thresholdCleared: Boolean) -> Unit)? = null
        internal var ratingDialogFormListener: ((feedback: String?) -> Unit)? = null
        internal var ratingDialogListener: ((rating: Float, thresholdCleared: Boolean) -> Unit)? = null

        fun onThresholdFailed(listener: ((dialog: RatingDialog?, rating: Float, thresholdCleared: Boolean) -> Unit)?): Builder {
            ratingThresholdFailedListener = listener
            return this
        }

        fun onRatingChanged(listener: ((rating: Float, thresholdCleared: Boolean) -> Unit)?): Builder {
            ratingDialogListener = listener
            return this
        }

        fun onRatingBarFormSubmit(listener: ((feedback: String?) -> Unit)?): Builder {
            ratingDialogFormListener = listener
            return this
        }

        fun build(): RatingDialog {
            return RatingDialog(activity, this)
        }
    }
}
