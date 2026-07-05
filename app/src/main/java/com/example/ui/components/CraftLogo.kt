package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
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
 * Full Craft Innovations Logo utilizing the uploaded high-resolution
 * official "Logo 2 (1).png" (logo_brand) branding asset.
 */
@Composable
fun CraftLogo(
    modifier: Modifier = Modifier,
    textColor: Color = Color.Unspecified,
    iconSize: Int = 40,
    showSubtitle: Boolean = true
) {
    Box(
        modifier = modifier
            .padding(vertical = 4.dp)
            .height(iconSize.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_brand),
            contentDescription = "Craft Innovations Official Logo",
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(4.35f), // Maintain standard corporate landscape proportions
            contentScale = ContentScale.Fit
        )
    }
}
