package xyz.lukioff.webstreamvr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.lukioff.webstreamvr.AspectRatio
import xyz.lukioff.webstreamvr.R
import xyz.lukioff.webstreamvr.StreamSettings
import xyz.lukioff.webstreamvr.StreamStatus
import xyz.lukioff.webstreamvr.cast.CaptureState
import xyz.lukioff.webstreamvr.ui.theme.NebulaCyan
import xyz.lukioff.webstreamvr.ui.theme.QuasarOrange
import xyz.lukioff.webstreamvr.ui.theme.StatusAmber
import xyz.lukioff.webstreamvr.ui.theme.StatusGray
import xyz.lukioff.webstreamvr.ui.theme.StatusGreen
import xyz.lukioff.webstreamvr.ui.theme.StatusRed

@Composable
fun MainScreen(
    status: StreamStatus,
    settings: StreamSettings,
    onSettingsChange: (StreamSettings) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header()
            StatusCard(status = status, onStart = onStart, onStop = onStop)
            SettingsCard(settings, onSettingsChange)
            Text(
                stringResource(R.string.hint_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Header() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(QuasarOrange),
        )
        Spacer(Modifier.size(12.dp))
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = NebulaCyan,
        )
    }
}

@Composable
private fun StatusCard(
    status: StreamStatus,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(stateColor(status)),
                )
                Spacer(Modifier.size(10.dp))
                Text(stateText(status), style = MaterialTheme.typography.titleMedium)
            }
            status.detail?.let { detail ->
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
            if (status.running && status.headset == CaptureState.STREAMING) {
                Text(
                    stringResource(
                        R.string.stats_line,
                        status.width, status.height, status.fps, status.kbps / 1000f, status.viewers,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (status.running) {
                status.webError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                status.streamCode?.let { code ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                stringResource(R.string.stream_code_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                code,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 6.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
                if (status.urls.isNotEmpty()) {
                    Text(stringResource(R.string.open_in_browser), style = MaterialTheme.typography.bodyMedium)
                    SelectionContainer {
                        Column {
                            status.urls.forEach {
                                Text(it, style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Monospace, color = NebulaCyan)
                            }
                        }
                    }
                }
            }
            if (status.running) {
                OutlinedButton(
                    onClick = onStop,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text(stringResource(R.string.action_stop)) }
            } else {
                Button(
                    onClick = onStart,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = QuasarOrange),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text(stringResource(R.string.action_start), fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsCard(settings: StreamSettings, onChange: (StreamSettings) -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleMedium)

            Setting(stringResource(R.string.setting_aspect)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AspectRatio.entries.forEach { aspect ->
                        FilterChip(
                            selected = settings.aspect == aspect,
                            onClick = { onChange(settings.copy(aspect = aspect)) },
                            label = { Text(aspect.label) },
                        )
                    }
                }
            }

            val (frameWidth, frameHeight) = settings.frameSize
            Setting(stringResource(R.string.setting_quality, frameWidth, frameHeight)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StreamSettings.QUALITIES.forEach { quality ->
                        FilterChip(
                            selected = settings.quality == quality,
                            onClick = { onChange(settings.copy(quality = quality)) },
                            label = { Text("${quality}p") },
                        )
                    }
                }
            }

            Setting(stringResource(R.string.setting_bitrate), stringResource(R.string.setting_bitrate_hint)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StreamSettings.BITRATES_MBPS.forEach { mbps ->
                        FilterChip(
                            selected = settings.bitrateMbps == mbps,
                            onClick = { onChange(settings.copy(bitrateMbps = mbps)) },
                            label = { Text(if (mbps == 0) stringResource(R.string.bitrate_auto) else stringResource(R.string.bitrate_mbps, mbps)) },
                        )
                    }
                }
            }

            Setting(stringResource(R.string.setting_frame_rate), stringResource(R.string.setting_frame_rate_hint)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StreamSettings.FRAME_RATES.forEach { fps ->
                        FilterChip(
                            selected = settings.frameRate == fps,
                            onClick = { onChange(settings.copy(frameRate = fps)) },
                            label = { Text("$fps") },
                        )
                    }
                }
            }

            SwitchRow(stringResource(R.string.setting_audio), settings.audio) {
                onChange(settings.copy(audio = it))
            }

            Setting(stringResource(R.string.setting_port)) {
                var text by remember(settings.httpPort) { mutableStateOf(settings.httpPort.toString()) }
                val port = text.toIntOrNull()
                val valid = port != null && port in 1024..65535
                OutlinedTextField(
                    value = text,
                    onValueChange = { value ->
                        text = value.filter(Char::isDigit).take(5)
                        text.toIntOrNull()?.takeIf { it in 1024..65535 }?.let { onChange(settings.copy(httpPort = it)) }
                    },
                    singleLine = true,
                    isError = !valid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { if (!valid) Text(stringResource(R.string.setting_port_invalid)) },
                )
            }
        }
    }
}

@Composable
private fun Setting(title: String, hint: String? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        hint?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        content()
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun stateText(status: StreamStatus): String = when {
    !status.running -> stringResource(R.string.state_stopped)
    else -> when (status.headset) {
        CaptureState.IDLE, CaptureState.STARTING -> stringResource(R.string.state_starting)
        CaptureState.STREAMING -> stringResource(R.string.state_streaming)
        CaptureState.ERROR -> stringResource(R.string.state_error)
    }
}

private fun stateColor(status: StreamStatus): Color = when {
    !status.running -> StatusGray
    status.headset == CaptureState.STREAMING -> StatusGreen
    status.headset == CaptureState.ERROR -> StatusRed
    else -> StatusAmber
}
