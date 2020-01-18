package net.imknown.android.forefrontinfo

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.coroutines.*
import net.imknown.android.forefrontinfo.base.IAndroidVersion
import net.imknown.android.forefrontinfo.ui.home.HomeFragment
import net.imknown.android.forefrontinfo.ui.others.OthersFragment
import net.imknown.android.forefrontinfo.ui.settings.SettingsFragment

class MainActivity : AppCompatActivity(), IAndroidVersion, CoroutineScope by MainScope() {

    companion object {
        private const val BUNDLE_ID_LAST_ID = "BUNDLE_ID_LAST_ID"
    }

    private val isBroughtToFront
        get() = intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0

    @IdRes
    private var lastId = R.id.navigation_home

    private val onNavigationItemSelectedListener =
        BottomNavigationView.OnNavigationItemSelectedListener listener@{
            launch(Dispatchers.Default) {
                if (it.itemId != lastId) {
                    switchFragment(it.itemId)
                }
            }

            true
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isBroughtToFront) {
            finish()
            return
        }

        setContentView(R.layout.main_activity)

        initViews()

        launch(Dispatchers.Default) {
            if (savedInstanceState == null) {
                // This is not the activity which is recreated
                showFirstFragment()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(BUNDLE_ID_LAST_ID, lastId)
    }

    private fun initViews() {
        initSubtitle()

        bottomNavigationView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)
    }

    private fun initSubtitle() {
        setSupportActionBar(toolbar)

        val subtitleTextView = Toolbar::class.java
            .getDeclaredField("mSubtitleTextView")
            .also { it.isAccessible = true }
            .get(toolbar) as TextView
        subtitleTextView.isVerticalScrollBarEnabled = false
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        lastId = savedInstanceState.getInt(BUNDLE_ID_LAST_ID)
    }

    /**
     * 'first' means the first fragment in the stack, not the first tab in the BottomNavigationView
     * This is because of the recreation of Activity
     */
    private suspend fun showFirstFragment() {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.drop_scale, FragmentTransaction.TRANSIT_NONE)

        createFragment(fragmentTransaction, lastId)

        fragmentTransaction.commitAllowingStateLoss()
    }

    private suspend fun switchFragment(@IdRes selectedId: Int) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.drop_scale, FragmentTransaction.TRANSIT_NONE)

        val isFragmentCreated = isFragmentCreated(selectedId)
        val selectedFragment = getFragment(fragmentTransaction, selectedId)

        if (isFragmentCreated) {
            fragmentTransaction.show(selectedFragment)
        }

        val lastFragment = getFragment(fragmentTransaction, lastId)
        fragmentTransaction.hide(lastFragment).commitAllowingStateLoss()

        lastId = selectedId
    }

    private fun findFragmentByTag(@IdRes id: Int) =
        supportFragmentManager.findFragmentByTag(id.toString())

    private fun isFragmentCreated(@IdRes id: Int) =
        findFragmentByTag(id) != null

    private suspend fun getFragment(fragmentTransaction: FragmentTransaction, @IdRes id: Int) =
        findFragmentByTag(id) ?: createFragment(fragmentTransaction, id)

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

    override fun onBackPressed() {
        launch(Dispatchers.Default) {
            if (isAtLeastAndroid10()) {
                // https://github.com/ChuckerTeam/chucker/issues/102
                // https://issuetracker.google.com/issues/139738913
                finishAfterTransition()
            } else {
                withContext(Dispatchers.Main) {
                    super.onBackPressed()
                }
            }
        }
    }
}
