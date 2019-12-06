package net.imknown.android.forefrontinfo

import android.content.Intent
import android.os.Bundle
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.coroutines.*
import net.imknown.android.forefrontinfo.ui.home.HomeFragment
import net.imknown.android.forefrontinfo.ui.others.OthersFragment
import net.imknown.android.forefrontinfo.ui.settings.SettingsFragment

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    companion object {
        private const val BUNDLE_ID_LAST_ID = "BUNDLE_ID_LAST_ID"

        @ColorInt
        var COLOR_STATE_LIST_DEFAULT_STYLE: Int = 0
        @ColorInt
        var COLOR_STATE_LIST_NO_PROBLEM: Int = 0
        @ColorInt
        var COLOR_STATE_LIST_WARNING: Int = 0
        @ColorInt
        var COLOR_STATE_LIST_CRITICAL: Int = 0
    }

    private val isBroughtToFront
        get() = intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0

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

    private fun initColors() {
        COLOR_STATE_LIST_DEFAULT_STYLE = getMyColor(R.color.colorStateless)
        COLOR_STATE_LIST_NO_PROBLEM = getMyColor(R.color.colorNoProblem)
        COLOR_STATE_LIST_WARNING = getMyColor(R.color.colorWaring)
        COLOR_STATE_LIST_CRITICAL = getMyColor(R.color.colorCritical)
    }

    private fun getMyColor(@ColorRes id: Int) = ContextCompat.getColor(this, id)

    private fun initTheme() {
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(MyApplication.instance)
        val themesValue =
            sharedPreferences.getString(
                MyApplication.getMyString(R.string.interface_themes_key),
                MyApplication.getMyString(R.string.interface_themes_follow_system_value)
            )!!
        MyApplication.instance.setMyTheme(themesValue)

        initColors()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isBroughtToFront) {
            finish()
            return
        }

        setContentView(R.layout.main_activity)

        val actionBar = supportActionBar
        actionBar?.subtitle = MyApplication.getMyString(R.string.lld_json_checking)

        nav_view.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)

        launch {
            withContext(Dispatchers.IO) {
                initTheme()

                withContext(Dispatchers.Main) {
                    if (savedInstanceState == null) {
                        showFragment(lastId)
                    }
//                    else {
//                        val fragment = supportFragmentManager.findFragmentById(R.id.container)
//                    }
                }
            }
        }
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
            .setCustomAnimations(R.anim.drop_scale, FragmentTransaction.TRANSIT_NONE)
            .hide(getFragmentInstance(lastId))
            .show(getFragmentInstance(selectedId))
            .commitNowAllowingStateLoss()

        lastId = selectedId
    }

    private fun getFragmentInstance(@IdRes id: Int): Fragment {
        var fragment = supportFragmentManager.findFragmentByTag(id.toString())

        if (fragment == null) {
            fragment = createFragment(id)
            addFragment(fragment, id)
        }

        return fragment
    }

    private fun createFragment(@IdRes id: Int): Fragment {
        return when (id) {
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
    }

    private fun addFragment(fragment: Fragment, @IdRes id: Int) {
        if (fragment.isAdded) {
            return
        }

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.drop_scale, FragmentTransaction.TRANSIT_NONE)
            .add(R.id.container, fragment, id.toString())
            .commitNowAllowingStateLoss()
    }

    override fun onBackPressed() {
        super.onBackPressed()

        launch {
            withContext(Dispatchers.IO) {
                delay(300)

                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }
}
