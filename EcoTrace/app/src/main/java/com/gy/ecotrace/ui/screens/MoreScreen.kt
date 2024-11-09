package com.gy.ecotrace.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.gy.ecotrace.sections.news.NewsItem

@Composable
fun MoreScreen(navController: NavController) {
    val items = listOf(
        ButtonItem(
            label = "Настройки",
            description = "Изменить параметры приложения",
            icon = Icons.Rounded.Settings,
            onClick = { }
        ),
        ButtonItem(
            label = "Профиль",
            description = "Информация о пользователе",
            icon = Icons.Rounded.Person,
            onClick = { navController.navigate("profile") }
        ),
        ButtonItem(
            label = "О приложении",
            description = "Версия и разработчики",
            icon = Icons.Rounded.Info,
            onClick = { }
        )
    )

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items) { item ->
            ButtonLink(item = item)
            Divider()
        }
    }
}



data class ButtonItem(
    val label: String,
    val description: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun ButtonLink(item: ButtonItem, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            modifier = Modifier
                .size(40.dp)
                .padding(end = 16.dp)
        )

        Column {
            Text(
                text = item.label,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}