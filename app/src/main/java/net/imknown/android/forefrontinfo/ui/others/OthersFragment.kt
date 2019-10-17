package net.imknown.android.forefrontinfo.ui.others

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import net.imknown.android.forefrontinfo.R

class OthersFragment : Fragment() {

    companion object {
        fun newInstance() = OthersFragment()
    }

    private lateinit var othersViewModel: OthersViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        othersViewModel =
            ViewModelProviders.of(this).get(OthersViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_others, container, false)
        val textView: TextView = root.findViewById(R.id.text_dashboard)
        othersViewModel.text.observe(this, Observer {
            textView.text = it
        })
        return root
    }
}