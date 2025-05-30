package net.imknown.android.forefrontinfo.ui.settings

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.extension.isChinaMainlandTimezone
import net.imknown.android.forefrontinfo.ui.base.ext.toast
import net.imknown.android.forefrontinfo.ui.base.list.verticalScrollbar

@Composable
fun SettingsScreen(
    listState: LazyListState = rememberLazyListState(),
    versionSummary: String = "",
    onThemeChanged: (String?) -> Unit = {},
    onScrollBarModeChanged: (String?) -> Unit = {},
    onOutdatedOrderChanged: () -> Unit = {},
    onVersionClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val sharedPreferences = remember(context) {
        PreferenceManager.getDefaultSharedPreferences(context)
    }
    val scrollBarKey = stringResource(id = R.string.interface_scroll_bar_key)
    val scrollBarDefaultValue = stringResource(id = R.string.interface_no_scroll_bar_value)
    val scrollBarModeSetting = remember(sharedPreferences, SettingsViewModel.scrollBarModeChanged) {
        sharedPreferences.getString(scrollBarKey, scrollBarDefaultValue)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .verticalScrollbar(listState, scrollBarModeSetting)
    ) {
        // Interface Category
        item {
            PreferenceGroupHeader(title = stringResource(id = R.string.interface_title))
        }
        item {
            ListPreferenceComposable(
                title = stringResource(id = R.string.interface_themes_title),
                summary = stringResource(id = R.string.interface_themes_summary),
                key = stringResource(id = R.string.interface_themes_key),
                defaultValue = stringResource(id = R.string.interface_themes_follow_system_value),
                entriesResId = R.array.themeKeys,
                entryValuesResId = R.array.themeValues,
                onValueChange = { onThemeChanged(it) }
            )
        }
        item {
            ListPreferenceComposable(
                title = stringResource(id = R.string.interface_scroll_bar_title),
                summary = stringResource(id = R.string.interface_scroll_bar_summary),
                key = stringResource(id = R.string.interface_scroll_bar_key),
                defaultValue = stringResource(id = R.string.interface_no_scroll_bar_value),
                entriesResId = R.array.scrollBarKeys,
                entryValuesResId = R.array.scrollBarValues,
                onValueChange = { onScrollBarModeChanged(it) }
            )
        }

        // Function Category
        item {
            PreferenceGroupHeader(title = stringResource(id = R.string.function_title))
        }
        item {
            SwitchPreferenceComposable(
                title = stringResource(id = R.string.function_allow_network_data_title),
                summary = stringResource(id = R.string.function_allow_network_data_summary),
                key = stringResource(id = R.string.function_allow_network_data_key),
                defaultValue = false
            )
        }
        item {
            SwitchPreferenceComposable(
                title = stringResource(id = R.string.function_outdated_target_order_by_package_name_first_title),
                summary = stringResource(id = R.string.function_outdated_target_order_by_package_name_first_summary),
                key = stringResource(id = R.string.function_outdated_target_order_by_package_name_first_key),
                defaultValue = false,
                onCheckedChange = { onOutdatedOrderChanged() }
            )
        }

        // About Category
        item {
            PreferenceGroupHeader(title = stringResource(id = R.string.about_title))
        }
        item {
            ClickablePreferenceComposable(
                title = stringResource(id = R.string.about_shop_title),
                summary = stringResource(id = R.string.about_shop_summary),
                key = stringResource(id = R.string.about_shop_key),
                onPreferenceClick = {
                    val uriResId = if (isChinaMainlandTimezone()) {
                        R.string.about_shop_china_mainland_uri
                    } else {
                        R.string.about_shop_uri
                    }
                    openInExternal(context, uriResId)
                }
            )
        }
        item {
            ClickablePreferenceComposable(
                title = stringResource(id = R.string.about_source_title),
                summary = stringResource(id = R.string.about_source_summary),
                key = stringResource(id = R.string.about_source_key),
                onPreferenceClick = { openInExternal(context, R.string.about_source_uri) }
            )
        }
        item {
            ClickablePreferenceComposable(
                title = stringResource(id = R.string.about_privacy_policy_title),
                summary = stringResource(id = R.string.about_privacy_policy_summary),
                key = stringResource(id = R.string.about_privacy_policy_key),
                onPreferenceClick = { openInExternal(context, R.string.about_privacy_policy_uri) }
            )
        }
        item {
            ClickablePreferenceComposable(
                title = stringResource(id = R.string.about_licenses_title),
                summary = stringResource(id = R.string.about_licenses_summary),
                key = stringResource(id = R.string.about_licenses_key),
                onPreferenceClick = { openInExternal(context, R.string.about_licenses_uri) }
            )
        }
        item {
            ClickablePreferenceComposable(
                title = stringResource(id = R.string.translation_language_and_translator),
                summary = stringResource(id = R.string.translator_more_info),
                key = stringResource(id = R.string.about_translator_more_info_key),
                onPreferenceClick = { openInExternal(context, R.string.translator_website) }
            )
        }
        item {
            ClickablePreferenceComposable(
                title = stringResource(id = R.string.about_version_title),
                summary = versionSummary.ifEmpty { stringResource(id = R.string.about_version_summary) },
                key = stringResource(id = R.string.about_version_key),
                onPreferenceClick = onVersionClick
            )
        }
    }
}

private fun openInExternal(context: Context, @StringRes uriResId: Int) {
    val uri = context.getString(uriResId).toUri()
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val component = intent.resolveActivity(context.packageManager)
    if (component != null) {
        context.startActivity(intent)
    } else {
        context.toast(R.string.no_browser_found)
    }
}

@Composable
fun PreferenceGroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun ListPreferenceComposable(
    title: String,
    summary: String,
    key: String,
    defaultValue: String,
    entriesResId: Int,
    entryValuesResId: Int,
    onValueChange: (String?) -> Unit = {},
    onPreferenceClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val sharedPreferences = remember(context) {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    val entries = stringArrayResource(id = entriesResId)
    val entryValues = stringArrayResource(id = entryValuesResId)
    
    var currentValue by remember { 
        mutableStateOf(sharedPreferences.getString(key, defaultValue) ?: defaultValue)
    }

    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = {
                expanded = true
                onPreferenceClick()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                val index = entryValues.indexOf(currentValue)
                val displaySummary = if (index != -1) entries[index] else summary
                Text(text = displaySummary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(x = 16.dp, y = 0.dp)
        ) {
            entryValues.forEachIndexed { index, value ->
                val isSelected = (value == currentValue)
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = isSelected, onClick = null)
                            Text(
                                text = entries[index],
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    },
                    onClick = {
                        currentValue = value
                        sharedPreferences.edit { putString(key, value) }
                        onValueChange(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SwitchPreferenceComposable(
    title: String,
    summary: String? = null,
    key: String,
    defaultValue: Boolean,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val sharedPreferences = remember(context) {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    var isChecked by rememberSaveable(key) {
        mutableStateOf(sharedPreferences.getBoolean(key, defaultValue))
    }

    Surface(
        onClick = {
            isChecked = !isChecked
            sharedPreferences.edit { putBoolean(key, isChecked) }
            onCheckedChange(isChecked)
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                if (summary != null) {
                    Text(text = summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Switch(
                checked = isChecked,
                onCheckedChange = {
                    isChecked = it
                    sharedPreferences.edit { putBoolean(key, it) }
                    onCheckedChange(it)
                },
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
fun ClickablePreferenceComposable(
    title: String,
    summary: String? = null,
    key: String,
    onPreferenceClick: () -> Unit = {}
) {
    Surface(
        onClick = onPreferenceClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                Text(text = summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen()
}
