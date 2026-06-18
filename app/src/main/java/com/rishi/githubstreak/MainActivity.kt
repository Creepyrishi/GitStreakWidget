package com.rishi.githubstreak

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import com.rishi.githubstreak.data.AppState
import com.rishi.githubstreak.data.GithubContributionClient
import com.rishi.githubstreak.data.SettingsRepository
import com.rishi.githubstreak.data.StreakStatus
import com.rishi.githubstreak.widget.GithubStreakWidget
import com.rishi.githubstreak.worker.RefreshStreakWorker
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = SettingsRepository.get(this)
        RefreshStreakWorker.enqueuePeriodic(this)

        setContent {
            GithubStreakTheme {
                GithubStreakScreen(repository = repository)
            }
        }
    }
}

@Composable
private fun GithubStreakTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF2F6F3E),
            secondary = Color(0xFFE58A2F),
            background = Color(0xFFFFF9EA),
            surface = Color(0xFFFFF9EA),
            surfaceVariant = Color(0xFFFFF0C2),
        ),
        content = content,
    )
}

@Composable
private fun GithubStreakScreen(repository: SettingsRepository) {
    val state by repository.appState.collectAsState(initial = AppState())
    val appContext = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var usernameInput by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state.username) {
        usernameInput = state.username
    }

    val normalizedUsername = GithubContributionClient.normalizeUsername(usernameInput)
    val usernameIsValid = normalizedUsername.isBlank() || GithubContributionClient.isValidUsername(normalizedUsername)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = "GitHub Streak",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.primary,
            )

            Text(
                text = "Public activity streak for any GitHub username. No token, no login.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF4E4A3F),
            )

            StreakCard(state = state)

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("GitHub username") },
                        singleLine = true,
                        isError = normalizedUsername.isNotBlank() && !usernameIsValid,
                        supportingText = {
                            Text(
                                text = if (usernameIsValid) {
                                    "Example: torvalds or octocat"
                                } else {
                                    "Use a valid GitHub username."
                                },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            enabled = normalizedUsername.isNotBlank() && usernameIsValid,
                            onClick = {
                                scope.launch {
                                    repository.saveUsername(normalizedUsername)
                                    GithubStreakWidget().updateAll(appContext)
                                    RefreshStreakWorker.enqueuePeriodic(appContext)
                                    RefreshStreakWorker.refreshNow(appContext)
                                }
                            },
                        ) {
                            Text("Save")
                        }

                        OutlinedButton(
                            enabled = state.username.isNotBlank(),
                            onClick = { RefreshStreakWorker.refreshNow(appContext) },
                        ) {
                            Text("Refresh")
                        }

                        TextButton(
                            enabled = state.username.isNotBlank(),
                            onClick = {
                                scope.launch {
                                    repository.saveUsername("")
                                    GithubStreakWidget().updateAll(appContext)
                                }
                            },
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }

            Text(
                text = "Rule: if there is no visible public activity today and yesterday, the streak shows 0.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B6251),
            )
        }
    }
}

@Composable
private fun StreakCard(state: AppState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0C2)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = mascotFor(state),
                fontSize = 42.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = if (state.username.isBlank()) "Choose a username" else "@${state.username}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF3A352C),
            )
            Text(
                text = "${state.streakDays} ${if (state.streakDays == 1) "active day" else "active days"}",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = statusMessage(state),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5A5246),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Last refresh: ${lastUpdatedText(state.lastUpdatedMillis)}",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF776E5E),
            )
        }
    }
}

private fun mascotFor(state: AppState): String {
    val secondFrame = state.lastUpdatedMillis > 0L && (state.lastUpdatedMillis / 1000L) % 2L == 0L
    return when (state.status) {
        StreakStatus.NEEDS_USERNAME -> if (secondFrame) "(?o?)" else "(?_?)"
        StreakStatus.PENDING -> if (secondFrame) "(._.)" else "(. .)"
        StreakStatus.ACTIVE_TODAY -> if (secondFrame) "(^o^)" else "(^_^)"
        StreakStatus.ACTIVE_YESTERDAY -> if (secondFrame) "(o_o;)" else "(o_o)"
        StreakStatus.RESET -> if (secondFrame) "(-_-)" else "(z_z)"
        StreakStatus.ERROR -> if (secondFrame) "(x_x)" else "(!)"
    }
}

private fun statusMessage(state: AppState): String = when (state.status) {
    StreakStatus.NEEDS_USERNAME -> "Add a username to start."
    StreakStatus.PENDING -> "Saved. Refreshing public GitHub activity soon."
    StreakStatus.ACTIVE_TODAY -> "Active today. The streak is safe."
    StreakStatus.ACTIVE_YESTERDAY -> "No public activity today yet. Yesterday still keeps it alive."
    StreakStatus.RESET -> "Two inactive days detected, so the streak is 0."
    StreakStatus.ERROR -> "Refresh failed: ${state.errorMessage ?: "unknown error"}"
}

private fun lastUpdatedText(millis: Long): String {
    if (millis <= 0L) return "never"
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))
}
