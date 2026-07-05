package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * 1. Main App Bar Logo Implementation
 * Renders the horizontal brand logo inside a TopAppBar or screen header.
 * Adheres strictly to 'Clear Spacing' principles with 16dp minimum surrounding safe bounds.
 */
@Composable
fun CraftBrandAppBarLogo(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp), // 16dp horizontal clear spacing padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_brand),
            contentDescription = "Craft Innovations Logo",
            modifier = Modifier
                .height(36.dp) // Optimized height to balance top bar layout height limits
                .aspectRatio(4.2f), // Matches the natural landscape aspect ratio of the brand logo
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * 2. Login Screen Header Implementation
 * Renders a bold, centered hero logo section for onboarding or authentication screens.
 * Leverages generous vertical spacing and clear separation to preserve brand distinction.
 */
@Composable
fun CraftBrandLoginHeader(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 24.dp), // Generous spacing to allow the brand to breathe
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Centered Brand Logo with high-resolution scaling
        Image(
            painter = painterResource(id = R.drawable.logo_brand),
            contentDescription = "Craft Innovations Corporate Identity",
            modifier = Modifier
                .height(64.dp) // Elevated height for high visual prominence on authentication screens
                .fillMaxWidth(0.85f), // Safe boundaries ensuring zero horizontal clipping on small screens
            contentScale = ContentScale.Fit
        )
        
        // Complementary typography with balanced negative space
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Welcome to CraftIoT",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Secure local SQLite offline synchronization & hardware-level simulation grid controls.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
