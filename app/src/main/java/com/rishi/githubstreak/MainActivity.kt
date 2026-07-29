package com.rishi.githubstreak

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.lifecycle.lifecycleScope
import com.rishi.githubstreak.data.AppData
import com.rishi.githubstreak.data.ProfileRepository
import com.rishi.githubstreak.ui.ProfilesScreen
import com.rishi.githubstreak.ui.WidgetsScreen
import com.rishi.githubstreak.ui.theme.GithubTheme
import com.rishi.githubstreak.ui.theme.LocalGithubPalette
import com.rishi.githubstreak.widget.WidgetInventory
import com.rishi.githubstreak.worker.RefreshStreakWorker
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = ProfileRepository.get(this)
        RefreshStreakWorker.enqueuePeriodic(this)

        // Redraw placed widgets on every launch. It costs no network and lets a widget whose
        // bitmap was rendered under the other system theme pick the right colours back up.
        lifecycleScope.launch { WidgetInventory.refreshAll(applicationContext) }

        setContent {
            GithubTheme {
                val data by repository.appData.collectAsState(initial = AppData())
                GithubStreakApp(data = data, repository = repository)
            }
        }
    }
}

private enum class HomeTab(val label: String) {
    PROFILES("Profiles"),
    WIDGETS("Widgets"),
}

@Composable
private fun GithubStreakApp(data: AppData, repository: ProfileRepository) {
    val palette = LocalGithubPalette.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = palette.canvasInset,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            AppHeader(profileCount = data.profiles.size)

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = palette.canvas,
                contentColor = palette.fg,
            ) {
                HomeTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        selectedContentColor = palette.fg,
                        unselectedContentColor = palette.fgMuted,
                        text = {
                            Text(
                                text = tab.label,
                                fontSize = 14.sp,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize().background(palette.canvasInset)) {
                when (HomeTab.entries[selectedTab]) {
                    HomeTab.PROFILES -> ProfilesScreen(data = data, repository = repository)
                    HomeTab.WIDGETS -> WidgetsScreen(data = data)
                }
            }
        }
    }
}

@Composable
private fun AppHeader(profileCount: Int) {
    val palette = LocalGithubPalette.current
    val context = LocalContext.current
    val refreshing by RefreshStreakWorker.isRefreshing(context)
        .collectAsState(initial = false)

    Column(modifier = Modifier.fillMaxWidth().background(palette.canvas)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_streak_mark),
                contentDescription = null,
                colorFilter = ColorFilter.tint(palette.fg),
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "GitHub Streak",
                    color = palette.fg,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = when {
                        refreshing -> "Refreshing…"
                        profileCount == 0 -> "No profiles tracked"
                        profileCount == 1 -> "1 profile tracked"
                        else -> "$profileCount profiles tracked"
                    },
                    color = if (refreshing) palette.accent else palette.fgMuted,
                    fontSize = 12.sp,
                )
            }
            if (profileCount > 0) {
                RefreshAllButton()
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.border))
    }
}

@Composable
private fun RefreshAllButton() {
    val palette = LocalGithubPalette.current
    val context = LocalContext.current

    androidx.compose.material3.IconButton(
        onClick = { RefreshStreakWorker.refreshNow(context) },
    ) {
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = "Refresh all profiles",
            tint = palette.fgMuted,
            modifier = Modifier.size(20.dp),
        )
    }
}
