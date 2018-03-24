package com.creomobile.lists.sample

import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.creomobile.lists.RecycleExpandableAdapter
import com.creomobile.lists.sample.databinding.FragmentExpandableBinding
import kotlinx.android.synthetic.main.fragment_list.*

class ExpandableFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentExpandableBinding>(inflater,
                R.layout.fragment_expandable, container, false)
        val viewModel = ViewModelProviders.of(this).get(ExpandableViewModel::class.java)
        binding.vm = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = RecycleExpandableAdapter.Builder()
                    .addView<PersonItem>(R.layout.item_person, BR.vm)
                    .addView<OrganizationItem>(R.layout.item_organization, BR.vm)
                    .addView<SeparatorItem>(R.layout.item_separator, BR.vm)
                    .withChildrenMarginStart()
                    .scrollToInserted()
                    .scrollToSelected()
                    .build()
        }
    }

    companion object {
        fun newInstance() = ExpandableFragment()
    }
}
