package net.imknown.android.forefrontinfo.ui.settings

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.ext.toast
import net.imknown.android.forefrontinfo.ui.common.State
import net.imknown.android.forefrontinfo.ui.settings.repository.SettingsRepository
import net.imknown.android.forefrontinfo.ui.theme.AppTheme

/**
 * Settings page: there is no official Compose Preference library yet (androidx.preference stopped at 1.2.1),
 * so the UI is hand-written from plain M3 components, mirroring how Now in Android builds its settings screen.
 * Storage untouched: the same SharedPreferences is read/written, so existing user preferences carry over as-is.
 * State hoisting: this function only reads preferences, subscribes to the version flow and wires callbacks;
 * all UI lives in [SettingsContent] (pure data, previewable).
 */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // ---- Current preference values (three stages): read SP once on entry, local state is the source of
    // truth afterwards, write through to SP on change. stringResource must be evaluated outside remember:
    val themeKey = stringResource(R.string.interface_themes_key)
    val themeDefaultValue = stringResource(R.string.interface_themes_follow_system_value)
    // No tombstone handling needed here: MyApplication.initTheme migrates a legacy stored
    // "power saver" value (its "1" tombstone is kept in strings.xml) back to follow system at startup
    var themeValue by remember {
        mutableStateOf(
            MyApplication.sharedPreferences.getString(themeKey, null)
                ?: themeDefaultValue
        )
    }

    val scrollBarKey = stringResource(R.string.interface_scroll_bar_key)
    val scrollBarDefaultValue = stringResource(R.string.interface_no_scroll_bar_value)
    var scrollBarValue by remember {
        mutableStateOf(
            MyApplication.sharedPreferences.getString(scrollBarKey, null)
                ?: scrollBarDefaultValue
        )
    }

    val allowNetworkKey = stringResource(R.string.function_allow_network_data_key)
    var allowNetwork by remember {
        mutableStateOf(MyApplication.sharedPreferences.getBoolean(allowNetworkKey, false))
    }

    val outdatedOrderKey =
        stringResource(R.string.function_outdated_target_order_by_package_name_first_key)
    var outdatedOrderFirst by remember {
        mutableStateOf(MyApplication.sharedPreferences.getBoolean(outdatedOrderKey, false))
    }

    // ---- Version info: mirrors the legacy Fragment ("subscribe + init once"; the VM guards re-entry) ----
    val versionState by viewModel.version.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.setBuiltInDataVersion(context.packageManager, context.packageName)
    }

    SettingsContent(
        themeValue = themeValue,
        scrollBarValue = scrollBarValue,
        allowNetwork = allowNetwork,
        outdatedOrderFirst = outdatedOrderFirst,
        versionState = versionState,
        onThemeSelect = { value ->
            themeValue = value
            MyApplication.sharedPreferences.edit { putString(themeKey, value) }
            MyApplication.setMyTheme(value) // writes the preference stream (single source of truth); AppTheme collects it and recomposes to switch light/dark, no Activity recreate
        },
        onScrollBarSelect = { value ->
            scrollBarValue = value
            MyApplication.sharedPreferences.edit { putString(scrollBarKey, value) }
            viewModel.emitScrollBarModeChangedSharedFlow(value) // list pages pick it up immediately
        },
        onAllowNetworkChange = { value ->
            allowNetwork = value
            MyApplication.sharedPreferences.edit { putBoolean(allowNetworkKey, value) }
        },
        onOutdatedOrderChange = { value ->
            outdatedOrderFirst = value
            MyApplication.sharedPreferences.edit { putBoolean(outdatedOrderKey, value) }
            // no broadcast needed: Home observes this preference key itself and reorders
        },
        onVersionClick = {
            viewModel.getVersionClickedMessage()?.let { context.toast(it) } // the 7-tap easter-egg logic lives in the VM, reused as-is
        },
        modifier = modifier,
    )
}

