package com.rishi.githubstreak.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rishi.githubstreak.data.AddProfileResult
import com.rishi.githubstreak.data.AppData
import com.rishi.githubstreak.data.GithubContributionClient
import com.rishi.githubstreak.data.Profile
import com.rishi.githubstreak.data.ProfileRepository
import com.rishi.githubstreak.ui.components.ContributionGraph
import com.rishi.githubstreak.ui.components.ContributionLegend
import com.rishi.githubstreak.ui.components.GhCard
import com.rishi.githubstreak.ui.components.InitialAvatar
import com.rishi.githubstreak.ui.components.StatTile
import com.rishi.githubstreak.ui.components.StatusPill
import com.rishi.githubstreak.ui.components.rememberStatusVisuals
import com.rishi.githubstreak.ui.theme.LocalGithubPalette
import com.rishi.githubstreak.worker.RefreshStreakWorker
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import kotlinx.coroutines.launch

@Composable
fun ProfilesScreen(
    data: AppData,
    repository: ProfileRepository,
    modifier: Modifier = Modifier,
) {
    val palette = LocalGithubPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<Profile?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(palette.canvasInset),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AddProfileCard(
                onAdd = { username ->
                    val result = repository.addProfile(username)
                    if (result is AddProfileResult.Added) {
                        RefreshStreakWorker.refreshNow(context, result.profile.id)
                    }
                    result
                },
            )
        }

        if (data.loaded && data.profiles.isEmpty()) {
            item { EmptyProfilesCard() }
        }

        items(data.profiles, key = { it.id }) { profile ->
            ProfileCard(
                profile = profile,
                canMoveUp = data.profiles.first().id != profile.id,
                canMoveDown = data.profiles.last().id != profile.id,
                onRefresh = { RefreshStreakWorker.refreshNow(context, profile.id) },
                onMove = { offset -> scope.launch { repository.moveProfile(profile.id, offset) } },
                onDelete = { pendingDelete = profile },
            )
        }

        item {
            Text(
                text = "A day counts when GitHub's public contribution graph shows any activity. " +
                    "The streak resets to 0 once today and yesterday are both empty.",
                color = palette.fgMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
        }
    }

    pendingDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = palette.canvas,
            title = { Text("Remove ${profile.username}?", color = palette.fg) },
            text = {
                Text(
                    "Widgets showing this profile will ask you to pick a new one.",
                    color = palette.fgMuted,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch { repository.removeProfile(profile.id) }
                        pendingDelete = null
                    },
                ) {
                    Text("Remove", color = palette.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel", color = palette.fgMuted)
                }
            },
        )
    }
}

@Composable
private fun AddProfileCard(onAdd: suspend (String) -> AddProfileResult) {
    val palette = LocalGithubPalette.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var input by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val normalized = GithubContributionClient.normalizeUsername(input)
    val canSubmit = normalized.isNotBlank() && GithubContributionClient.isValidUsername(normalized)

    fun submit() {
        if (!canSubmit) return
        keyboard?.hide()
        scope.launch {
            when (val result = onAdd(normalized)) {
                is AddProfileResult.Added -> {
                    input = ""
                    isError = false
                    message = "Added ${result.profile.username}. Fetching contributions…"
                }
                is AddProfileResult.Duplicate -> {
                    isError = true
                    message = "${result.profile.username} is already being tracked."
                }
                AddProfileResult.InvalidUsername -> {
                    isError = true
                    message = "That is not a valid GitHub username."
                }
            }
        }
    }

    GhCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Track a GitHub profile",
                color = palette.fg,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        message = null
                    },
                    placeholder = { Text("username", color = palette.fgSubtle) },
                    singleLine = true,
                    shape = RoundedCornerShape(6.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = palette.fg,
                        unfocusedTextColor = palette.fg,
                        focusedContainerColor = palette.canvasSubtle,
                        unfocusedContainerColor = palette.canvasSubtle,
                        focusedBorderColor = palette.accent,
                        unfocusedBorderColor = palette.border,
                        cursorColor = palette.accent,
                    ),
                    modifier = Modifier.weight(1f),
                )

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = ::submit,
                    enabled = canSubmit,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.successEmphasis,
                        contentColor = androidx.compose.ui.graphics.Color.White,
                        disabledContainerColor = palette.canvasSubtle,
                        disabledContentColor = palette.fgSubtle,
                    ),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }

            message?.let { text ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = text,
                    color = if (isError) palette.danger else palette.fgMuted,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun EmptyProfilesCard() {
    val palette = LocalGithubPalette.current

    GhCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("No profiles yet", color = palette.fg, fontWeight = FontWeight.SemiBold)
            Text(
                text = "Add a username above, then place a widget for it.",
                color = palette.fgMuted,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun ProfileCard(
    profile: Profile,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onRefresh: () -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    val palette = LocalGithubPalette.current
    val status = rememberStatusVisuals(profile)

    GhCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InitialAvatar(username = profile.username)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.username,
                    color = palette.fg,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = status.message,
                    color = palette.fgMuted,
                    fontSize = 12.sp,
                    maxLines = 2,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            StatusPill(
                text = status.label,
                contentColor = status.foreground,
                containerColor = status.background,
            )
        }

        HorizontalRule()

        Column(modifier = Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                StatTile(
                    label = "Current streak",
                    value = profile.streakDays.toString(),
                    valueColor = if (profile.streakDays > 0) palette.success else palette.fgMuted,
                )
                StatTile(
                    label = "Longest",
                    value = profile.longestStreak.toString(),
                    valueColor = palette.fg,
                )
                StatTile(
                    label = "Contributions",
                    value = NumberFormat.getIntegerInstance().format(profile.totalContributions),
                    valueColor = palette.fg,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (profile.calendar.isEmpty) {
                Text(
                    text = "The contribution graph appears after the first refresh.",
                    color = palette.fgMuted,
                    fontSize = 12.sp,
                )
            } else {
                ContributionGraph(
                    calendar = profile.calendar,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = lastUpdatedText(profile.lastUpdatedMillis),
                        color = palette.fgSubtle,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f),
                    )
                    ContributionLegend()
                }
            }
        }

        HorizontalRule()

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onRefresh) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Refresh", color = palette.accent, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) {
                Icon(
                    Icons.Filled.KeyboardArrowUp,
                    contentDescription = "Move up",
                    tint = if (canMoveUp) palette.fgMuted else palette.fgSubtle.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = { onMove(1) }, enabled = canMoveDown) {
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Move down",
                    tint = if (canMoveDown) palette.fgMuted else palette.fgSubtle.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Remove profile",
                    tint = palette.danger,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
internal fun HorizontalRule() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(LocalGithubPalette.current.borderMuted),
    )
}

internal fun lastUpdatedText(millis: Long): String {
    if (millis <= 0L) return "Never refreshed"
    val formatted = DateFormat
        .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(millis))
    return "Updated $formatted"
}
