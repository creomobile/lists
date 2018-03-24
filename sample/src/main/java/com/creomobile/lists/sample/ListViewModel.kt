package com.creomobile.lists.sample

import android.databinding.ObservableArrayList
import android.databinding.ObservableField
import android.databinding.ObservableList
import com.creomobile.lists.Selection
import com.creomobile.lists.SingleSelection
import io.reactivex.disposables.CompositeDisposable

class ListViewModel : ViewModelBase() {

    private val selectionSubscriptions = CompositeDisposable()
    private var selection: Selection<PersonItem>? = null
        set(value) {
            selectionSubscriptions.clear()
            field = value
            if (value == null) {
                selectedCount.set(null)
                selectedId.set(null)
                selectedIds.set(null)
            } else {
                selectionSubscriptions.addAll(
                        value.selectedCount.subscribe { selectedCount.set(it) },
                        value.selectedItems.subscribe {
                            selectedIds.set(it.joinToString { it.id.toString() })
                        })
                if (value is SingleSelection<PersonItem>)
                    selectionSubscriptions.add((value).selectedItem.subscribe { selectedId.set(it?.value?.id) })
                else
                    selectedId.set(null)
            }
        }
    val selectionBehaviors = (listOf(null) + Selection.BehaviorType.values())
            .map { SelectionBehaviorItem(it) as Any }
    var selectionBehavior = object : ObservableField<Any?>() {
        override fun set(value: Any?) {
            super.set(value)
            resetSelection()
        }
    }
    val selectedCount = ObservableField<Int?>()
    val selectedIds = ObservableField<String?>()
    val selectedId = ObservableField<Int?>()

    init {
        items.set(createItems())
    }

    override fun initList() {
        super.initList()
        resetSelection()
    }

    private fun resetSelection() {
        val items = items.get() ?: return
        Selection.release(items)
        val behavior = (selectionBehavior.get() as? SelectionBehaviorItem)?.selectionBehavior
        selection = if (behavior == null) null else Selection.apply(items, behavior)
    }

    override fun createItems(): ObservableList<Any> {
        val list = ObservableArrayList<Any>()
        list.addAll((1..5).map { createPersonItem() })
        return list
    }

    private class SelectionBehaviorItem(val selectionBehavior: Selection.BehaviorType?) {
        override fun toString() = selectionBehavior?.toString() ?: "Behavior"
    }
}
