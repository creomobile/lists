package com.creomobile.lists.sample

import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.creomobile.lists.RecycleListAdapter
import com.creomobile.lists.sample.databinding.FragmentListBinding
import kotlinx.android.synthetic.main.fragment_list.*

class ListFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentListBinding>(inflater, R.layout.fragment_list,
                container, false)
        val viewModel = ViewModelProviders.of(this).get(ListViewModel::class.java)
        binding.vm = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = RecycleListAdapter.Builder()
                    .addView<PersonItem>(R.layout.item_person_selectable, BR.vm)
                    .addView<OrganizationItem>(R.layout.item_organization, BR.vm)
                    .addView<SeparatorItem>(R.layout.item_separator, BR.vm)
                    .scrollToInserted()
                    .build()
        }
    }

    companion object {
        fun newInstance(): ListFragment = ListFragment()
    }
}
