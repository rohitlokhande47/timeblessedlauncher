package com.example.timeblessedlauncher.Screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.timeblessedlauncher.Data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    app: AppInfo,
    currentRestriction: AppRestriction?,
    onSave: (TimeRange) -> Unit,
    onDismiss: () -> Unit
) {
    var fromHour by remember {
        mutableIntStateOf(currentRestriction?.showFromHour ?: 9)
    }
    var fromMinute by remember { mutableIntStateOf(0) }
    var toHour by remember {
        mutableIntStateOf(currentRestriction?.showUntilHour ?: 17)
    }
    var toMinute by remember { mutableIntStateOf(0) }

    val timeRange = TimeRange(fromHour, fromMinute, toHour, toMinute)
    val isValidRange = timeRange.isValid()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Set Time Restriction",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = app.name,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Info text
                Text(
                    text = "Choose when this app will be available (1-2 hours only)",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // From Time
                Text(
                    text = "Available From:",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                TimeSelector(
                    hour = fromHour,
                    minute = fromMinute,
                    onHourChange = { fromHour = it },
                    onMinuteChange = { fromMinute = it },
                    label = "Start Time"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // To Time
                Text(
                    text = "Available Until:",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                TimeSelector(
                    hour = toHour,
                    minute = toMinute,
                    onHourChange = { toHour = it },
                    onMinuteChange = { toMinute = it },
                    label = "End Time"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Preview
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isValidRange) "Preview:" else "Invalid Range:",
                        color = if (isValidRange) Color.White else Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (isValidRange) {
                        Text(
                            text = "Available: ${timeRange}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Duration: ${String.format("%.1f", timeRange.getDurationHours())} hours",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    } else {
                        val duration = timeRange.getDurationHours()
                        val errorMessage = when {
                            duration < 1f -> "Duration must be at least 1 hour"
                            duration > 2f -> "Duration cannot exceed 2 hours"
                            else -> "Invalid time range"
                        }
                        Text(
                            text = errorMessage,
                            color = Color.Red.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (isValidRange) {
                                onSave(timeRange)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isValidRange,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                            disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Restriction")
                    }
                }
            }
        }
    }
}

@Composable
fun TimeSelector(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    label: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hour selector
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Hour",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { onHourChange((hour - 1 + 24) % 24) }
                ) {
                    Text("-", color = Color.White, fontSize = 20.sp)
                }

                Text(
                    text = String.format("%02d", hour),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.Center
                )

                TextButton(
                    onClick = { onHourChange((hour + 1) % 24) }
                ) {
                    Text("+", color = Color.White, fontSize = 20.sp)
                }
            }
        }

        Text(
            text = ":",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        // Minute selector
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Minute",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { onMinuteChange((minute - 15 + 60) % 60) }
                ) {
                    Text("-", color = Color.White, fontSize = 20.sp)
                }

                Text(
                    text = String.format("%02d", minute),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.Center
                )

                TextButton(
                    onClick = { onMinuteChange((minute + 15) % 60) }
                ) {
                    Text("+", color = Color.White, fontSize = 20.sp)
                }
            }
        }
    }
}