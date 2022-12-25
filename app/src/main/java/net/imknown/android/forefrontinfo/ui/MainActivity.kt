package net.imknown.android.forefrontinfo.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commitNow
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.mvvm.IView
import net.imknown.android.forefrontinfo.base.mvvm.viewBinding
import net.imknown.android.forefrontinfo.databinding.MainActivityBinding
import net.imknown.android.forefrontinfo.ui.home.HomeFragment
import net.imknown.android.forefrontinfo.ui.others.OthersFragment
import net.imknown.android.forefrontinfo.ui.prop.PropFragment
import net.imknown.android.forefrontinfo.ui.settings.SettingsFragment

class MainActivity : AppCompatActivity(), IView {

    private val binding by viewBinding(MainActivityBinding::inflate)

    private val mainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        initViews()

        if (savedInstanceState == null) {
            supportFragmentManager.switch(R.id.navigation_home, true)
        }

        mainViewModel.requestShizikuPermission()
    }

    private fun initViews() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        ViewCompat.setOnApplyWindowInsetsListener(binding.appBar) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = insets.left,
                right = insets.right,
                top = insets.top
            )

            windowInsets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigationView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = insets.left,
                right = insets.right,
                bottom = insets.bottom
            )

            windowInsets
        }

        initSubtitle()

        binding.bottomNavigationView.setOnItemSelectedListener {
            supportFragmentManager.switch(it.itemId)

            true
        }
    }

    private fun initSubtitle() {
        setSupportActionBar(binding.toolbar)
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
