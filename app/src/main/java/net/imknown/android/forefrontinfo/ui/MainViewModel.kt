package net.imknown.android.forefrontinfo.ui

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.ui.base.IAndroidVersion
import net.imknown.android.forefrontinfo.ui.home.HomeFragment
import net.imknown.android.forefrontinfo.ui.others.OthersFragment
import net.imknown.android.forefrontinfo.ui.settings.SettingsFragment

class MainViewModel : ViewModel(), IAndroidVersion {

    companion object {
        private const val BUNDLE_ID_LAST_ID = "BUNDLE_ID_LAST_ID"
    }

    @IdRes
    private var lastId = R.id.navigation_home

    fun switchFragment(
        supportFragmentManager: FragmentManager,
        @IdRes selectedId: Int
    ) = viewModelScope.launch(Dispatchers.Default) {
        if (selectedId == lastId) {
            return@launch
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

        withContext(Dispatchers.Main) {
            fragmentTransaction.commitNowAllowingStateLoss()
        }

        lastId = selectedId
    }

    private fun findFragmentByTag(supportFragmentManager: FragmentManager, @IdRes id: Int) =
        supportFragmentManager.findFragmentByTag(id.toString())

    private fun isFragmentCreated(supportFragmentManager: FragmentManager, @IdRes id: Int) =
        findFragmentByTag(supportFragmentManager, id) != null

    private suspend fun getFragment(
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
    ) = viewModelScope.launch(Dispatchers.Default) {
        if (savedInstanceState != null) {
            // This is the activity which is recreated
            return@launch
        }

        val fragmentTransaction = supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.drop_scale, FragmentTransaction.TRANSIT_NONE)

        createFragment(fragmentTransaction, lastId)

        withContext(Dispatchers.Main) {
            fragmentTransaction.commitNowAllowingStateLoss()
        }
    }

    private suspend fun createFragment(fragmentTransaction: FragmentTransaction, @IdRes id: Int): Fragment {
        val fragment: Fragment = when (id) {
            R.id.navigation_home -> {
                HomeFragment.newInstance()
            }

            R.id.navigation_others -> {
                OthersFragment.newInstance()
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

    fun saveInstanceState(outState: Bundle) {
        outState.putInt(BUNDLE_ID_LAST_ID, lastId)
    }

    fun restoreInstanceState(savedInstanceState: Bundle) {
        lastId = savedInstanceState.getInt(BUNDLE_ID_LAST_ID)
    }
}