@Composable
private fun SettingsContent(
    themeValue: String, // current theme stored value
    scrollBarValue: String, // current scroll bar stored value
    allowNetwork: Boolean, // allow-network-data switch
    outdatedOrderFirst: Boolean, // outdated-order-by-package-name switch
    versionState: State<SettingsRepository.Version>, // version info (VM subscription result)
    onThemeSelect: (String) -> Unit, // all events go up (dumb component, keeps no state)
    onScrollBarSelect: (String) -> Unit,
    onAllowNetworkChange: (Boolean) -> Unit,
    onOutdatedOrderChange: (Boolean) -> Unit,
    onVersionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showScrollBarDialog by rememberSaveable { mutableStateOf(false) }

    val groupSpacing = dimensionResource(R.dimen.item_divider_space_vertical)
    val groupHorizontalPadding = dimensionResource(R.dimen.item_divider_space_horizontal)
    // Divider = page background color (surfaceContainer): reads as a groove on the surfaceBright
    // cards and adapts to light/dark automatically; height nudged up from 1dp
    val dividerColor = MaterialTheme.colorScheme.surfaceContainer
    val dividerThickness = 1.5.dp

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer), // page background = same as the first three list pages
        // horizontal 12dp is part of the content design (not insets compensation); top bar / bottom bar / system bars are handled by Scaffold innerPadding
        contentPadding = PaddingValues(
            start = groupHorizontalPadding,
            end = groupHorizontalPadding,
            bottom = groupSpacing,
        ),
    ) {
        item { SettingsCategoryHeader(R.string.interface_title) }
        item {
            SettingsGroup(
                modifier = Modifier.padding(bottom = groupSpacing)
            ) {
                SettingsChoiceRow(
                    title = R.string.interface_themes_title,
                    summary = R.string.interface_themes_summary,
                    onClick = { showThemeDialog = true },
                )
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                SettingsChoiceRow(
                    title = R.string.interface_scroll_bar_title,
                    summary = R.string.interface_scroll_bar_summary,
                    onClick = { showScrollBarDialog = true },
                )
            }
        }
        item { SettingsCategoryHeader(R.string.function_title) }
        item {
            SettingsGroup(
                modifier = Modifier.padding(bottom = groupSpacing)
            ) {
                SettingsSwitchRow(
                    title = R.string.function_allow_network_data_title,
                    summary = R.string.function_allow_network_data_summary,
                    checked = allowNetwork,
                    onCheckedChange = onAllowNetworkChange,
                )
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                SettingsSwitchRow(
                    title = R.string.function_outdated_target_order_by_package_name_first_title,
                    summary = R.string.function_outdated_target_order_by_package_name_first_summary,
                    checked = outdatedOrderFirst,
                    onCheckedChange = onOutdatedOrderChange,
                )
            }
        }
        item { SettingsCategoryHeader(R.string.about_title) }
        item {
            SettingsGroup {
                settingsLinks.forEachIndexed { index, link ->
                    if (index > 0) {
                        HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                    }
                    val uri = stringResource(link.uri) // resolved during composition: click callbacks are not a composable context
                    ListItem(
                        headlineContent = { Text(stringResource(link.title)) },
                        supportingContent = { Text(stringResource(link.summary)) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { openInExternal(context, uri) },
                    )
                }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                val version = (versionState as? State.Done<SettingsRepository.Version>)?.value
                ListItem(
                    headlineContent = { Text(stringResource(R.string.about_version_title)) },
                    supportingContent = {
                        version?.let { v ->
                            Text(
                                stringResource(
                                    v.id,
                                    v.versionName, v.versionCode, v.assetLldVersion,
                                    v.distributor, v.installer, v.firstInstallTime, v.lastUpdateTime,
                                )
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onVersionClick() },
                )
            }
        }
    }

    if (showThemeDialog) {
        // the arrays only matter while the dialog is open: declaring them inside the sole consumer skips the resource reads entirely when closed
        val themeLabels = stringArrayResource(R.array.themeKeys).toPersistentList()
        val themeValues = stringArrayResource(R.array.themeValues).toPersistentList()
        SettingsChoiceDialog(
            title = stringResource(R.string.interface_themes_title),
            labels = themeLabels,
            values = themeValues,
            selected = themeValue,
            onSelect = { value ->
                onThemeSelect(value)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false },
        )
    }
    if (showScrollBarDialog) {
        val scrollBarLabels = stringArrayResource(R.array.scrollBarKeys).toPersistentList()
        val scrollBarValues = stringArrayResource(R.array.scrollBarValues).toPersistentList()
        SettingsChoiceDialog(
            title = stringResource(R.string.interface_scroll_bar_title),
            labels = scrollBarLabels,
            values = scrollBarValues,
            selected = scrollBarValue,
            onSelect = { value ->
                onScrollBarSelect(value)
                showScrollBarDialog = false
            },
            onDismiss = { showScrollBarDialog = false },
        )
    }
}

/**
 * Grouped rounded container: surfaceBright background (= the MyModelCard card color of the first
 * three pages) + medium corner shape (= the default Card shape), matching the list pages.
 */
@Composable
private fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceBright, MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium)
            .fillMaxWidth(),
        content = content,
    )
}

