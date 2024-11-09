package com.gy.ecotrace.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object News : Screen("news", "Новости", Icons.Default.Info)
    data object Education : Screen("education", "Образование", Icons.Default.Face)
    data object Rating : Screen("rating", "Рейтинг", Icons.Default.Star)
    data object More : Screen("more", "Еще", Icons.Default.MoreVert)

    companion object {
        fun fromRoute(route: String): Screen =
            when (route) {
                News.route -> News
                Education.route -> Education
                Rating.route -> Rating
                More.route -> More
                else -> throw IllegalArgumentException("Unknown route")
            }
    }
}
