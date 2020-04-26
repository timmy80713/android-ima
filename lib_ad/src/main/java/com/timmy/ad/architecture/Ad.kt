package com.timmy.ad.architecture

import android.content.Context
import android.view.View
import com.google.ads.interactivemedia.v3.api.AdsManager
import com.timmy.ad.widget.AdPlayback

/**
 * 廣告請求參數
 */
sealed class RequestParams {
    data class InteractiveMedia(val context: Context, val url: String, val logger: AdInteractiveMedia.Logger) : RequestParams()
}

/**
 * 廣告結果
 */
sealed class Response {
    data class InteractiveMedia(val view: View,
                                val adPlayback: AdPlayback,
                                val adsManager: AdsManager?,
                                var durationMillis: Long = 0,
                                var currentPositionMillis: Long = 0,
                                var adPosition: Int = 0,
                                var totalAds: Int = 0) : Response()
}