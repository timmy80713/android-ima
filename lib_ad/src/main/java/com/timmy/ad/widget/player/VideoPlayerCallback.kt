package com.timmy.ad.widget.player

// 從 google ima github sample code 複製過來並轉為 kotlin
// https://github.com/googleads/googleads-ima-android/blob/master/AdvancedExample/app/src/main/java/com/google/ads/interactivemedia/v3/samples/samplevideoplayer/VideoPlayer.java
/** Interface for alerting caller of major video events. */
interface VideoPlayerCallback {

    /** Called when the current video starts playing from the beginning.  */
    fun onPlay()

    /** Called when the current video pauses playback.  */
    fun onPause()

    /** Called when the current video resumes playing from a paused state.  */
    fun onResume()

    /** Called when the current video has completed playback to the end of the video.  */
    fun onComplete()

    /** Called when an error occurs during video playback.  */
    fun onError()
}