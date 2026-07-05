package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CraftCobaltBlue
import com.example.ui.theme.CraftLavender

/**
 * Geometric, high-fidelity native vector canvas implementation of the
 * official Craft Innovations interlocking logo symbol.
 */
@Composable
fun CraftLogoIcon(
    modifier: Modifier = Modifier,
    primaryColor: Color = CraftCobaltBlue,
    secondaryColor: Color = CraftLavender
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Top-Right Bracket (C-upper part)
        val upperPath = Path().apply {
            moveTo(w * 0.20f, h * 0.15f)
            lineTo(w * 0.80f, h * 0.15f)
            lineTo(w * 0.80f, h * 0.40f)
            lineTo(w * 0.55f, h * 0.40f)
            lineTo(w * 0.40f, h * 0.55f)
            lineTo(w * 0.20f, h * 0.35f)
            close()
        }
        drawPath(upperPath, color = primaryColor)

        // Bottom-Left Bracket (Stem & lower part)
        val lowerPath = Path().apply {
            moveTo(w * 0.20f, h * 0.35f)
            lineTo(w * 0.40f, h * 0.55f)
            lineTo(w * 0.25f, h * 0.70f)
            lineTo(w * 0.45f, h * 0.90f)
            lineTo(w * 0.80f, h * 0.90f)
            lineTo(w * 0.55f, h * 0.65f)
            lineTo(w * 0.80f, h * 0.65f)
            lineTo(w * 0.80f, h * 0.40f)
            lineTo(w * 0.55f, h * 0.40f)
            close()
        }
        drawPath(lowerPath, color = secondaryColor)
    }
}

/**
 * Full Craft Innovations Logo with Logotype and NIGERIA LIMITED subtitle
 * adhering strictly to the "Clear Spacing" guidelines.
 */
@Composable
fun CraftLogo(
    modifier: Modifier = Modifier,
    textColor: Color = Color.Unspecified,
    iconSize: Int = 40,
    showSubtitle: Boolean = true
) {
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CraftLogoIcon(
            modifier = Modifier.size(iconSize.dp),
            primaryColor = MaterialTheme.colorScheme.primary,
            secondaryColor = MaterialTheme.colorScheme.secondary
        )
        
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = "Craft Innovations",
                fontStyle = MaterialTheme.typography.titleLarge.fontStyle,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = textColor,
                letterSpacing = (-0.5).sp
            )
            if (showSubtitle) {
                Text(
                    text = "NIGERIA LIMITED",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor.copy(alpha = 0.7f),
                    letterSpacing = 4.5.sp, // Wide tracking as requested in branding guide
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }
    }
}
