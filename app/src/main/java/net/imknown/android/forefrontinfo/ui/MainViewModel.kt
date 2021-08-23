package net.imknown.android.forefrontinfo.ui

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.ui.home.HomeFragment
import net.imknown.android.forefrontinfo.ui.others.OthersFragment
import net.imknown.android.forefrontinfo.ui.prop.PropFragment
import net.imknown.android.forefrontinfo.ui.settings.SettingsFragment

class MainViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    companion object {
        private const val SAVED_STATE_HANDLE_KEY_LAST_ID = "SAVED_STATE_HANDLE_KEY_LAST_ID"
    }

    @IdRes
    private var lastId = getSavedStateLastId()

    fun switchFragment(
        supportFragmentManager: FragmentManager,
        @IdRes selectedId: Int
    ) {
        if (selectedId == lastId) {
            return
        }

        val fragmentTransaction = supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.drop_scale, FragmentTransaction.TRANSIT_NONE)

        val isFragmentCreated = isFragmentCreated(supportFragmentManager, selectedId)
        val selectedFragment = getFragment(supportFragmentManager, fragmentTransaction, selectedId)

        if (isFragmentCreated) {
            fragmentTransaction.show(selectedFragment)
        }

        val lastFragment = getFragment(supportFragmentManager, fragmentTransaction, lastId)
        fragmentTransaction.hide(lastFragment)

        fragmentTransaction.setReorderingAllowed(true)

        fragmentTransaction.commitNowAllowingStateLoss()

        lastId = selectedId

        setSavedStateLastId()
    }

    private fun findFragmentByTag(supportFragmentManager: FragmentManager, @IdRes id: Int) =
        supportFragmentManager.findFragmentByTag(id.toString())

    private fun isFragmentCreated(supportFragmentManager: FragmentManager, @IdRes id: Int) =
        findFragmentByTag(supportFragmentManager, id) != null

    private fun getFragment(
        supportFragmentManager: FragmentManager,
        fragmentTransaction: FragmentTransaction, @IdRes id: Int
    ) = findFragmentByTag(supportFragmentManager, id)
        ?: createFragment(fragmentTransaction, id)

    /**
     * 'top' means the top fragment in the stack, not the first tab in the order of BottomNavigationView
     * This is because of the recreation of Activity
     */
    fun showTheTopFragmentOfTheStack(
        savedInstanceState: Bundle?,
        supportFragmentManager: FragmentManager
    ) {
        if (savedInstanceState != null) {
            // This is the activity which is recreated
            return
        }

        val fragmentTransaction = supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.drop_scale, FragmentTransaction.TRANSIT_NONE)

        createFragment(fragmentTransaction, lastId)

        fragmentTransaction.setReorderingAllowed(true)

        fragmentTransaction.commitNowAllowingStateLoss()
    }

    private fun createFragment(
        fragmentTransaction: FragmentTransaction,
        @IdRes id: Int
    ): Fragment {
        val fragment: Fragment = when (id) {
            R.id.navigation_home -> {
                HomeFragment.newInstance()
            }

            R.id.navigation_others -> {
                OthersFragment.newInstance()
            }

            R.id.navigation_prop -> {
                PropFragment.newInstance()
            }

            R.id.navigation_settings -> {
                SettingsFragment.newInstance()
            }

            else -> {
                throw Exception()
            }
        }

        fragmentTransaction.add(R.id.container, fragment, id.toString())

        return fragment
    }

    private fun setSavedStateLastId() {
        savedStateHandle[SAVED_STATE_HANDLE_KEY_LAST_ID] = lastId
    }

    private fun getSavedStateLastId() = savedStateHandle[SAVED_STATE_HANDLE_KEY_LAST_ID]
        ?: R.id.navigation_home
}