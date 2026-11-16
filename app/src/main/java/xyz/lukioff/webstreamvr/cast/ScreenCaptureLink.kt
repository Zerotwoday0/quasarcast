package xyz.lukioff.webstreamvr.cast

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.Executors

/**
 * Captures the headset's own screen via the standard Android MediaProjection API and
 * encodes it to H.264, in place of asking Meta's (now signature-gated) casting service
 * to do it. No special permission beyond the one-time system capture dialog.
 */
class ScreenCaptureLink(private val context: Context, private val listener: CaptureListener) {

    class Settings(val width: Int, val height: Int, val bitrate: Int, val fps: Int, val audio: Boolean)

    @Volatile private var running = false
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var codec: MediaCodec? = null
    private var drainThread: Thread? = null
    private val io = Executors.newSingleThreadExecutor { Thread(it, "ScreenCaptureLink-io") }

    fun start(resultCode: Int, resultData: Intent, initial: Settings) {
        if (running) return
        running = true
        io.execute {
            try {
                startProjection(resultCode, resultData, initial)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start screen capture", e)
                listener.onStateChanged(CaptureState.ERROR, "Не удалось начать запись экрана: ${e.message}")
                running = false
            }
        }
    }

    fun stop() {
        if (!running) return
        running = false
        io.execute { teardown() }
        io.shutdown()
        listener.onStateChanged(CaptureState.IDLE, null)
    }

    fun requestKeyframe() {
        val c = codec ?: return
        try {
            val params = Bundle()
            params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0)
            c.setParameters(params)
        } catch (_: Exception) {
        }
    }

    private fun startProjection(resultCode: Int, resultData: Intent, s: Settings) {
        listener.onStateChanged(CaptureState.STARTING, null)
        val manager = context.getSystemService(MediaProjectionManager::class.java)
        val proj = manager.getMediaProjection(resultCode, resultData)
            ?: throw IllegalStateException("getMediaProjection returned null")
        projection = proj
        proj.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                Log.i(TAG, "MediaProjection stopped")
                if (running) listener.onStateChanged(CaptureState.ERROR, "Запись экрана остановлена системой")
                running = false
                teardown()
            }
        }, Handler(Looper.getMainLooper()))

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, s.width, s.height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, s.bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, s.fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
        }
        val enc = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        enc.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val inputSurface = enc.createInputSurface()
        enc.start()
        codec = enc

        virtualDisplay = proj.createVirtualDisplay(
            "WebStreamVR-Capture",
            s.width, s.height, 1,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            inputSurface, null, null,
        )

        listener.onLayerConfigured(s.width, s.height)
        listener.onStateChanged(CaptureState.STREAMING, null)
        drainThread = Thread({ drainLoop(enc) }, "ScreenCaptureLink-drain").apply { start() }
    }

    private fun drainLoop(enc: MediaCodec) {
        val info = MediaCodec.BufferInfo()
        while (running) {
            val idx = try {
                enc.dequeueOutputBuffer(info, 100_000)
            } catch (e: Exception) {
                if (running) Log.w(TAG, "dequeueOutputBuffer failed", e)
                break
            }
            if (idx >= 0) {
                val buf = enc.getOutputBuffer(idx)
                if (buf != null && info.size > 0) {
                    val data = ByteArray(info.size)
                    buf.position(info.offset)
                    buf.get(data, 0, info.size)
                    listener.onVideo(data, 0, data.size, info.presentationTimeUs)
                }
                try {
                    enc.releaseOutputBuffer(idx, false)
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun teardown() {
        drainThread?.interrupt()
        drainThread = null
        try {
            codec?.stop()
        } catch (_: Exception) {
        }
        try {
            codec?.release()
        } catch (_: Exception) {
        }
        codec = null
        try {
            virtualDisplay?.release()
        } catch (_: Exception) {
        }
        virtualDisplay = null
        try {
            projection?.stop()
        } catch (_: Exception) {
        }
        projection = null
    }

    companion object {
        private const val TAG = "ScreenCaptureLink"
    }
}
