package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SensorLog
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TelemetryChart(
    logs: List<SensorLog>,
    metricName: String,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.secondary,
    gridColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
) {
    val displayMetric = metricName.uppercase()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        if (logs.size < 2) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Awaiting Live Telemetry Stream...",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
                Text(
                    text = "Ensure simulated ESP32 devices are active",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    fontSize = 11.sp
                )
            }
        } else {
            val maxVal = logs.maxOfOrNull { it.value } ?: 100f
            val minVal = logs.minOfOrNull { it.value } ?: 0f
            val diff = (maxVal - minVal).coerceAtLeast(1f)
            val paddingMultiplier = 1.1f
            val adjustedMax = maxVal + (diff * 0.05f)
            val adjustedMin = (minVal - (diff * 0.05f)).coerceAtLeast(0f)
            val finalRange = (adjustedMax - adjustedMin).coerceAtLeast(1f)

            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$displayMetric REAL-TIME HISTORY",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = lineColor,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "Avg: %.1f | Max: %.1f".format(logs.map { it.value }.average(), maxVal),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Chart Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val width = size.width
                    val height = size.height

                    // Draw Horizontal Grid lines (3 lines)
                    val gridLinesCount = 3
                    for (i in 0..gridLinesCount) {
                        val y = (height / gridLinesCount) * i
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Map points
                    val sortedLogs = logs.sortedBy { it.timestamp }
                    val points = ArrayList<Offset>(sortedLogs.size)
                    for (index in sortedLogs.indices) {
                        val log = sortedLogs[index]
                        val x = (width / (sortedLogs.size - 1)) * index
                        val y = height - ((log.value - adjustedMin) / finalRange) * height
                        points.add(Offset(x, y))
                    }

                    // Draw Gradient Fill under the line
                    val fillPath = Path().apply {
                        val firstPt = points.first()
                        val lastPt = points.last()
                        moveTo(firstPt.x, height)
                        for (pt in points) {
                            lineTo(pt.x, pt.y)
                        }
                        lineTo(lastPt.x, height)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                lineColor.copy(alpha = 0.25f),
                                lineColor.copy(alpha = 0.0f)
                            ),
                            startY = 0f,
                            endY = height
                        )
                    )

                    // Draw main line path
                    val linePath = Path().apply {
                        val firstPt = points.first()
                        moveTo(firstPt.x, firstPt.y)
                        for (i in 1 until points.size) {
                            val prev = points[i - 1]
                            val curr = points[i]
                            // Cubic curve for ultra smooth rendering
                            val cp1X = prev.x + (curr.x - prev.x) / 2f
                            val cp1Y = prev.y
                            val cp2X = prev.x + (curr.x - prev.x) / 2f
                            val cp2Y = curr.y
                            cubicTo(cp1X, cp1Y, cp2X, cp2Y, curr.x, curr.y)
                        }
                    }

                    drawPath(
                        path = linePath,
                        color = lineColor,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw individual telemetry dots
                    for (pt in points) {
                        // Draw outer glow
                        drawCircle(
                            color = lineColor.copy(alpha = 0.4f),
                            radius = 5.dp.toPx(),
                            center = pt
                        )
                        // Draw inner core
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = pt
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Footer showing timestamps
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val firstTime = sdf.format(Date(logs.first().timestamp))
                    val lastTime = sdf.format(Date(logs.last().timestamp))

                    Text(firstTime, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Text("LIVE FEED", fontSize = 9.sp, color = lineColor.copy(alpha = 0.8f), fontFamily = FontFamily.Monospace)
                    Text(lastTime, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
        }
    }
}
