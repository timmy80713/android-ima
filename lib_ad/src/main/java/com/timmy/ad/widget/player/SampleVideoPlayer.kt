package com.timmy.ad.widget.player

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.AttributeSet
import android.widget.VideoView

// 從 google ima github sample code 複製過來並轉為 kotlin
// https://github.com/googleads/googleads-ima-android/blob/master/AdvancedExample/app/src/main/java/com/google/ads/interactivemedia/v3/samples/samplevideoplayer/SampleVideoPlayer.java
class SampleVideoPlayer : VideoView, VideoPlayer {

    private enum class PlaybackState {
        STOPPED, PAUSED, PLAYING
    }

    private var playbackState: PlaybackState? = null
    private val videoPlayerCallbacks = HashSet<VideoPlayerCallback>()
    private var onVideoSizeChangedBlock: ((width: Int, height: Int) -> Unit)? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        playbackState = PlaybackState.STOPPED
        setMediaController(null)

        super.setOnPreparedListener {
            it.setOnVideoSizeChangedListener { mp, width, height ->
                onVideoSizeChangedBlock?.invoke(width, height)
            }
        }

        // Set OnCompletionListener to notify our callbacks when the video is completed.
        super.setOnCompletionListener {
            // Reset the MediaPlayer.
            it.reset()
            it.setDisplay(holder)
            playbackState = PlaybackState.STOPPED
            videoPlayerCallbacks.forEach { callback -> callback.onCompleted() }
        }

        // Set OnErrorListener to notify our callbacks if the video errors.
        super.setOnErrorListener { _, _, _ ->
            playbackState = PlaybackState.STOPPED
            videoPlayerCallbacks.forEach { callback -> callback.onError() }

            // Returning true signals to MediaPlayer that we handled the error. This will
            // prevent the completion handler from being called.
            true
        }
    }

    override fun setOnPreparedListener(l: MediaPlayer.OnPreparedListener?) {
        // The setOnPreparedListener can only be implemented by SampleVideoPlayer.
        throw UnsupportedOperationException()
    }

    override fun setOnCompletionListener(listener: MediaPlayer.OnCompletionListener) {
        // The OnCompletionListener can only be implemented by SampleVideoPlayer.
        throw UnsupportedOperationException()
    }

    override fun setOnErrorListener(listener: MediaPlayer.OnErrorListener) {
        // The OnErrorListener can only be implemented by SampleVideoPlayer.
        throw UnsupportedOperationException()
    }

    override fun play() {
        super.start()
        videoPlayerCallbacks.forEach { callback -> callback.onPlay() }
        playbackState = PlaybackState.PLAYING
    }

    override fun resume() {
        super.start()
        videoPlayerCallbacks.forEach { callback -> callback.onResume() }
        playbackState = PlaybackState.PLAYING
    }

    override fun pause() {
        super.pause()
        playbackState = PlaybackState.PAUSED
        videoPlayerCallbacks.forEach { callback -> callback.onPause() }
    }

    override fun getDuration(): Int {
        return if (playbackState == PlaybackState.STOPPED) 0 else super.getDuration()
    }

    override fun getVolume(): Int {
        // Get the system's audio service and get media volume from it.
        val audioManager: AudioManager? = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        audioManager?.run {
            val volume = getStreamVolume(AudioManager.STREAM_MUSIC).toDouble()
            val max = getStreamMaxVolume(AudioManager.STREAM_MUSIC).toDouble()
            if (max <= 0) {
                return 0
            }
            // Return a range from 0-100.
            return (volume / max * 100.0f).toInt()
        }
        return 0
    }

    override fun stopPlayback() {
        if (playbackState == PlaybackState.STOPPED) return
        super.stopPlayback()
        playbackState = PlaybackState.STOPPED
    }

    override fun addPlayerCallback(callback: VideoPlayerCallback) {
        videoPlayerCallbacks.add(callback)
    }

    override fun removePlayerCallback(callback: VideoPlayerCallback) {
        videoPlayerCallbacks.remove(callback)
    }

    override fun setOnVideoSizeChangedBlock(onVideoSizeChangedBlock: (width: Int, height: Int) -> Unit) {
        this.onVideoSizeChangedBlock = onVideoSizeChangedBlock
    }
}