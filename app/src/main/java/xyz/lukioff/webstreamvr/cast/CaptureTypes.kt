package xyz.lukioff.webstreamvr.cast

/** Lifecycle of a capture session, reported by [ScreenCaptureLink] to its listener. */
enum class CaptureState { IDLE, STARTING, STREAMING, ERROR }

/** Callbacks a capture source (currently only [ScreenCaptureLink]) reports to. */
interface CaptureListener {
    fun onStateChanged(state: CaptureState, detail: String?)
    fun onLayerConfigured(width: Int, height: Int)
    fun onVideo(data: ByteArray, offset: Int, length: Int, timestampUs: Long)
    fun onAudio(data: ByteArray, offset: Int, length: Int, timestampUs: Long)
}
