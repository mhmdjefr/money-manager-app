package com.mhmdjefr.moneymanager.ui.onboarding

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhmdjefr.moneymanager.ui.theme.SoftBlue

private const val PREFS_NAME = "money_prefs"

fun isOnboardingSeen(context: Context, key: String): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean("onboarding_$key", false)
}

fun markOnboardingSeen(context: Context, key: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean("onboarding_$key", true).apply()
}

@Composable
fun OnboardingTooltip(
    visible: Boolean,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E293B))
                .clickable { onDismiss() }
                .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.widthIn(max = 220.dp)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun TutorialReplayButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(50))
            .background(SoftBlue.copy(alpha = 0.12f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "?",
            color = SoftBlue,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

class OnboardingState(
    initialVisible: Boolean,
    private val context: Context,
    private val key: String
) {
    var isVisible by mutableStateOf(initialVisible)
        private set

    fun show() {
        isVisible = true
    }

    fun dismiss() {
        isVisible = false
        markOnboardingSeen(context, key)
    }
}

@Composable
fun rememberOnboardingState(key: String): OnboardingState {
    val context = LocalContext.current
    return remember(key) {
        OnboardingState(
            initialVisible = !isOnboardingSeen(context, key),
            context = context,
            key = key
        )
    }
}
