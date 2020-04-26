package com.timmy.app.ima

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.timmy.ad.architecture.*
import kotlinx.android.synthetic.main.activity_vast.*

class VastActivity  : AppCompatActivity() {
    // 廣告結果
    private var response: Response? = null
    private var adInteractiveMedia: AdInteractiveMedia? = null

    // Provide an implementation of a logger so we can output SDK events to the UI.
    val logger = object : AdInteractiveMedia.Logger {
        override fun log(logMessage: String) {
            logText?.append(logMessage)
            logScroll?.post { logScroll.fullScroll(View.FOCUS_DOWN) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vast)
        initAdPlayerInteractiveMedial()
        load.setOnClickListener {
            logText?.text = ""
            adInteractiveMedia?.request()
        }

        destroy.setOnClickListener {
            logText?.text = ""
            adInteractiveMedia?.destroy()
        }
    }

    private fun initAdPlayerInteractiveMedial() {
        val vastUrl =
                "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dlinear&correlator="
        val parameter = RequestParams.InteractiveMedia(this, vastUrl, logger)

        adInteractiveMedia = AdInteractiveMedia(
                parameter,
                object : SimpleRequestCallback<RequestParams.InteractiveMedia, Response.InteractiveMedia>() {
                    override fun onVASTLoadSuccess(params: RequestParams.InteractiveMedia, response: Response.InteractiveMedia) {
                        this@VastActivity.response = response
                        container.addView(response.view)
                        response.adsManager?.start()
                    }

                    override fun onError(params: RequestParams.InteractiveMedia, error: AdException) {
                        super.onError(params, error)
                        container.removeAllViews()
                    }
                },
                object : ImaInteractionCallback<Response.InteractiveMedia> {
                    override fun onImpression(ad: Response.InteractiveMedia) {
                    }

                    override fun onDismissed(ad: Response.InteractiveMedia) {
                    }

                    override fun onClicked(ad: Response.InteractiveMedia) {
                    }

                    override fun onVideoStarted(ad: Response.InteractiveMedia) {
                    }

                    override fun onVideoComplete(ad: Response.InteractiveMedia) {
                    }

                    override fun onVideoAllComplete(response: Response.InteractiveMedia) {
                    }

                    override fun onVideoSkipped(ad: Response.InteractiveMedia) {
                    }

                    override fun onVideoTapped(ad: Response.InteractiveMedia) {
                    }

                    override fun onVideoFakeClicked(response: Response.InteractiveMedia) {
                    }
                })
    }

    public override fun onResume() {
        // IMA 要恢復播放
        response?.run {
            if (this is Response.InteractiveMedia) {
                adPlayback.restorePosition()
                if (adsManager != null && adPlayback.isAdDisplayed) {
                    adsManager!!.resume()
                }
            }
        }
        super.onResume()
    }

    public override fun onPause() {
        // IMA 要暫停播放
        response?.run {
            if (this is Response.InteractiveMedia) {
                adPlayback.savePosition()
                if (adsManager != null && adPlayback.isAdDisplayed) {
                    adsManager!!.pause()
                }
            }
        }
        super.onPause()
    }
}