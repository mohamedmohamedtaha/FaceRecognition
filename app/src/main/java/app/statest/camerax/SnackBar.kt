package app.statest.camerax

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar

fun View.showSnackBar(message: String,right:Boolean) {
    if (rootView != null) {
        val snackBar = Snackbar.make(this, message, Snackbar.LENGTH_LONG)
        (snackBar.view as Snackbar.SnackbarLayout).let { snackBarLayout ->
            snackBarLayout.removeAllViews()
            snackBarLayout.setPadding(0, 0, 0, 0)
            snackBarLayout.setBackgroundColor(Color.TRANSPARENT)
            LayoutInflater.from(context).inflate(R.layout.snackbar_top, snackBarLayout)
                .let { layout ->
                    val tvTitle = layout.findViewById(R.id.tvTitle) as AppCompatTextView
                    val imageIcon = layout.findViewById(R.id.imageIcon) as AppCompatImageView
                    if (right)
                        imageIcon.setImageResource(R.drawable.ic_check)
                    else
                        imageIcon.setImageResource(R.drawable.ic_close)

                    tvTitle.text = message
                }
            val params = snackBarLayout.layoutParams
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            if (params is FrameLayout.LayoutParams) {
                params.gravity = Gravity.TOP
            } else if (params is CoordinatorLayout.LayoutParams) {
                params.gravity = Gravity.TOP
            }
            snackBarLayout.layoutParams = params
        }
        snackBar.show()
    }
}