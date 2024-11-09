package com.gy.ecotrace.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.gy.ecotrace.BuildConfig
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.network.dto.RatingItem
import com.gy.ecotrace.sections.rating.RatingViewModel
import com.gy.ecotrace.ui.more.profile.ProfileActivity

@Composable
fun RatingScreen(viewModel: RatingViewModel = hiltViewModel()) {
    val users by viewModel.users.collectAsState()
    val loading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    Box(Modifier.fillMaxSize()) {
        when {
            loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            users.isEmpty() -> Text("Рейтинг пуст", Modifier.align(Alignment.Center))
            else -> LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
                itemsIndexed(users) { index, user ->
                    RatingRow(index + 1, user) {
                        Globals.getInstance().setString("CurrentlyWatching", user.id.toString())
                        context.startActivity(Intent(context, ProfileActivity::class.java))
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingRow(place: Int, user: RatingItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$place", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.width(36.dp))
        AsyncImage(
            model = user.avatarKey?.let { "${BuildConfig.SERVER_API_URI}uploads/users/$it" },
            contentDescription = null,
            placeholder = painterResource(R.drawable.baseline_person_24),
            error = painterResource(R.drawable.baseline_person_24),
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(44.dp).clip(CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Text(user.username, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text("${user.experience}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}
