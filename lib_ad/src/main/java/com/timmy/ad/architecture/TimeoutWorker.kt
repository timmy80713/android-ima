package com.timmy.ad.architecture

import android.os.Handler
import android.os.Looper
import androidx.annotation.IntRange

/**
 * TimeoutWorker define self timeout, prevent third ad SDK no response.
 */
abstract class TimeoutWorker(
        @IntRange(from = LOAD_TIMEOUT_INFINITY.toLong(), to = LOAD_TIMEOUT_MAX_SECOND.toLong())
        private val vastTimeoutSecond: Int = LOAD_VAST_TIMEOUT_DEFAULT_SECOND,
        @IntRange(from = LOAD_TIMEOUT_INFINITY.toLong(), to = LOAD_TIMEOUT_MAX_SECOND.toLong())
        private val sourceTimeoutSecond: Int = LOAD_SOURCE_TIMEOUT_DEFAULT_SECOND
) {

    companion object {
        const val LOAD_TIMEOUT_INFINITY = 0
        const val LOAD_VAST_TIMEOUT_DEFAULT_SECOND = 15
        const val LOAD_SOURCE_TIMEOUT_DEFAULT_SECOND = 10
        const val LOAD_TIMEOUT_MAX_SECOND = 30
    }

    init {
        val argumentInvalid = vastTimeoutSecond < LOAD_TIMEOUT_INFINITY
                || vastTimeoutSecond > LOAD_TIMEOUT_MAX_SECOND
                || sourceTimeoutSecond < LOAD_TIMEOUT_INFINITY
                || sourceTimeoutSecond > LOAD_TIMEOUT_MAX_SECOND
        if (argumentInvalid) {
            throw IllegalArgumentException("minimum must greater than or equal to 0, maximum must less than or equal to 30.")
        }
    }

    private var loadVASTCompleted = false
    private var loadSourceCompleted = false

    private val timeoutHandler = Handler(Looper.getMainLooper())

    private val vastTimeoutRunnable = Runnable {
        invalidateLoadVAST {
            loadVASTTimeout(99999, "Load VAST occur timeout.")
        }
    }

    private val sourceTimeoutRunnable = Runnable {
        invalidateLoadSource {
            loadSourceTimeout(99998, "Load source occur timeout.")
        }
    }

    /**
     * Call super auto validate load VAST handler.
     */
    open fun request() {
        invalidateLoadVAST()
        invalidateLoadSource()
        validateLoadVAST()
    }

    /**
     * Call super auto invalidate load VAST and Source handler.
     */
    open fun destroy() {
        invalidateLoadVAST()
        invalidateLoadSource()
    }

    /**
     * Called load VAST timeout runnable.
     * @param errorCode
     * @param errorMessage
     */
    protected abstract fun loadVASTTimeout(errorCode: Int, errorMessage: String)

    /**
     * Called load source timeout runnable.
     * @param errorCode
     * @param errorMessage
     */
    protected abstract fun loadSourceTimeout(errorCode: Int, errorMessage: String)

    /**
     * Validate load VAST handler.
     */
    protected fun validateLoadVAST() {
        loadVASTCompleted = false
        timeoutHandler.removeCallbacks(vastTimeoutRunnable)
        if (vastTimeoutSecond != LOAD_TIMEOUT_INFINITY) {
            timeoutHandler.postDelayed(vastTimeoutRunnable, vastTimeoutSecond * 1000L)
        }
    }

    /**
     * Validate load source handler.
     */
    protected fun validateLoadSource() {
        loadSourceCompleted = false
        timeoutHandler.removeCallbacks(sourceTimeoutRunnable)
        if (sourceTimeoutSecond != LOAD_TIMEOUT_INFINITY) {
            timeoutHandler.postDelayed(sourceTimeoutRunnable, sourceTimeoutSecond * 1000L)
        }
    }

    /**
     * Invalidate load VAST timeout handler.
     * @param firstInvalidate call once, prevent race condition..
     */
    @Synchronized
    protected fun invalidateLoadVAST(firstInvalidate: (() -> Unit)? = null) {
        if (loadVASTCompleted.not()) {
            loadVASTCompleted = true
            timeoutHandler.removeCallbacks(vastTimeoutRunnable)
            firstInvalidate?.invoke()
        }
    }

    /**
     * Invalidate load source timeout handler.
     * @param firstInvalidate call once, prevent race condition.
     */
    @Synchronized
    protected fun invalidateLoadSource(firstInvalidate: (() -> Unit)? = null) {
        if (loadSourceCompleted.not()) {
            loadSourceCompleted = true
            timeoutHandler.removeCallbacks(sourceTimeoutRunnable)
            firstInvalidate?.invoke()
        }
    }
}