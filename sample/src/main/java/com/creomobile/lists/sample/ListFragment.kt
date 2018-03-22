package com.creomobile.lists.sample

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.creomobile.lists.RecycleListAdapter
import com.creomobile.lists.Selection
import com.creomobile.lists.sample.databinding.FragmentListBinding
import kotlinx.android.synthetic.main.fragment_list.*

class ListFragment : Fragment() {

    lateinit var binding: FragmentListBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate<FragmentListBinding>(inflater, R.layout.fragment_list,
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
                    .addView<PersonItem>(R.layout.item_person, BR.vm)
                    .addView<OrganizationItem>(R.layout.item_organization, BR.vm)
                    .addView<SeparatorItem>(R.layout.item_separator, BR.vm)
                    .build()
        }
    }

    companion object {
        fun newInstance(): ListFragment = ListFragment()
    }
}
