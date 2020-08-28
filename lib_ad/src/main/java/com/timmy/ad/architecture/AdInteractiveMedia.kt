package com.timmy.ad.architecture

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.google.ads.interactivemedia.v3.api.*
import com.timmy.ad.R
import kotlinx.android.synthetic.main.ad_player_interactive_media.view.*

/**
 * @param params 請求  interactive medial 廣告所需參數
 * @param requestCallback 廣告讀取 callback
 * @param interactionCallback 廣告行為 callback
 */
class AdInteractiveMedia(
        private val params: RequestParams.InteractiveMedia,
        private val requestCallback: RequestCallback<in RequestParams.InteractiveMedia, in Response.InteractiveMedia>,
        private val interactionCallback: ImaInteractionCallback<in Response.InteractiveMedia>,
        vastTimeoutSecond: Int = LOAD_TIMEOUT_INFINITY,
        sourceTimeoutSecond: Int = LOAD_TIMEOUT_INFINITY
) : TimeoutWorker(vastTimeoutSecond, sourceTimeoutSecond) {

    private val TAG = this::class.java.simpleName

    private val ERROR_NO_VAST_AD_TAG_RUL_SPECIFIED_CODE = -1
    private val ERROR_NO_VAST_AD_TAG_RUL_SPECIFIED_MESSAGE = "No VAST ad tag URL specified"

    private var adView = LayoutInflater.from(params.context).inflate(R.layout.ad_player_interactive_media, null)
    private var adPlayback = adView.ad_player_interactive_media_playback
    private var adBanner = adView.ad_player_interactive_media_banner

    // The AdsLoader instance exposes the requestAds method.
    private var adsLoader: AdsLoader? = null

    // AdsManager exposes methods to control ad playback and listen to ad events.
    private var adsManager: AdsManager? = null

    private lateinit var response: Response.InteractiveMedia

    private var currentTimeMillis = 0L

    private var vastLoadedListener = getVastLoadedListener()
    private var vastErrorListener = getVastErrorListener()

    private var adEventListener = getAdEventListener()
    private var adErrorListener = getAdErrorListener()

    private fun getElapsedTimeMillis() = System.currentTimeMillis() - currentTimeMillis

    override fun request() {
        super.request()
        // Since we're switching to a new video, tell the SDK the previous video is finished.
        release()

        currentTimeMillis = System.currentTimeMillis()

        Log.i(TAG, " ***廣告*** 開始讀取 ima url : ${params.url}")
        log("開始讀取 ima url : ${params.url}")

        requestCallback.onStart(params)
        requestCallback.onVASTLoadStart(params)

        if (params.url.isEmpty() or params.url.isBlank()) {
            invalidateLoadVAST {
                release()
                val exception = AdException(ERROR_NO_VAST_AD_TAG_RUL_SPECIFIED_CODE, ERROR_NO_VAST_AD_TAG_RUL_SPECIFIED_MESSAGE)
                requestCallback.onVASTLoadError(params, exception)
                requestCallback.onError(params, exception)
            }
            return
        }

        val imaSdkSettings = ImaSdkFactory.getInstance().createImaSdkSettings().apply {
            language = "zh-TW"
        }

        val adDisplayContainer = ImaSdkFactory.createAdDisplayContainer(adPlayback.adUiContainer, adPlayback.videoAdPlayer)

        adsLoader = ImaSdkFactory.getInstance().createAdsLoader(params.context, imaSdkSettings, adDisplayContainer).apply {
            addAdErrorListener(vastErrorListener)
            addAdsLoadedListener(vastLoadedListener)
        }

        // Create the ads request.
        val request = ImaSdkFactory.getInstance().createAdsRequest().apply {
            adTagUrl = params.url
        }

        // Request the ad. After the ad is loaded, onAdsManagerLoaded() will be called.
        adsLoader?.requestAds(request)
    }

    override fun destroy() {
        super.destroy()
        release()
    }

    override fun loadVASTTimeout(errorCode: Int, errorMessage: String) {
        Log.i(TAG, " ***廣告*** 花了 ${getElapsedTimeMillis()} 毫秒，IMA VAST 讀取失敗: [$errorCode: $errorMessage]")
        log("花了 ${getElapsedTimeMillis()} 毫秒，IMA VAST 讀取失敗: [$errorCode: $errorMessage]")
        release()
        val exception = AdException(errorCode, errorMessage)
        requestCallback.onVASTLoadError(params, exception)
        requestCallback.onError(params, exception)
    }

    override fun loadSourceTimeout(errorCode: Int, errorMessage: String) {
        Log.i(TAG, " ***廣告*** 花了 ${getElapsedTimeMillis()} 毫秒，IMA Source 讀取失敗: [$errorCode: $errorMessage]")
        log("花了 ${getElapsedTimeMillis()} 毫秒，IMA Source 讀取失敗: [$errorCode: $errorMessage]")
        release()
        val exception = AdException(errorCode, errorMessage)
        requestCallback.onSourceLoadError(params, exception)
        requestCallback.onError(params, exception)
    }

    private fun getVastLoadedListener(): AdsLoader.AdsLoadedListener {
        return AdsLoader.AdsLoadedListener {
            /**
             * An event raised when ads are successfully loaded from the ad server via AdsLoader.
             */
            Log.i(TAG, " ***廣告*** 花了 ${getElapsedTimeMillis()} 毫秒，IMA VAST 讀取成功")
            log("花了 ${getElapsedTimeMillis()} 毫秒，IMA VAST 讀取成功")

            // Ads were successfully loaded, so get the AdsManager instance. AdsManager has events for ad playback and errors.
            adsManager = it.adsManager.apply {
                // Attach event and error event listeners.
                addAdErrorListener(adErrorListener)
                addAdEventListener(adEventListener)

                val adsRenderingSettings = ImaSdkFactory.getInstance().createAdsRenderingSettings().apply {
                    bitrateKbps = 700
                    setPlayAdsAfterTime((-1).toDouble())
                }
                init(adsRenderingSettings)
            }

            response = Response.InteractiveMedia(adView, adPlayback, adsManager)
        }
    }

    private fun getVastErrorListener(): AdErrorEvent.AdErrorListener {
        return AdErrorEvent.AdErrorListener { error ->
            invalidateLoadVAST {
                Log.i(TAG, " ***廣告*** 花了 ${getElapsedTimeMillis()} 毫秒，IMA VAST 讀取失敗: [${error.error.errorCode}: ${error.error.message}]")
                log("花了 ${getElapsedTimeMillis()} 毫秒，IMA VAST 讀取失敗: [${error.error.errorCode}: ${error.error.message}]")
                release()
                val exception = AdException(error.error.errorCodeNumber, error.error.message)
                requestCallback.onVASTLoadError(params, exception)
                requestCallback.onError(params, exception)
            }
        }
    }

    private fun getAdEventListener(): AdEvent.AdEventListener {
        return AdEvent.AdEventListener { event ->
            // These are the suggested event types to handle. For full list of all ad
            // event types, see the documentation for AdEvent.AdEventType.
            if (event.type != AdEvent.AdEventType.AD_PROGRESS) {
                Log.i(TAG, " ***廣告*** 觸發事件 : $event")
                log("觸發事件 : ${event.type}")
            }

            when (event.type) {
                AdEvent.AdEventType.LOADED -> {
                    // AdEventType.LOADED will be fired when ads are ready to be
                    // played. AdsManager.start() begins ad playback. This method is
                    // ignored for VMAP or ad rules playlists, as the SDK will
                    // automatically start executing the playlist.

                    // LOADED 只是代表讀到 VAST ，不代表 media 可以播，所以 != 拿到廣告

                    invalidateLoadVAST {
                        validateLoadSource()

                        currentTimeMillis = System.currentTimeMillis()

                        Log.i(TAG, " ***廣告*** [${event.ad.adPodInfo.adPosition}/${event.ad.adPodInfo.totalAds}]")
                        log("[${event.ad.adPodInfo.adPosition}/${event.ad.adPodInfo.totalAds}]")

                        response.adPosition = event.ad.adPodInfo.adPosition
                        response.totalAds = event.ad.adPodInfo.totalAds

                        requestCallback.onVASTLoadSuccess(params, response)
                        requestCallback.onSourceLoadStart(params, response)
                    }
                }
                AdEvent.AdEventType.STARTED -> {

                    // 觸發這件事表示 media 可以播，所以 == 拿到廣告
                    invalidateLoadSource {
                        Log.i(TAG, " ***廣告*** 花了 ${getElapsedTimeMillis()} 毫秒，IMA Source 讀取成功")
                        log("花了 ${getElapsedTimeMillis()} 毫秒，IMA Source 讀取成功")
                        Log.i(TAG, " ***廣告*** Description: ${event.ad.description}")
                        log("Description: ${event.ad.description}")

                        // 取得廣告的總長度
                        response.durationMillis = adPlayback.getDuration()

                        requestCallback.onSourceLoadSuccess(params, response)
                        requestCallback.onSuccess(params, response)
                        interactionCallback.onImpression(response)
                        interactionCallback.onVideoStarted(response)

                        adBanner.run {
                            setImageDrawable(ContextCompat.getDrawable(params.context, R.drawable.vector_ad_banner))
                            isVisible = false
                            setOnClickListener {
                                interactionCallback.onVideoFakeClicked(response)
                            }
                        }
                    }
                }
                AdEvent.AdEventType.AD_PROGRESS -> {
                    // 取得廣告目前的進度
                    response.currentPositionMillis = adPlayback.getCurrentPosition()
                }
                AdEvent.AdEventType.CLICKED -> {
                    // 右上角 瞭解更多 點擊 (real click)
                    interactionCallback.onClicked(response)
                }
                AdEvent.AdEventType.TAPPED -> {
                    // 右上角 real click , fake click 以外的所有區域點擊
                    interactionCallback.onVideoTapped(response)
                }
                AdEvent.AdEventType.SKIPPED -> {
                    removeFromParent()
                    resetBanner()
                    if (event.ad.adPodInfo.adPosition != event.ad.adPodInfo.totalAds) {
                        validateLoadVAST()
                    }
                    interactionCallback.onVideoSkipped(response)
                }
                AdEvent.AdEventType.COMPLETED -> {
                    removeFromParent()
                    resetBanner()
                    if (event.ad.adPodInfo.adPosition != event.ad.adPodInfo.totalAds) {
                        validateLoadVAST()
                    }
                    interactionCallback.onVideoComplete(response)
                }
                AdEvent.AdEventType.ALL_ADS_COMPLETED -> {
                    interactionCallback.onVideoAllComplete(response)
                    release()
                }
                else -> {
                }
            }
        }
    }

    private fun getAdErrorListener(): AdErrorEvent.AdErrorListener {
        return AdErrorEvent.AdErrorListener { error ->
            invalidateLoadSource {
                Log.i(TAG, " ***廣告*** 發生錯誤: [${error.error.errorCode}: ${error.error.message}]")
                log("發生錯誤: [${error.error.errorCode}: ${error.error.message}]")
                release()
                val exception = AdException(error.error.errorCodeNumber, error.error.message)
                requestCallback.onSourceLoadError(params, exception)
                requestCallback.onError(params, exception)
            }
        }
    }

    private fun release() {

        removeFromParent()
        resetBanner()

        adsManager?.apply {
            removeAdEventListener(adEventListener)
            removeAdErrorListener(adErrorListener)
            destroy()
        }
        adsManager = null

        adsLoader?.apply {
            removeAdsLoadedListener(vastLoadedListener)
            removeAdErrorListener(vastErrorListener)
        }
    }

    // 把自己從 parent 中移除
    private fun removeFromParent() {
        adView.parent?.run {
            if (this is ViewGroup) {
                removeView(adView)
            }
        }
    }

    // Reset banner.
    private fun resetBanner() {
        adBanner.apply {
            isGone = true
            setImageDrawable(null)
        }
    }

    /**
     * Log interface, so we can output the log commands to the UI or similar.
     */
    interface Logger {
        fun log(logMessage: String)
    }

    // View that we can write log messages to, to display in the UI.
    private val logger: Logger = params.logger

    private fun log(message: String) {
        logger.log(message + "\n")
    }
}