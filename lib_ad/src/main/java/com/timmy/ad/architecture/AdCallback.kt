package com.timmy.ad.architecture

/**
 * 廣告讀取 callback
 * @param Params 請求廣告所需參數
 * @param Response 請求廣告結果回應
 */
interface RequestCallback<Params, Response> {
    fun onStart(params: Params)

    fun onVASTLoadStart(params: Params)
    fun onVASTLoadSuccess(params: Params, response: Response)
    fun onVASTLoadError(params: Params, error: AdException)

    fun onSourceLoadStart(params: Params, response: Response)
    fun onSourceLoadSuccess(params: Params, response: Response)
    fun onSourceLoadError(params: Params, error: AdException)

    fun onError(params: Params, error: AdException)
    fun onSuccess(params: Params, response: Response)
}

/**
 * 廣告讀取 simple callback
 * @param Params 請求廣告所需參數
 * @param Response 請求廣告結果回應
 */
abstract class SimpleRequestCallback<Params, Response> : RequestCallback<Params, Response> {
    override fun onStart(params: Params) {
    }

    override fun onVASTLoadStart(params: Params) {
    }

    override fun onVASTLoadSuccess(params: Params, response: Response) {
    }

    override fun onVASTLoadError(params: Params, error: AdException) {
    }

    override fun onSourceLoadStart(params: Params, response: Response) {
    }

    override fun onSourceLoadSuccess(params: Params, response: Response) {
    }

    override fun onSourceLoadError(params: Params, error: AdException) {
    }

    override fun onError(params: Params, error: AdException) {
    }

    override fun onSuccess(params: Params, response: Response) {
    }
}

/**
 * 廣告操作 callback
 * @param Response 廣告結果回應
 */
interface InteractionCallback<Response> {
    fun onImpression(ad: Response)
    fun onClicked(ad: Response)
    fun onDismissed(ad: Response)
}

/**
 * 影音廣告操作 callback
 * @param Response 廣告結果回應
 */
interface VideoInteractionCallback<Response> : InteractionCallback<Response> {
    fun onVideoStarted(ad: Response)
    fun onVideoComplete(ad: Response)
    fun onVideoAllComplete(response: Response)
}

/**
 * 影音廣告操作 callback
 * @param Response 廣告結果回應
 */
interface VideoCustomInteractionCallback<Response> {
    fun onVideoSkipped(ad: Response)
    fun onVideoTapped(ad: Response)
    fun onVideoFakeClicked(response: Response)
}

/**
 * interactive media ad 專用的 callback
 */
interface ImaInteractionCallback<Response> : VideoInteractionCallback<Response>, VideoCustomInteractionCallback<Response>