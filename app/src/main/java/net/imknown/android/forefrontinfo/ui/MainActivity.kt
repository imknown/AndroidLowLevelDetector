package net.imknown.android.forefrontinfo.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import kotlinx.android.synthetic.main.main_activity.*
import net.imknown.android.forefrontinfo.R

class MainActivity : AppCompatActivity() {

    private val isBroughtToFront
        get() = intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0

    private val mainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isBroughtToFront) {
            finish()
            return
        }

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
}
