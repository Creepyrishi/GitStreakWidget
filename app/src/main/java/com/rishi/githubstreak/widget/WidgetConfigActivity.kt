package com.rishi.githubstreak.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rishi.githubstreak.data.AppData
import com.rishi.githubstreak.data.Profile
import com.rishi.githubstreak.data.ProfileRepository
import com.rishi.githubstreak.ui.components.GhCard
import com.rishi.githubstreak.ui.components.InitialAvatar
import com.rishi.githubstreak.ui.theme.GithubTheme
import com.rishi.githubstreak.ui.theme.LocalGithubPalette
import kotlinx.coroutines.launch

/**
 * Shown when a widget is dropped on the home screen, and again from the app's Widgets tab.
 * Binds one widget instance to one profile.
 */
class WidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // The host treats a cancelled result as "remove the widget again", so set it up front.
        setResult(RESULT_CANCELED, resultIntent(appWidgetId))

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val kind = WidgetKind.fromReceiverClassName(
            AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)?.provider?.className,
        )
        if (kind == null) {
            finish()
            return
        }

        val repository = ProfileRepository.get(this)

        setContent {
            GithubTheme {
                WidgetConfigScreen(
                    kind = kind,
                    data = repository.appData.collectAsState(initial = AppData()).value,
                    appWidgetId = appWidgetId,
                    onCancel = ::finish,
                    onSaved = {
                        setResult(RESULT_OK, resultIntent(appWidgetId))
                        finish()
                    },
                )
            }
        }
    }

    private fun resultIntent(appWidgetId: Int) =
        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

    companion object {
        fun reconfigureIntent(context: Context, appWidgetId: Int): Intent =
            Intent(context, WidgetConfigActivity::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }
}

@Composable
private fun WidgetConfigScreen(
    kind: WidgetKind,
    data: AppData,
    appWidgetId: Int,
    onCancel: () -> Unit,
    onSaved: () -> Unit,
) {
    val palette = LocalGithubPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var loaded by remember { mutableStateOf(false) }
    var selectedProfileId by remember { mutableStateOf<String?>(null) }
    var theme by remember { mutableStateOf(WidgetThemeOption.SYSTEM) }
    var showLabels by remember { mutableStateOf(true) }

    // Pre-fill from the widget's existing state when reconfiguring.
    androidx.compose.runtime.LaunchedEffect(appWidgetId) {
        val existing = runCatching { WidgetInventory.load(context) }
            .getOrNull()
            ?.firstOrNull { it.appWidgetId == appWidgetId }
            ?.config
        if (existing != null) {
            selectedProfileId = existing.profileId
            theme = existing.theme
            showLabels = existing.showLabels
        }
        loaded = true
    }

    val effectiveProfileId = selectedProfileId ?: data.profiles.firstOrNull()?.id

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.canvasInset)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "${kind.title} widget",
            color = palette.fg,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(text = kind.summary, color = palette.fgMuted, fontSize = 13.sp)

        GhCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "Profile",
                    color = palette.fgMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )

                if (data.profiles.isEmpty()) {
                    Text(
                        text = "Add a GitHub username in the app first, then come back here.",
                        color = palette.fgMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                } else {
                    data.profiles.forEach { profile ->
                        ProfileRow(
                            profile = profile,
                            selected = profile.id == effectiveProfileId,
                            onSelect = { selectedProfileId = profile.id },
                        )
                    }
                }
            }
        }

        GhCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "Appearance",
                    color = palette.fgMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )

                WidgetThemeOption.entries.forEach { option ->
                    OptionRow(
                        label = option.label,
                        selected = option == theme,
                        onSelect = { theme = option },
                    )
                }

                if (kind == WidgetKind.CALENDAR) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Header and labels", color = palette.fg, fontSize = 14.sp)
                            Text(
                                text = "Turn off for a bare graph on small widgets.",
                                color = palette.fgMuted,
                                fontSize = 12.sp,
                            )
                        }
                        Switch(
                            checked = showLabels,
                            onCheckedChange = { showLabels = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = palette.successEmphasis,
                                uncheckedTrackColor = palette.canvasSubtle,
                                uncheckedBorderColor = palette.border,
                            ),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    val chosen = effectiveProfileId ?: return@Button
                    scope.launch {
                        WidgetInventory.apply(
                            context = context,
                            appWidgetId = appWidgetId,
                            kind = kind,
                            config = WidgetConfig(
                                profileId = chosen,
                                theme = theme,
                                showLabels = showLabels,
                            ),
                        )
                        onSaved()
                    }
                },
                enabled = loaded && effectiveProfileId != null,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.successEmphasis,
                    contentColor = Color.White,
                    disabledContainerColor = palette.canvasSubtle,
                    disabledContentColor = palette.fgSubtle,
                ),
                modifier = Modifier.weight(1f),
            ) {
                Text("Save widget")
            }

            TextButton(onClick = onCancel) {
                Text("Cancel", color = palette.fgMuted)
            }
        }
    }
}

@Composable
private fun ProfileRow(profile: Profile, selected: Boolean, onSelect: () -> Unit) {
    val palette = LocalGithubPalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = palette.accent,
                unselectedColor = palette.fgSubtle,
            ),
        )
        Spacer(modifier = Modifier.width(4.dp))
        InitialAvatar(username = profile.username, size = 26)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(profile.username, color = palette.fg, fontSize = 14.sp, maxLines = 1)
            Text(
                text = "${profile.streakDays} day streak",
                color = palette.fgMuted,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    val palette = LocalGithubPalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = palette.accent,
                unselectedColor = palette.fgSubtle,
            ),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = palette.fg, fontSize = 14.sp)
    }
}
