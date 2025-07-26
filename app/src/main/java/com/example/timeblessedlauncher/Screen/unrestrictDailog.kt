package com.example.timeblessedlauncher.Screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.timeblessedlauncher.Data.AppInfo
import kotlinx.coroutines.delay

data class UnrestrictQuestion(
    val question: String,
    val subtitle: String
)

@Composable
fun UnrestrictDialog(
    app: AppInfo,
    onUnrestrict: () -> Unit,
    onCancel: () -> Unit
) {
    val allQuestions = remember {
        listOf(
            UnrestrictQuestion(
                "Oh wow, did you finally cure boredom with productivity... or are you just back here again?",
                "\"Yes\" = hmm, maybe just a scroll. \"No\" = back to work, champ."
            ),
            UnrestrictQuestion(
                "Is this really urgent, or is your thumb just bored again?",
                "Be honest with yourself here..."
            ),
            UnrestrictQuestion(
                "Are you opening this because you're changing the world... or just avoiding your to-do list (again)?",
                "Your future self is watching..."
            ),
            UnrestrictQuestion(
                "Did Instagram personally text you saying they miss you? No? Then why are you here?",
                "They'll survive without you for a few more hours"
            ),
            UnrestrictQuestion(
                "Have you already wasted enough time today, or do you want to go for a new personal record?",
                "Setting new records isn't always good..."
            ),
            UnrestrictQuestion(
                "Would your \"I'm gonna be successful\" self approve of this decision?",
                "You know, that motivated version of you from last week"
            ),
            UnrestrictQuestion(
                "Is this part of your 10-year plan, or your 10-second impulse?",
                "Long-term vision vs short-term distraction"
            ),
            UnrestrictQuestion(
                "Are you looking for inspiration or just more memes you'll forget in 5 minutes?",
                "Be real about what you're actually going to do"
            ),
            UnrestrictQuestion(
                "Is your attention span officially under threat, or just having a snack break?",
                "Some breaks turn into 3-hour Netflix sessions..."
            ),
            UnrestrictQuestion(
                "On a scale of 1 to \"doomscroll till regret,\" how proud will you feel after this?",
                "Future you is either going to thank you or facepalm"
            ),
            UnrestrictQuestion(
                "Are you about to make your screen time stats cry, or is this actually important?",
                "Your weekly report is judging you already"
            ),
            UnrestrictQuestion(
                "Is this the \"productive break\" you promised yourself, or just procrastination in disguise?",
                "We both know how this usually ends..."
            ),
            UnrestrictQuestion(
                "Did you suddenly become immune to the dopamine trap, or are you walking right into it?",
                "The algorithm is waiting for you with personalized distractions"
            ),
            UnrestrictQuestion(
                "Are you opening this to learn something new, or to forget something important?",
                "Knowledge vs. escapism - choose wisely"
            ),
            UnrestrictQuestion(
                "Is this your brain asking for a reward, or just your fingers being restless?",
                "Sometimes our hands move faster than our thoughts"
            )
        )
    }

    // Randomize and select 5 questions each time dialog opens
    val selectedQuestions = remember {
        allQuestions.shuffled().take(10)
    }

    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var isVisible by remember { mutableStateOf(true) }

    if (isVisible) {
        Dialog(
            onDismissRequest = onCancel,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            AnimatedContent(
                targetState = currentQuestionIndex,
                transitionSpec = {
                    (slideInHorizontally(
                        initialOffsetX = { width -> width },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))) togetherWith
                            (slideOutHorizontally(
                                targetOffsetX = { width -> -width },
                                animationSpec = tween(300)
                            ) + fadeOut(animationSpec = tween(300)))
                },
                label = "question_transition"
            ) { questionIndex ->
                QuestionCard(
                    app = app,
                    question = selectedQuestions[questionIndex],
                    questionNumber = questionIndex + 1,
                    totalQuestions = selectedQuestions.size,
                    onYes = {
                        if (questionIndex < selectedQuestions.size - 1) {
                            // Move to next question
                            currentQuestionIndex++
                        } else {
                            // All questions answered with "Yes" - unrestrict the app
                            isVisible = false
                            onUnrestrict()
                        }
                    },
                    onNo = {
                        // User said "No" - keep restriction and close dialog
                        isVisible = false
                        onCancel()
                    },
                    onSkipAll = {
                        // Skip all remaining questions and unrestrict
                        isVisible = false
                        onUnrestrict()
                    }
                )
            }
        }
    }
}

@Composable
fun QuestionCard(
    app: AppInfo,
    question: UnrestrictQuestion,
    questionNumber: Int,
    totalQuestions: Int,
    onYes: () -> Unit,
    onNo: () -> Unit,
    onSkipAll: () -> Unit
) {
    var timeLeft by remember { mutableIntStateOf(5) }
    var isTimerComplete by remember { mutableStateOf(false) }

    // Timer effect
    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
        isTimerComplete = true
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A2E)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header with app info and progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // App icon
                    val (color1, color2) = Pair(0xFF667eea, 0xFF764ba2)

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(color1),
                                        Color(color2)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = app.name.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = app.name,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Wants to be unrestricted",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }

                // Progress indicator
                Text(
                    text = "$questionNumber/$totalQuestions",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Timer display
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isTimerComplete)
                        Color.Green.copy(alpha = 0.1f)
                    else
                        Color(0xFFFF6B35).copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isTimerComplete) Icons.Default.CheckCircle else Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (isTimerComplete) Color.Green else Color(0xFFFF6B35),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTimerComplete)
                                "Time to reflect complete ✓"
                            else
                                "Take a moment to think... $timeLeft seconds",
                            color = if (isTimerComplete) Color.Green else Color(0xFFFF6B35),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Circular timer indicator
                    Box(
                        modifier = Modifier.size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { (5 - timeLeft) / 5f },
                            modifier = Modifier.size(24.dp),
                            color = if (isTimerComplete) Color.Green else Color(0xFFFF6B35),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = if (isTimerComplete) "✓" else timeLeft.toString(),
                            color = if (isTimerComplete) Color.Green else Color(0xFFFF6B35),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { questionNumber.toFloat() / totalQuestions.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF667eea),
                trackColor = Color.White.copy(alpha = 0.1f),
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Question card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2D2D44)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Question icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = Color(0xFFFF6B35),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Honest moment:",
                            color = Color(0xFFFF6B35),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Main question
                    Text(
                        text = question.question,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Subtitle
                    Text(
                        text = question.subtitle,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Yes button (continue/unrestrict) - disabled until timer completes
                Button(
                    onClick = onYes,
                    enabled = isTimerComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (questionNumber == totalQuestions) Icons.Default.LockOpen else Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (isTimerComplete) Color.White else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (questionNumber == totalQuestions) "Yes, Unrestrict App" else "Yes, Continue",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isTimerComplete) Color.White else Color.Gray
                        )
                    }
                }

                // No button (keep restriction) - always enabled
                OutlinedButton(
                    onClick = onNo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.Red.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No, Keep Restricted",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Red.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Motivational footer
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Blue.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color.Blue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTimerComplete)
                            "Great! You've taken time to reflect. Now choose mindfully."
                        else
                            "Use these $timeLeft seconds to honestly consider your motivation",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}