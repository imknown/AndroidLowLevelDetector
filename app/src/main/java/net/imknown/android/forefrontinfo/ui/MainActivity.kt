package net.imknown.android.forefrontinfo.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import net.imknown.android.forefrontinfo.databinding.MainActivityBinding
import net.imknown.android.forefrontinfo.ui.base.ext.viewBinding
import net.imknown.android.forefrontinfo.ui.common.isAtLeastAndroid10
import net.imknown.android.forefrontinfo.ui.theme.AppTheme

class MainActivity : AppCompatActivity() { // kept on purpose: the four theme modes rely on AppCompatDelegate (legacy optimization, chapter 10)

    // BaseListFragment (deleted in step 7) still references this binding; remove it together with main_activity.xml then
    internal val binding by viewBinding(MainActivityBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        // https://developer.android.com/design/ui/mobile/guides/foundations/system-bars#button_modes
        // https://developer.android.com/develop/ui/views/layout/edge-to-edge#create-transparent
        // https://developer.android.com/develop/ui/views/layout/edge-to-edge-manually#change-color
        // https://developer.android.com/develop/ui/compose/layouts/system-bars#create-transparent
        enableEdgeToEdge() // edge to edge: content draws behind system bars (insets handled by Scaffold)
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
}
