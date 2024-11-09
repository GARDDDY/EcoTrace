package com.gy.ecotrace.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import com.gy.ecotrace.sections.news.NewsItem
import com.gy.ecotrace.sections.news.NewsViewModel
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.delay

@Composable
fun NewsScreen(viewModel: NewsViewModel = hiltViewModel()) {
    val news by viewModel.news.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                LoadingTextWithDots(modifier = Modifier.align(Alignment.Center))
            }
            error != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Ошибка: $error")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.retry() }) {
                        Text("Попробовать снова")
                    }
                }
            }
            news.isEmpty() -> {
                Text("Новостей нет", modifier = Modifier.align(Alignment.Center))
            }
            else -> {
                LazyColumn {
                    items(news) { item ->
                        NewsItemView(item)
                    }
                }
            }
        }
    }
}

@Composable
fun NewsItemView(item: NewsItem) {
    Column (
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        SubcomposeAsyncImage (
            model = item.postImage,
            contentDescription = item.postTitle,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shimmer(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .size(200.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ) {}
                }
            },
            error = {
                Box(
                    modifier = Modifier
                        .size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Ошибка")
                }
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = item.postTitle,
                fontSize = 25.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}

@Composable
fun LoadingTextWithDots(
    baseText: String = "Ищем новости",
    maxDots: Int = 3,
    delayMillis: Long = 500,
    modifier: Modifier = Modifier
) {
    var dotCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(delayMillis)
            dotCount = (dotCount + 1) % (maxDots + 1)
        }
    }

    Text(
        text = baseText + ".".repeat(dotCount),
        modifier = modifier,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Black
    )
}

