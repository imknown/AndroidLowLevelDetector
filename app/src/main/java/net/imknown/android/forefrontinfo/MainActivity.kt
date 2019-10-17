package net.imknown.android.forefrontinfo

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.main_activity.*
import net.imknown.android.forefrontinfo.ui.home.HomeFragment
import net.imknown.android.forefrontinfo.ui.others.OthersFragment
import net.imknown.android.forefrontinfo.ui.settings.SettingsFragment

class MainActivity : AppCompatActivity() {

    companion object {
        private const val BUNDLE_ID_LAST_ID = "BUNDLE_ID_LAST_ID"
    }

    @IdRes
    private var lastId = R.id.navigation_home

    private val onNavigationItemSelectedListener =
        BottomNavigationView.OnNavigationItemSelectedListener listener@{
            if (it.itemId != lastId) {
                when (it.itemId) {
                    R.id.navigation_home,
                    R.id.navigation_others,
                    R.id.navigation_settings -> {
                        showFragment(it.itemId)
                        return@listener true
                    }
                }
            }

            false
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        nav_view.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)

        if (savedInstanceState == null) {
            showFragment(lastId)
        }
//        else {
//            val fragment = supportFragmentManager.findFragmentById(R.id.container)
//        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(BUNDLE_ID_LAST_ID, lastId)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        lastId = savedInstanceState.getInt(BUNDLE_ID_LAST_ID)
    }

    private fun showFragment(@IdRes selectedId: Int) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.fade_out)
            .hide(getFragmentInstance(lastId))
            .show(getFragmentInstance(selectedId))
            .commitNow()

        lastId = selectedId
    }

    private fun getFragmentInstance(@IdRes id: Int): Fragment {
        val fragmentInstance: Fragment

        when (id) {
            R.id.navigation_home -> {
                fragmentInstance = HomeFragment.newInstance()
            }

            R.id.navigation_others -> {
                fragmentInstance = OthersFragment.newInstance()
            }

            R.id.navigation_settings -> {
                fragmentInstance = SettingsFragment.newInstance()
            }

            else -> {
                throw Exception()
            }
        }

        return createOrReuseFragment(fragmentInstance, id)
    }

    private fun createOrReuseFragment(fragmentInstance: Fragment, @IdRes id: Int): Fragment {
        var fragment = supportFragmentManager.findFragmentByTag(id.toString())

        if (fragment == null) {
            fragment = fragmentInstance
            addFragment(fragment, id)
        }

        return fragment
    }

    private fun addFragment(fragment: Fragment, @IdRes id: Int) {
        supportFragmentManager.beginTransaction()
            .add(R.id.container, fragment, id.toString())
            .commitNow()
    }

    override fun onBackPressed() {
        // https://github.com/ChuckerTeam/chucker/issues/102
        // https://issuetracker.google.com/issues/139738913
        finishAfterTransition()
    }
}
