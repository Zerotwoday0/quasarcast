package xyz.lukioff.webstreamvr

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import xyz.lukioff.webstreamvr.cast.ScreenCaptureLink
import kotlin.math.sqrt

/** Frame shape. The headset's screen is fit into whatever surface size we request. */
enum class AspectRatio(val width: Int, val height: Int, val label: String) {
    WIDE(16, 9, "16:9"),
    CLASSIC(4, 3, "4:3"),
    SQUARE(1, 1, "1:1"),
    PORTRAIT(9, 16, "9:16"),
    /** The headset's own display buffer, close to its native shape. */
    HEADSET(10, 11, "Как в шлеме"),
}

data class StreamSettings(
    val aspect: AspectRatio = AspectRatio.WIDE,
    /** Short side of the frame in pixels, see [QUALITIES]. */
    val quality: Int = 1080,
    val frameRate: Int = 60,
    /** 0 lets the encoder pick the bitrate itself. */
    val bitrateMbps: Int = 8,
    val audio: Boolean = true,
    val httpPort: Int = DEFAULT_PORT,
) {
    /** Encoder size for this device; keeps the aspect ratio when it has to shrink. */
    val frameSize: Pair<Int, Int> get() = frameSize(aspect, quality, maxEncoderPixels())

    fun toCaptureSettings(): ScreenCaptureLink.Settings {
        val (width, height) = frameSize
        return ScreenCaptureLink.Settings(
            width = width,
            height = height,
            bitrate = if (bitrateMbps > 0) bitrateMbps * 1_000_000 else 8_000_000,
            fps = frameRate,
            audio = audio,
        )
    }

    companion object {
        const val DEFAULT_PORT = 8080
        val QUALITIES = listOf(720, 1080, 1440)
        val BITRATES_MBPS = listOf(0, 4, 6, 8, 12, 16, 20)
        val FRAME_RATES = listOf(30, 60)

        private const val PREFS = "stream_settings"

        /**
         * Width and height for [aspect] with [shortSide] pixels on the short side, scaled
         * down to [maxPixels] and rounded to multiples of 8 for the encoder.
         */
        fun frameSize(aspect: AspectRatio, shortSide: Int, maxPixels: Int): Pair<Int, Int> {
            var width: Double
            var height: Double
            if (aspect.width >= aspect.height) {
                height = shortSide.toDouble()
                width = height * aspect.width / aspect.height
            } else {
                width = shortSide.toDouble()
                height = width * aspect.height / aspect.width
            }
            if (width * height > maxPixels) {
                val scale = sqrt(maxPixels / (width * height))
                width *= scale
                height *= scale
            }
            return (width.toInt() / 8 * 8) to (height.toInt() / 8 * 8)
        }

        /** Pixel budget for the encoder (native screen resolution per device). */
        fun maxEncoderPixels(device: String = Build.DEVICE): Int = when (device) {
            "eureka", "panther" -> 2560 * 1440 // Quest 3, Quest 3S
            else -> 1920 * 1080 // Quest 2, Quest Pro
        }

        fun generateRandomPort(): Int = (1024..65535).random()

        fun load(context: Context): StreamSettings {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val defaults = StreamSettings()
            val savedPort = if (p.contains("httpPort")) p.getInt("httpPort", 0).takeIf { it in 1024..65535 } else null
            val httpPort = savedPort ?: generateRandomPort().also { port ->
                p.edit { putInt("httpPort", port) }
            }
            return StreamSettings(
                aspect = AspectRatio.entries.firstOrNull { it.name == p.getString("aspect", null) } ?: defaults.aspect,
                quality = p.getInt("quality", defaults.quality).takeIf { it in QUALITIES } ?: defaults.quality,
                frameRate = p.getInt("frameRate", defaults.frameRate),
                bitrateMbps = p.getInt("bitrateMbps", defaults.bitrateMbps),
                audio = p.getBoolean("audio", defaults.audio),
                httpPort = httpPort,
            )
        }

        fun save(context: Context, settings: StreamSettings) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
                putString("aspect", settings.aspect.name)
                putInt("quality", settings.quality)
                putInt("frameRate", settings.frameRate)
                putInt("bitrateMbps", settings.bitrateMbps)
                putBoolean("audio", settings.audio)
                putInt("httpPort", settings.httpPort)
            }
        }
    }
}
