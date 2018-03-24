package com.creomobile.lists.sample

import android.databinding.Bindable
import android.databinding.ObservableArrayList
import android.databinding.ObservableList
import com.creomobile.lists.SingleSelection

class ExpandableViewModel : ViewModelBase() {

    private val selection = SingleSelection.create<OrganizationItem>(
            SingleSelection.BehaviorType.SingleOrNone)

    init {
        items.set(createItems())
        selection.selectedItem.subscribe { updateIndex() }
    }

    override fun initList() {
        super.initList()
        selection.items = items.get()
    }

    private fun createPersons(): ObservableArrayList<Any> {
        val result = ObservableArrayList<Any>()
        result.addAll((1..3).map { createPersonItem() })
        return result
    }

    override fun createItems(): ObservableList<Any> {
        val list = ObservableArrayList<Any>()
        list.addAll((1..3).map {
            val org = createOrganizationItem()
            org.items.set(createPersons())
            org
        })
        return list
    }

    override fun getDefaultIndex(): Int? {
        val org = selection.currentSelectedItem ?: return super.getDefaultIndex()
        return super.getDefaultIndex(org.items.get()?.size ?: 0)
    }

    @Bindable
    override fun getCanClear(): Boolean {
        val org = selection.currentSelectedItem ?: return super.getCanClear()
        return org.items.get()?.size ?: 0 > 0
    }

    @Bindable
    override fun getCanAddOrReplace(): Boolean {
        val org = selection.currentSelectedItem ?: return super.getCanAddOrReplace()
        return getCanAddOrReplace(org.items.get()?.size ?: 0)
    }

    override fun remove(item: Any) {
        val items = items.get()!!
        if (items.remove(item)) return
        items
                .filterIsInstance<OrganizationItem>()
                .map { it.items.get() as? ObservableArrayList<Any> }
                .first { it?.contains(item) == true }!!
                .remove(item)
        updateIndex()
    }

    override fun addOrReplaceItem(item: Any) {
        val org = selection.currentSelectedItem
        if (org == null) {
            super.addOrReplaceItem(item)
            return
        }

        var items = org.items.get() as? ObservableArrayList<Any>
        if (items == null) {
            items = ObservableArrayList()
            org.items.set(items)
        }

        val index = index.get()!!
        if (replace.get()) items[index] = item
        else items.add(index, item)
        updateIndex()
    }

    override fun refresh() {
        val org = selection.currentSelectedItem
        if (org == null) {
            super.refresh()
            return
        }
        org.items.set(createPersons())
        onListUpdated()
    }

    override fun clear() {
        val org = selection.currentSelectedItem
        if (org == null) {
            super.clear()
            return
        }
        org.items.set(null)
        onListUpdated()
    }
}