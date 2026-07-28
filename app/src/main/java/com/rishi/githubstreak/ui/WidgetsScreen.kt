package com.rishi.githubstreak.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.rishi.githubstreak.data.AppData
import com.rishi.githubstreak.ui.components.GhCard
import com.rishi.githubstreak.ui.components.InitialAvatar
import com.rishi.githubstreak.ui.components.StatusPill
import com.rishi.githubstreak.ui.theme.LocalGithubPalette
import com.rishi.githubstreak.widget.PlacedWidget
import com.rishi.githubstreak.widget.WidgetConfigActivity
import com.rishi.githubstreak.widget.WidgetInventory
import com.rishi.githubstreak.widget.WidgetKind

@Composable
fun WidgetsScreen(
    data: AppData,
    modifier: Modifier = Modifier,
) {
    val palette = LocalGithubPalette.current
    val context = LocalContext.current
    var reloadKey by remember { mutableIntStateOf(0) }
    var widgets by remember { mutableStateOf<List<PlacedWidget>?>(null) }

    val configureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { reloadKey += 1 }

    // Widgets can appear while we are in the background (pin dialog, launcher widget picker),
    // so re-read the list every time this screen comes back to the foreground.
    LifecycleResumeEffect(Unit) {
        reloadKey += 1
        onPauseOrDispose { }
    }

    LaunchedEffect(reloadKey, data.profiles) {
        widgets = WidgetInventory.load(context)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(palette.canvasInset),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { AddWidgetCard() }

        item {
            Text(
                text = "Placed widgets",
                color = palette.fgMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        val placed = widgets
        if (placed != null && placed.isEmpty()) {
            item {
                GhCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("Nothing on the home screen yet", color = palette.fg, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Add a widget above, or long-press your home screen and pick GitHub Streak.",
                            color = palette.fgMuted,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }

        items(placed.orEmpty(), key = { it.appWidgetId }) { widget ->
            PlacedWidgetCard(
                widget = widget,
                data = data,
                onConfigure = {
                    configureLauncher.launch(
                        WidgetConfigActivity.reconfigureIntent(context, widget.appWidgetId),
                    )
                },
            )
        }

        item {
            Text(
                text = "Each widget keeps its own profile and theme, so you can watch several " +
                    "accounts side by side.",
                color = palette.fgMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun AddWidgetCard() {
    val palette = LocalGithubPalette.current
    val context = LocalContext.current
    val canPin = remember { WidgetInventory.canPin(context) }

    GhCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Add a widget",
                color = palette.fg,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (canPin) {
                    "Pick a kind and your launcher will ask where to drop it."
                } else {
                    "This launcher cannot add widgets from inside apps. " +
                        "Long-press the home screen and pick GitHub Streak."
                },
                color = palette.fgMuted,
                fontSize = 12.sp,
            )

            WidgetKind.entries.forEach { kind ->
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(kind.title, color = palette.fg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(kind.summary, color = palette.fgMuted, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedButton(
                        onClick = { WidgetInventory.requestPin(context, kind) },
                        enabled = canPin,
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlacedWidgetCard(
    widget: PlacedWidget,
    data: AppData,
    onConfigure: () -> Unit,
) {
    val palette = LocalGithubPalette.current
    val profile = data.resolveForWidget(widget.config.profileId)
    val orphaned = widget.config.profileId != null && profile == null

    GhCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InitialAvatar(username = profile?.username ?: "?")
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = widget.kind.title,
                    color = palette.fg,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = when {
                        orphaned -> "Profile was removed — pick another"
                        profile == null -> "No profile yet"
                        widget.config.profileId == null -> "${profile.username} (first profile)"
                        else -> profile.username
                    },
                    color = if (orphaned) palette.danger else palette.fgMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }
            StatusPill(
                text = widget.config.theme.label,
                contentColor = palette.fgMuted,
                containerColor = palette.canvasSubtle,
            )
        }

        HorizontalRule()

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onConfigure) {
                Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Configure", color = palette.accent, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "#${widget.appWidgetId}",
                color = palette.fgSubtle,
                fontSize = 11.sp,
                modifier = Modifier.padding(end = 12.dp),
            )
        }
    }
}
