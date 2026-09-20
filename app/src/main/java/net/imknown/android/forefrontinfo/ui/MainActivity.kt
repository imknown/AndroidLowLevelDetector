package net.imknown.android.forefrontinfo.ui

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import net.imknown.android.forefrontinfo.base.AppThemeMode
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.common.isAtLeastAndroid10
import net.imknown.android.forefrontinfo.ui.theme.AppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // https://developer.android.com/design/ui/mobile/guides/foundations/system-bars#button_modes
        // https://developer.android.com/develop/ui/views/layout/edge-to-edge#create-transparent
        // https://developer.android.com/develop/ui/views/layout/edge-to-edge-manually#change-color
        // https://developer.android.com/develop/ui/compose/layouts/system-bars#create-transparent
        // Unify the "is the app dark" source: detectDarkMode reads MyApplication.themeMode, so a config change can't let edge-to-edge overwrite the SideEffect result with the system value
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                ::isAppDark,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                ::isAppDark,
            ),
        ) // edge to edge: content draws behind system bars (insets handled by Scaffold)
        if (isAtLeastAndroid10()) {
            window.isNavigationBarContrastEnforced = false // no forced contrast scrim on the navigation bar
        }

        super.onCreate(savedInstanceState)

        setContent { // Compose-flavored setContentView: this tree becomes the whole UI
            AppTheme { // wrap the theme at the root: dark/light and dynamic color flow down the tree
                AppRoot() // skeleton (Scaffold + navigation) + four pages, all grown from this one function
            }
        }
    }

    // Single source of truth for "is the app dark", shared by edge-to-edge and AppTheme's system-bar SideEffect; aligned with themeMode
    private fun isAppDark(resources: Resources): Boolean = when (MyApplication.themeMode.value) {
        AppThemeMode.AlwaysLight -> false
        AppThemeMode.AlwaysDark -> true
        AppThemeMode.FollowSystem -> (
                resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                ) == Configuration.UI_MODE_NIGHT_YES
    }
}
