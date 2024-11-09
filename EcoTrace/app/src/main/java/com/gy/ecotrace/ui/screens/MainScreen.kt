import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.gy.ecotrace.ui.common.BottomNavigationBar
import com.gy.ecotrace.ui.screens.EducationScreen
import com.gy.ecotrace.ui.screens.MoreScreen
import com.gy.ecotrace.ui.screens.NewsScreen
import com.gy.ecotrace.ui.screens.ProfileScreen
import com.gy.ecotrace.ui.screens.RatingScreen
import com.gy.ecotrace.utils.Screen

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen() {
    val navController = rememberAnimatedNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination?.route

    val bottomBarScreens = listOf(
        Screen.News.route,
        Screen.Education.route,
        Screen.Rating.route,
        Screen.More.route
    )
    val showBottomBar = currentDestination in bottomBarScreens

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(navController = navController, items = bottomBarScreens.map { Screen.fromRoute(it) })
            }
        }
    ) { innerPadding ->
        AnimatedNavHost(
            navController = navController,
            startDestination = Screen.News.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() }
        ) {
            composable(Screen.News.route) { NewsScreen() }
            composable(Screen.Education.route) { EducationScreen() }
            composable(Screen.Rating.route) { RatingScreen() }
            composable(Screen.More.route) { MoreScreen(navController) }

            composable("profile") { ProfileScreen() }
        }
    }
}


@Composable
fun ConnectionBanner(hasConnection: Boolean) {
    AnimatedVisibility(
        visible = !hasConnection,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Red)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Нет соединения с сервером",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


