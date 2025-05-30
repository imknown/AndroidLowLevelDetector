package net.imknown.android.forefrontinfo.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch // Added Switch import
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue // Added for state
import androidx.compose.runtime.mutableStateOf // Added for state
import androidx.compose.runtime.remember // Added for state
import androidx.compose.runtime.setValue // Added for state
import androidx.compose.ui.Alignment // Added for Row alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview // Added for Preview
import androidx.compose.ui.unit.dp
import net.imknown.android.forefrontinfo.R

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            // .padding(16.dp) // Padding will be applied per item/group for more control
    ) {
        // Interface Category
        PreferenceGroupHeader(title = stringResource(id = R.string.interface_title))
        ListPreferenceComposable(
            title = stringResource(id = R.string.interface_themes_title),
            summary = stringResource(id = R.string.interface_themes_summary),
            key = stringResource(id = R.string.interface_themes_key),
            defaultValue = stringResource(id = R.string.interface_themes_follow_system_value),
            entriesResId = R.array.themeKeys,
            entryValuesResId = R.array.themeValues
        )
        ListPreferenceComposable(
            title = stringResource(id = R.string.interface_scroll_bar_title),
            summary = stringResource(id = R.string.interface_scroll_bar_summary),
            key = stringResource(id = R.string.interface_scroll_bar_key),
            defaultValue = stringResource(id = R.string.interface_no_scroll_bar_value),
            entriesResId = R.array.scrollBarKeys,
            entryValuesResId = R.array.scrollBarValues
        )

        // Function Category
        PreferenceGroupHeader(title = stringResource(id = R.string.function_title))
        SwitchPreferenceComposable(
            title = stringResource(id = R.string.function_allow_network_data_title),
            summary = stringResource(id = R.string.function_allow_network_data_summary),
            key = stringResource(id = R.string.function_allow_network_data_key),
            defaultValue = false // Assuming default from typical SwitchPreference behavior
        )
        SwitchPreferenceComposable(
            title = stringResource(id = R.string.function_outdated_target_order_by_package_name_first_title),
            summary = stringResource(id = R.string.function_outdated_target_order_by_package_name_first_summary),
            key = stringResource(id = R.string.function_outdated_target_order_by_package_name_first_key),
            defaultValue = false // Assuming default
        )

        // About Category
        PreferenceGroupHeader(title = stringResource(id = R.string.about_title))
        ClickablePreferenceComposable(
            title = stringResource(id = R.string.about_shop_title),
            summary = stringResource(id = R.string.about_shop_summary),
            key = stringResource(id = R.string.about_shop_key)
        )
        ClickablePreferenceComposable(
            title = stringResource(id = R.string.about_source_title),
            summary = stringResource(id = R.string.about_source_summary),
            key = stringResource(id = R.string.about_source_key)
        )
        ClickablePreferenceComposable(
            title = stringResource(id = R.string.about_privacy_policy_title),
            summary = stringResource(id = R.string.about_privacy_policy_summary),
            key = stringResource(id = R.string.about_privacy_policy_key)
        )
        ClickablePreferenceComposable(
            title = stringResource(id = R.string.about_licenses_title),
            summary = stringResource(id = R.string.about_licenses_summary),
            key = stringResource(id = R.string.about_licenses_key)
        )
        ClickablePreferenceComposable(
            title = stringResource(id = R.string.translation_language_and_translator),
            summary = stringResource(id = R.string.translator_more_info),
            key = stringResource(id = R.string.about_translator_more_info_key)
        )
        ClickablePreferenceComposable(
            title = stringResource(id = R.string.about_version_title),
            // tools:summary was used in XML, using the actual string for Compose
            summary = stringResource(id = R.string.about_version_summary), 
            key = stringResource(id = R.string.about_version_key)
        )
    }
}

@Composable
fun PreferenceGroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
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
    onPreferenceClick: () -> Unit = {} // Placeholder for click to show dialog
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPreferenceClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Text(text = summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SwitchPreferenceComposable(
    title: String,
    summary: String? = null,
    key: String,
    defaultValue: Boolean,
    onCheckedChange: (Boolean) -> Unit = {} // To be connected to ViewModel
) {
    var isChecked by remember { mutableStateOf(defaultValue) } // Internal state for now

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                isChecked = !isChecked
                onCheckedChange(isChecked)
            }
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
                onCheckedChange(it)
            },
            modifier = Modifier.padding(start = 16.dp) // Padding between text and switch
        )
    }
}

@Composable
fun ClickablePreferenceComposable(
    title: String,
    summary: String? = null,
    key: String,
    onPreferenceClick: () -> Unit = {} // To be connected to navigation or action
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPreferenceClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        if (summary != null) {
            Text(text = summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    // Assuming you have a Theme set up for your project, wrap with it for accurate preview
    // YourAppTheme { SettingsScreen() }
    SettingsScreen()
}
