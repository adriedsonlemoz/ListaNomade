package com.listanomade.app.util

import android.view.View
import android.view.WindowInsets

object SystemBarInsets {
    fun apply(view: View, top: Boolean = true, bottom: Boolean = true) {
        val left = view.paddingLeft
        val baseTop = view.paddingTop
        val right = view.paddingRight
        val baseBottom = view.paddingBottom

        view.setOnApplyWindowInsetsListener { target, insets ->
            val topInset = if (top) insets.systemWindowInsetTop else 0
            val bottomInset = if (bottom) insets.systemWindowInsetBottom else 0
            target.setPadding(left, baseTop + topInset, right, baseBottom + bottomInset)
            insets
        }
        view.requestApplyInsets()
    }
}
