/* SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2026 BRUMA contributors */
package com.linkcore.emulator

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest

class OpenSourceLicensesActivity : Activity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val view = WebView(this)
        view.settings.javaScriptEnabled = false
        view.settings.allowContentAccess = false
        view.settings.allowFileAccess = true
        view.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if (uri.toString().startsWith("file:///android_asset/licenses/")) return false
                if (uri.scheme == "https") {
                    try { startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: android.content.ActivityNotFoundException) {}
                }
                return true
            }
        }
        setContentView(view)
        view.loadUrl("file:///android_asset/licenses/index.html")
    }
}
