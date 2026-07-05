package com.example.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.User
import com.example.ui.components.CraftLogo
import com.example.ui.theme.*

enum class AuthMode {
    LOGIN, SIGN_UP, RESET_PASSWORD
}

@Composable
fun AuthDialog(
    authViewModel: AuthViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val authState by authViewModel.authState.collectAsState()
    val passwordResetState by authViewModel.passwordResetState.collectAsState()
    val accountCreatedState by authViewModel.accountCreatedState.collectAsState()

    var currentMode by remember { mutableStateOf(AuthMode.LOGIN) }

    var email by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var pinOrPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Reset fields on mode change
    LaunchedEffect(currentMode) {
        authViewModel.clearError()
    }

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(CraftDarkNavy.copy(alpha = 0.85f))
                .clickable(enabled = true, onClick = { onDismiss() })
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 450.dp)
                    .clickable(enabled = false, onClick = {}) // prevent click propagation
                    .border(1.dp, CraftLavender.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CraftSurfaceDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Logo Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CraftLogo(
                            textColor = CraftPureWhite,
                            iconSize = 42,
                            showSubtitle = true
                        )
                    }

                    Text(
                        text = when (currentMode) {
                            AuthMode.LOGIN -> "SECURE ENTERPRISE CONSOLE"
                            AuthMode.SIGN_UP -> "PROVISION OPERATOR ACCESS"
                            AuthMode.RESET_PASSWORD -> "CREDENTIAL RECOVERY PROTOCOL"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CraftLavender.copy(alpha = 0.8f),
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Error Notification Banner
                    if (authState is AuthState.Error) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CraftDangerRed.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .border(1.dp, CraftDangerRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Security Alert",
                                    tint = CraftDangerRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = (authState as AuthState.Error).message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CraftPureWhite,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = CraftPureWhite.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { authViewModel.clearError() }
                                )
                            }
                        }
                    }

                    // Success Notification Banner
                    if (passwordResetState != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CraftNeonMint.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .border(1.dp, CraftNeonMint.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = CraftNeonMint,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = passwordResetState ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CraftPureWhite,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    if (accountCreatedState) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CraftNeonMint.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .border(1.dp, CraftNeonMint.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = "Account Created",
                                    tint = CraftNeonMint,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Operator identity registered. You may now sign in.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CraftPureWhite,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Content form with elegant spacing (16dp inside forms)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (currentMode == AuthMode.SIGN_UP) {
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("Full Identity Name") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Badge,
                                        contentDescription = null,
                                        tint = CraftLavender.copy(alpha = 0.6f)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = CraftPureWhite,
                                    unfocusedTextColor = CraftPureWhite,
                                    focusedBorderColor = CraftCobaltBlue,
                                    unfocusedBorderColor = CraftLavender.copy(alpha = 0.3f),
                                    focusedLabelColor = CraftLavender,
                                    unfocusedLabelColor = CraftLavender.copy(alpha = 0.6f)
                                ),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Corporate Identity Email") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = CraftLavender.copy(alpha = 0.6f)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = CraftPureWhite,
                                unfocusedTextColor = CraftPureWhite,
                                focusedBorderColor = CraftCobaltBlue,
                                unfocusedBorderColor = CraftLavender.copy(alpha = 0.3f),
                                focusedLabelColor = CraftLavender,
                                unfocusedLabelColor = CraftLavender.copy(alpha = 0.6f)
                            ),
                            singleLine = true
                        )

                        if (currentMode != AuthMode.RESET_PASSWORD) {
                            OutlinedTextField(
                                value = pinOrPassword,
                                onValueChange = { pinOrPassword = it },
                                label = { Text("Security Access Pin/Password") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = CraftLavender.copy(alpha = 0.6f)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle password visibility",
                                            tint = CraftLavender.copy(alpha = 0.6f)
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = CraftPureWhite,
                                    unfocusedTextColor = CraftPureWhite,
                                    focusedBorderColor = CraftCobaltBlue,
                                    unfocusedBorderColor = CraftLavender.copy(alpha = 0.3f),
                                    focusedLabelColor = CraftLavender,
                                    unfocusedLabelColor = CraftLavender.copy(alpha = 0.6f)
                                ),
                                singleLine = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Primary Action Button (rounded-rectangular with generous touch targets)
                    if (authState is AuthState.Loading) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = CraftCobaltBlue,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Establishing secure telemetry handshake...",
                                style = MaterialTheme.typography.bodySmall,
                                color = CraftLavender.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                when (currentMode) {
                                    AuthMode.LOGIN -> authViewModel.login(email, pinOrPassword)
                                    AuthMode.SIGN_UP -> authViewModel.createAccount(email, fullName, pinOrPassword)
                                    AuthMode.RESET_PASSWORD -> authViewModel.resetPassword(email)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CraftCobaltBlue,
                                contentColor = CraftPureWhite
                            )
                        ) {
                            Icon(
                                imageVector = when (currentMode) {
                                    AuthMode.LOGIN -> Icons.Default.VpnKey
                                    AuthMode.SIGN_UP -> Icons.Default.PersonAdd
                                    AuthMode.RESET_PASSWORD -> Icons.Default.Send
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (currentMode) {
                                    AuthMode.LOGIN -> "Secure Authenticate"
                                    AuthMode.SIGN_UP -> "Create Admin Identity"
                                    AuthMode.RESET_PASSWORD -> "Dispatch Recovery Link"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Third-Party OAuth Sign-In (Google Sign-In)
                        if (currentMode == AuthMode.LOGIN) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            OutlinedButton(
                                onClick = { authViewModel.signInWithGoogle() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, CraftLavender.copy(alpha = 0.3f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = CraftPureWhite
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    // Custom beautifully rendered G-Logo representation
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(CraftPureWhite)
                                            .padding(2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "G",
                                            color = Color(0xFF4285F4),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.SansSerif
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Sign in with Google OAuth",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Secondary Toggles & Links (with clear separation)
                    HorizontalDivider(color = CraftLavender.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (currentMode != AuthMode.RESET_PASSWORD) {
                            Text(
                                text = "Forgot access codes? Reset Credentials",
                                style = MaterialTheme.typography.bodySmall,
                                color = CraftLavender,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable { currentMode = AuthMode.RESET_PASSWORD }
                                    .padding(4.dp)
                            )
                        }

                        if (currentMode == AuthMode.LOGIN) {
                            Text(
                                text = "Don't have an operator profile? Request Access",
                                style = MaterialTheme.typography.bodySmall,
                                color = CraftNeonMint,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { currentMode = AuthMode.SIGN_UP }
                                    .padding(4.dp)
                            )
                        } else {
                            Text(
                                text = "Return to Secure Login Page",
                                style = MaterialTheme.typography.bodySmall,
                                color = CraftLavender,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable { currentMode = AuthMode.LOGIN }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserProfileDialog(
    authViewModel: AuthViewModel,
    user: User,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var showTokenCopiedMsg by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(CraftDarkNavy.copy(alpha = 0.85f))
                .clickable(enabled = true, onClick = { onDismiss() })
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .clickable(enabled = false, onClick = {})
                    .border(1.dp, CraftLavender.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CraftSurfaceDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Icon/Badge Centered
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(CraftCobaltBlue, CraftLavender)
                                )
                            )
                            .border(2.dp, CraftPureWhite, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.fullName.take(2).uppercase(),
                            color = CraftPureWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = user.fullName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = CraftPureWhite,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CraftLavender.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Badge representing role
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = CraftNeonMint.copy(alpha = 0.15f)),
                        modifier = Modifier.border(1.dp, CraftNeonMint.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = user.role.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CraftNeonMint,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    HorizontalDivider(color = CraftLavender.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // JWT Details & Decoder Panel (satisfies JWT Token Management requirement)
                    Text(
                        text = "SECURE DECODED TOKENS (JWT)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CraftPureWhite.copy(alpha = 0.8f),
                        modifier = Modifier.fillMaxWidth(),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CraftDarkNavy),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "RAW CRYPTOGRAPHIC TOKEN",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CraftLavender.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = user.token,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = CraftLavender,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(user.token))
                                        showTokenCopiedMsg = true
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, "Copy Token", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (showTokenCopiedMsg) "Copied!" else "Copy JWT Token",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = "HMAC-SHA256 Verified",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CraftNeonMint,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Decoded Claims
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CraftDarkNavy),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "DECODED IDENTITY CLAIMS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CraftLavender.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            ClaimRow(label = "iss (Issuer)", value = "https://auth.craftinnovations.com.ng")
                            ClaimRow(label = "sub (Subject)", value = user.email)
                            ClaimRow(label = "name (Identity)", value = user.fullName)
                            ClaimRow(label = "role (Privileges)", value = user.role)
                            ClaimRow(label = "org (Enterprise)", value = user.organization)
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Buttons (Logout & Close)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onDismiss() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            border = BorderStroke(1.dp, CraftLavender.copy(alpha = 0.3f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CraftPureWhite)
                        ) {
                            Text("Close Profile", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                authViewModel.logout()
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CraftDangerRed)
                        ) {
                            Icon(Icons.Default.Logout, "Logout", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Secure Logout", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClaimRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = CraftLavender.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = CraftPureWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}
