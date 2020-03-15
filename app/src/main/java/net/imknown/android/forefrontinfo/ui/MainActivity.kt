package net.imknown.android.forefrontinfo.ui

import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import kotlinx.android.synthetic.main.main_activity.*
import net.imknown.android.forefrontinfo.R

class MainActivity : AppCompatActivity() {

    private val mainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity)

        initViews()

        mainViewModel.showTheTopFragmentOfTheStack(savedInstanceState, supportFragmentManager)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        mainViewModel.saveInstanceState(outState)
    }

    private fun initViews() {
        initSubtitle()

        bottomNavigationView.setOnNavigationItemSelectedListener {
            mainViewModel.switchFragment(supportFragmentManager, it.itemId)

            true
        }
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

        mainViewModel.restoreInstanceState(savedInstanceState)
    }

    override fun onBackPressed() {
        // Finish now, so no need to use async.

        if (is10Leak()) {
            finishAfterTransition()
        } else {
            super.onBackPressed()
        }
    }

    // https://github.com/square/leakcanary/issues/1594
    // https://issuetracker.google.com/issues/139738913
    private fun is10Leak() = Build.VERSION.SDK_INT == Build.VERSION_CODES.Q
            && (Build.ID.split('.').size < 2
            /* */ || Build.ID.split('.')[1] < "191205")
}
