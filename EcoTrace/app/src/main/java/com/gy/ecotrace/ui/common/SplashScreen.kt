package com.gy.ecotrace.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    isLoading: Boolean,
    onSplashDismiss: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            SplashOverlay()
        }
    }
}

@Composable
fun SplashOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Red)
            .zIndex(1f),
        contentAlignment = Alignment.Center
    ) {
        LoadingMessagesAnimated()
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LoadingMessagesAnimated(modifier: Modifier = Modifier) {
    val messages = listOf("Загружаем приложение…", "Уже почти всё готово…", "А теперь точно готово!")
    var currentIndex by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentIndex) {
        while (currentIndex < messages.lastIndex) {
            delay(750)
            currentIndex++
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = messages[currentIndex],
            transitionSpec = {
                slideInVertically(initialOffsetY = { it }) + fadeIn() with
                        slideOutVertically(targetOffsetY = { -it }) + fadeOut()

            },
            label = "LoadingText"
        ) { targetText ->
            Text(
                text = targetText,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}