package com.timmy.ad.widget

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.google.ads.interactivemedia.v3.api.AdPodInfo
import com.google.ads.interactivemedia.v3.api.player.AdMediaInfo
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate
import com.timmy.ad.R
import com.timmy.ad.widget.player.VideoPlayer
import com.timmy.ad.widget.player.VideoPlayerCallback
import kotlinx.android.synthetic.main.view_ad_playback.view.*
import java.util.*

// 從 google ima github sample code 複製過來並轉為 kotlin 並把和 ad 無關的程式碼移除。
// https://github.com/googleads/googleads-ima-android/blob/master/AdvancedExample/app/src/main/java/com/google/ads/interactivemedia/v3/samples/videoplayerapp/VideoPlayerWithAdPlayback.java
class AdPlayback : ConstraintLayout {

    // The SDK will render ad playback UI elements into this ViewGroup.
    val adUiContainer: ViewGroup

    // VideoAdPlayer interface implementation for the SDK to send ad play/pause type events.
    lateinit var videoAdPlayer: VideoAdPlayer

    // The wrapped video player.
    private val videoPlayer: VideoPlayer

    // Track the currently playing media file. If doing preloading, this will need to be an
    // array or other data structure.
    private var adMediaInfo: AdMediaInfo? = null

    // Used to track if the current video is an ad (as opposed to a content video).
    var isAdDisplayed: Boolean = false
        private set

    // The saved position in the ad to resume if app is backgrounded during ad playback.
    private var savedAdPosition: Int = 0

    private val adCallbacks = HashSet<VideoAdPlayer.VideoAdPlayerCallback>()

    // A Timer to help track media updates
    private var timer: Timer? = null

    private val mainHandler: Handler = Handler(Looper.getMainLooper())

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        LayoutInflater.from(context).inflate(R.layout.view_ad_playback, this, true)
        setBackgroundColor(Color.parseColor("#80FF0000"))
        videoPlayer = view_ad_playback_player
        adUiContainer = view_ad_playback_container
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        init()
    }

    private fun init() {
        isAdDisplayed = false
        savedAdPosition = 0

        // Define VideoAdPlayer connector.
        videoAdPlayer = object : VideoAdPlayer {
            override fun loadAd(adMediaInfo: AdMediaInfo, adPodInfo: AdPodInfo) {
                view_ad_playback_progress.isVisible = true
                this@AdPlayback.adMediaInfo = adMediaInfo
                isAdDisplayed = false
                videoPlayer.setVideoPath(adMediaInfo.url)
            }

            override fun playAd(adMediaInfo: AdMediaInfo) {
                startTracking()
                if (isAdDisplayed) {
                    videoPlayer.resume()
                } else {
                    isAdDisplayed = true
                    videoPlayer.play()
                }
            }

            override fun pauseAd(adMediaInfo: AdMediaInfo) {
                stopTracking()
                videoPlayer.pause()
            }

            override fun stopAd(adMediaInfo: AdMediaInfo) {
                stopTracking()
                videoPlayer.stopPlayback()
            }

            override fun release() {
            }

            override fun getAdProgress(): VideoProgressUpdate {
                if (isAdDisplayed.not() || videoPlayer.getDuration() <= 0) {
                    return VideoProgressUpdate.VIDEO_TIME_NOT_READY
                } else {
                    mainHandler.post {
                        if (view_ad_playback_progress.isVisible) {
                            view_ad_playback_progress.isGone = true
                        }
                    }
                    return VideoProgressUpdate(videoPlayer.getCurrentPosition().toLong(), videoPlayer.getDuration().toLong())
                }
            }

            override fun getVolume(): Int {
                return videoPlayer.getVolume()
            }

            override fun removeCallback(videoAdPlayerCallback: VideoAdPlayer.VideoAdPlayerCallback) {
                adCallbacks.remove(videoAdPlayerCallback)
            }

            override fun addCallback(videoAdPlayerCallback: VideoAdPlayer.VideoAdPlayerCallback) {
                adCallbacks.add(videoAdPlayerCallback)
            }
        }

        // Set player callbacks for delegating major video events.
        videoPlayer.addPlayerCallback(object : VideoPlayerCallback {
            override fun onPlay() {
                if (isAdDisplayed) {
                    adCallbacks.forEach { it.onPlay(adMediaInfo) }
                }
            }

            override fun onResume() {
                if (isAdDisplayed) {
                    adCallbacks.forEach { it.onResume(adMediaInfo) }
                }
            }

            override fun onPause() {
                if (isAdDisplayed) {
                    adCallbacks.forEach { it.onPause(adMediaInfo) }
                }
            }

            override fun onCompleted() {
                if (isAdDisplayed) {
                    adCallbacks.forEach { it.onEnded(adMediaInfo) }
                }
            }

            override fun onError() {
                if (isAdDisplayed) {
                    adCallbacks.forEach { it.onError(adMediaInfo) }
                }
            }
        })
    }

    private fun startTracking() {
        if (timer != null) {
            return
        }
        timer = Timer()
        val updateTimerTask: TimerTask = object : TimerTask() {
            override fun run() {
                // Tell IMA the current video progress. A better implementation would be
                // reactive to events from the media player, instead of polling.
                adCallbacks.forEach {
                    it.onAdProgress(adMediaInfo, videoAdPlayer.adProgress)
                }
            }
        }
        val initialDelayMs = 250L
        val pollingTimeMs = 250L
        timer!!.schedule(updateTimerTask, pollingTimeMs, initialDelayMs)
    }

    private fun stopTracking() {
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
    }

    /**
     * Save the playback progress state of the currently playing video. This is called when content
     * is paused to prepare for ad playback or when app is backgrounded.
     */
    fun savePosition() {
        if (isAdDisplayed) {
            savedAdPosition = videoPlayer.getCurrentPosition()
        }
    }

    /**
     * Restore the currently loaded video to its previously saved playback progress state. This is
     * called when content is resumed after ad playback or when focus has returned to the app.
     */
    fun restorePosition() {
        if (isAdDisplayed) {
            videoPlayer.seekTo(savedAdPosition)
        }
    }

    /**
     * Returns current position
     */
    fun getCurrentPosition(): Long {
        return videoPlayer.getCurrentPosition().toLong()
    }

    /**
     * Returns duration
     */
    fun getDuration(): Long {
        return videoPlayer.getDuration().toLong()
    }

    /**
     * Returns volume
     */
    fun getVolume(): Int {
        return videoPlayer.getVolume()
    }
}