@Composable
private fun SettingsChoiceRow(
    @StringRes title: Int,
    @StringRes summary: Int, // static text (same as the legacy XML app:summary)
    onClick: () -> Unit,
    modifier: Modifier = Modifier, // conventional parameter: caller constraints
) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        supportingContent = { Text(stringResource(summary)) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent), // follows the row card background
        modifier = modifier.clickable { onClick() },
    )
}

@Composable
private fun SettingsChoiceDialog(
    title: String, // resolved to a string by the caller
    labels: PersistentList<String>, // display labels (translatable); PersistentList is stable, Array is not
    values: PersistentList<String>, // stored values (translatable=false, never change)
    selected: String, // currently selected value (drives the RadioButton states)
    onSelect: (String) -> Unit, // selection event goes up
    onDismiss: () -> Unit, // dismiss event goes up
) {
    AlertDialog(
        onDismissRequest = onDismiss, // tap outside or system back = dismiss
        title = { Text(title) }, // slot parameter: the value is @Composable, content is up to you
        text = {
            Column {
                labels.zip(values).forEach { [label, value] -> // zip the arrays into (label, value) pairs; Kotlin 2.4 positional destructuring uses brackets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) } // whole row clickable (not just the radio)
                            .padding(vertical = 12.dp), // vertical breathing room for tap targets
                        verticalAlignment = Alignment.CenterVertically, // radio and text vertically centered
                    ) {
                        RadioButton(selected = value == selected, onClick = null) // display only, clicks handled by the row
                        Text(text = label, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = { // the slot requires non-null: a "cancel" fallback (selection applies immediately, no confirm)
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

@Composable
private fun SettingsSwitchRow(
    @StringRes title: Int,
    @StringRes summary: Int,
    checked: Boolean, // switch state passed in (dumb component, keeps none)
    onCheckedChange: (Boolean) -> Unit, // switch event goes up
    modifier: Modifier = Modifier, // conventional parameter: caller constraints
) {
    ListItem(
        headlineContent = { Text(stringResource(title)) }, // headline (= Preference title)
        supportingContent = { Text(stringResource(summary)) }, // supporting (= Preference summary)
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) }, // trailing
        colors = ListItemDefaults.colors(containerColor = Color.Transparent), // follows the row card background
        modifier = modifier.clickable { onCheckedChange(!checked) }, // the whole row is also clickable (UX detail)
    )
}

private data class SettingsLink(
    @StringRes val title: Int,
    @StringRes val summary: Int,
    @StringRes val uri: Int,
)

private val settingsLinks = persistentListOf(
    SettingsLink(R.string.about_shop_title, R.string.about_shop_summary, R.string.about_shop_uri),
    SettingsLink(R.string.about_source_title, R.string.about_source_summary, R.string.about_source_uri),
    SettingsLink(R.string.about_privacy_policy_title, R.string.about_privacy_policy_summary, R.string.about_privacy_policy_uri),
    SettingsLink(R.string.about_licenses_title, R.string.about_licenses_summary, R.string.about_licenses_uri),
    SettingsLink(R.string.translation_language_and_translator, R.string.translator_more_info, R.string.translator_website),
)

private fun openInExternal(context: Context, uri: String) {
    val intent = Intent(Intent.ACTION_VIEW, uri.toUri()).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        context.toast(R.string.no_browser_found)
    }
}

/** Category header, mirroring PreferenceCategory. */
@Composable
private fun SettingsCategoryHeader(@StringRes titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
    )
}

// The screen function takes a ViewModel and cannot be previewed directly; the preview targets the data-only SettingsContent instead
@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun SettingsContentPreview() {
    AppTheme {
        SettingsContent(
            themeValue = "-1",
            scrollBarValue = "1",
            allowNetwork = false,
            outdatedOrderFirst = true,
            versionState = State.Done(
                SettingsRepository.Version(
                    id = R.string.about_version_summary,
                    versionName = "1.18.8",
                    versionCode = 73,
                    assetLldVersion = "2026.09.00",
                    distributor = "Google Play",
                    installer = "com.android.vending",
                    firstInstallTime = "2026-01-01 00:00",
                    lastUpdateTime = "2026-09-19 00:00",
                )
            ),
            onThemeSelect = {},
            onScrollBarSelect = {},
            onAllowNetworkChange = {},
            onOutdatedOrderChange = {},
            onVersionClick = {},
        )
    }
}
