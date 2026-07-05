package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.dashboard.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAssistantScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val aiResponse by viewModel.aiResponse.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    val devices by viewModel.devices.collectAsState()

    var promptInput by remember { mutableStateOf("") }

    val quickPrompts = listOf(
        "Diagnose Rover Robot Car",
        "Generate servo sweep ESP32 C++ code",
        "Suggest automation rules for Hydroponics",
        "Explain MQTT TLS secure handshake"
    )

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("ai_assistant_root"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Page header
        item {
            Column {
                Text(
                    text = "CraftIoT AI Copilot",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Automate device workflows, inspect raw register dumps, and generate hardware-level firmware code.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        // Live telemetry feed state notice
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Insights, "Insights", tint = MaterialTheme.colorScheme.secondary)
                    Text(
                        text = "Real-time Telemetry context from ${devices.size} active nodes is automatically injected into the AI's system window.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Quick prompts row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "QUICK DIAGNOSTIC CONTEXTS",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickPrompts.take(2).forEach { qp ->
                        SuggestionChip(
                            onClick = {
                                promptInput = qp
                                viewModel.queryAiAssistant(qp)
                            },
                            label = { Text(qp, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickPrompts.drop(2).forEach { qp ->
                        SuggestionChip(
                            onClick = {
                                promptInput = qp
                                viewModel.queryAiAssistant(qp)
                            },
                            label = { Text(qp, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Text input card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        label = { Text("Ask the IoT Engineer CoPilot...") },
                        placeholder = { Text("e.g. Help me optimize greenhouse power usage") },
                        modifier = Modifier.fillMaxWidth().testTag("ai_prompt_input_field"),
                        maxLines = 4
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (aiResponse != null) {
                            TextButton(
                                onClick = {
                                    viewModel.clearAiResponse()
                                    promptInput = ""
                                },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text("Clear Chat")
                            }
                        }

                        Button(
                            onClick = { viewModel.queryAiAssistant(promptInput) },
                            enabled = promptInput.isNotBlank() && !aiLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("ai_submit_btn")
                        ) {
                            if (aiLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Send, "Send", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Consult", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Live scrolling response window
        item {
            AnimatedVisibility(
                visible = aiResponse != null || aiLoading,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("ai_response_panel"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoAwesome, "AI Core", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                            }
                            Text(
                                text = "CO-PILOT ENGINEERING ADVISORY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }

                        if (aiLoading) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Analyzing register values & compiling insights...", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        } else {
                            Text(
                                text = aiResponse ?: "",
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
