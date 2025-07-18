package net.imknown.android.forefrontinfo.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commitNow
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.databinding.MainActivityBinding
import net.imknown.android.forefrontinfo.ui.base.ext.viewBinding
import net.imknown.android.forefrontinfo.ui.base.ext.windowInsetsCompatTypes
import net.imknown.android.forefrontinfo.ui.common.isAtLeastStableAndroid10
import net.imknown.android.forefrontinfo.ui.home.HomeFragment
import net.imknown.android.forefrontinfo.ui.others.OthersFragment
import net.imknown.android.forefrontinfo.ui.prop.PropFragment
import net.imknown.android.forefrontinfo.ui.settings.SettingsFragment

class MainActivity : AppCompatActivity() {

    internal val binding by viewBinding(MainActivityBinding::inflate)

    private val mainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        // https://developer.android.com/design/ui/mobile/guides/foundations/system-bars#button_modes
        // https://developer.android.com/develop/ui/views/layout/edge-to-edge#create-transparent
        // https://developer.android.com/develop/ui/views/layout/edge-to-edge-manually#change-color
        // https://developer.android.com/develop/ui/compose/layouts/system-bars#create-transparent
        enableEdgeToEdge()
        if (isAtLeastStableAndroid10()) {
            window.setNavigationBarContrastEnforced(false)
        }

        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        initWindowInsets()

        initViews()

        if (savedInstanceState == null) {
            supportFragmentManager.switch(R.id.navigation_home, true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

//        mainViewModel.removeRequestPermissionResultListener()
    }

    private fun initWindowInsets() {
        // https://developer.android.com/develop/ui/views/layout/edge-to-edge#material-components
        // https://developer.android.com/develop/ui/compose/layouts/insets#material3-components
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBar) { appBar, windowInsetsCompat ->
            val insets = windowInsetsCompat.getInsets(windowInsetsCompatTypes)
            appBar.updatePadding(
                left = insets.left,
                right = insets.right,
                top = insets.top
            )

            windowInsetsCompat
        }

        // https://developer.android.com/develop/ui/views/layout/edge-to-edge#material-components
        // https://developer.android.com/develop/ui/compose/layouts/insets#material3-components
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigationView) { bnView, windowInsetsCompat ->
            val insets = windowInsetsCompat.getInsets(windowInsetsCompatTypes)
            bnView.updatePadding(
                left = insets.left,
                right = insets.right,
                bottom = insets.bottom
            )

            windowInsetsCompat
        }
    }

    private fun initViews() {
        setSupportActionBar(binding.toolbar)

        binding.bottomNavigationView.setOnItemSelectedListener {
            supportFragmentManager.switch(it.itemId)

            true
        }
    }

    private fun FragmentManager.switch(@IdRes selectedId: Int, isFirst: Boolean = false) {
        val lastId = mainViewModel.lastId
        if (selectedId == lastId && !isFirst) {
            return
        }

        commitNow(true) {
            setCustomAnimations(R.anim.drop_scale, FragmentTransaction.TRANSIT_NONE)

            val selectedFragment = findFragmentByTag(selectedId.toString())
                ?: createFragment(selectedId)
            show(selectedFragment)

            findFragmentByTag(lastId.toString())?.let {
                hide(it)
            }

            setReorderingAllowed(true)
        }

        mainViewModel.setSavedStateLastId(selectedId)
    }

    private fun FragmentTransaction.createFragment(@IdRes id: Int): Fragment = when (id) {
        R.id.navigation_home -> HomeFragment.newInstance()
        R.id.navigation_others -> OthersFragment.newInstance()
        R.id.navigation_prop -> PropFragment.newInstance()
        R.id.navigation_settings -> SettingsFragment.newInstance()
        else -> throw Exception()
    }.apply {
        add(R.id.container, this, id.toString())
    }
}
