package net.imknown.android.forefrontinfo.ui

import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.mvvm.IView
import net.imknown.android.forefrontinfo.base.mvvm.viewBinding
import net.imknown.android.forefrontinfo.databinding.MainActivityBinding

class MainActivity : AppCompatActivity(), IView {

    private val binding by viewBinding(MainActivityBinding::inflate)

    private val mainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        MyApplication.initTheme()

        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        initViews()

        mainViewModel.showTheTopFragmentOfTheStack(savedInstanceState, supportFragmentManager)
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

        lifecycleScope.launch(Dispatchers.IO) {
            val subtitleTextView = Toolbar::class.java
                .getDeclaredField("mSubtitleTextView")
                .apply { isAccessible = true }
                .get(binding.toolbar) as TextView
            subtitleTextView.isVerticalScrollBarEnabled = false
        }
    }
}
