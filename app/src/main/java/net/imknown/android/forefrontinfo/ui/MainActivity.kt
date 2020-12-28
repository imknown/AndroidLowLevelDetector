package net.imknown.android.forefrontinfo.ui

import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import net.imknown.android.forefrontinfo.databinding.MainActivityBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: MainActivityBinding

    private val mainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()

        mainViewModel.showTheTopFragmentOfTheStack(savedInstanceState, supportFragmentManager)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        mainViewModel.saveInstanceState(outState)
    }

    private fun initViews() {
        initSubtitle()

        binding.bottomNavigationView.setOnNavigationItemSelectedListener {
            mainViewModel.switchFragment(supportFragmentManager, it.itemId)

            true
        }
    }

    private fun initSubtitle() {
        setSupportActionBar(binding.toolbar)

        val subtitleTextView = Toolbar::class.java
            .getDeclaredField("mSubtitleTextView")
            .apply { isAccessible = true }
            .get(binding.toolbar) as TextView
        subtitleTextView.isVerticalScrollBarEnabled = false
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        mainViewModel.restoreInstanceState(savedInstanceState)
    }
}
