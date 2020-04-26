package com.timmy.ad.widget.player

// 從 google ima github sample code 複製過來並轉為 kotlin
// https://github.com/googleads/googleads-ima-android/blob/master/AdvancedExample/app/src/main/java/com/google/ads/interactivemedia/v3/samples/samplevideoplayer/VideoPlayer.java
/** Interface definition for controlling video playback. */
interface VideoPlayer {
    /** Play the currently loaded video from its current position.  */
    fun play()

    /** Pause the currently loaded video.  */
    fun pause()

    /** Resume the currently loaded video.  */
    fun resume()

    /** Get the playback progress state (milliseconds) of the current video.  */
    fun getCurrentPosition(): Int

    /** Progress the currently loaded video to the given position (milliseconds).  */
    fun seekTo(videoPosition: Int)

    /** Get the total length of the currently loaded video in milliseconds.  */
    fun getDuration(): Int

    /** Gets the current volume. Range is [0-100].  */
    fun getVolume(): Int

    /** Stop playing the currently loaded video.  */
    fun stopPlayback()

    /** Set the URL or path of the video to play.  */
    fun setVideoPath(videoUrl: String)

    /** Provide the player with a callback for major video events (pause, complete, resume, etc).  */
    fun addPlayerCallback(callback: VideoPlayerCallback)

    /** Remove a player callback from getting notified on video events.  */
    fun removePlayerCallback(callback: VideoPlayerCallback)
}