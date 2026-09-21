package net.imknown.android.forefrontinfo.ui

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.common.isAtLeastAndroid10
import net.imknown.android.forefrontinfo.ui.theme.AppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // The manifest theme (values/values-night AppTheme pair) follows the *system* configuration, which
        // is wrong for the activity window when the in-app theme overrides it ("always dark" on a light
        // system would cold-start with a light window background and light status bar icons until
        // Compose's first frame + SideEffect catch up). Switch to the enforced variant matching the app
        // theme here — before enableEdgeToEdge, which instantiates the decor view and locks the theme in.
        setTheme(if (isAppDark(resources)) R.style.AppTheme_Dark else R.style.AppTheme_Light)

        // https://developer.android.com/design/ui/mobile/guides/foundations/system-bars#button_modes
        // https://developer.android.com/develop/ui/views/layout/edge-to-edge#create-transparent
        // https://developer.android.com/develop/ui/views/layout/edge-to-edge-manually#change-color
        // https://developer.android.com/develop/ui/compose/layouts/system-bars#create-transparent
        // The default detectDarkMode reads the *system* uiMode — identical to ours only in follow-system
        // mode. Passing ::isAppDark is belt-and-suspenders rather than load-bearing: it makes every writer
        // of the bar appearance (theme initial value -> enableEdgeToEdge -> AppTheme's SideEffect) derive
        // from themeMode, so no intermediate wrong state ever exists and correctness doesn't rest on the
        // implicit "SideEffect overwrites before the first presented frame" ordering. It would become
        // required if the scrims were ever non-transparent (detectDarkMode also picks the scrim).
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

    // Single source of truth for "is the app dark", shared by edge-to-edge and AppTheme's system-bar SideEffect;
    // the three-way mapping lives on AppThemeMode.isDark so every caller stays aligned
    private fun isAppDark(resources: Resources): Boolean = MyApplication.themeMode.value.isDark(
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES
    )
}
