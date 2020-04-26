package com.timmy.ad.architecture

/**
 * Ad Exception
 */

class AdException(val code: Int, message: String?) : Exception(message)