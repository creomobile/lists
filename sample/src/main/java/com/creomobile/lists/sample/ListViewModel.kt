package com.creomobile.lists.sample

import android.arch.lifecycle.ViewModel
import android.databinding.*
import com.creomobile.lists.Selection
import com.creomobile.lists.SingleSelection
import io.reactivex.disposables.CompositeDisposable
import java.lang.ref.WeakReference

class ListViewModel : ViewModel(), Observable {

    private val registry = PropertyChangeRegistry()
    private val listChangedCallback = OnListChangedCallback(this)
    val items = object : ObservableField<ObservableList<PersonItem>>() {
        override fun set(value: ObservableList<PersonItem>?) {
            super.get()?.removeOnListChangedCallback(listChangedCallback)
            super.set(value)
            value?.addOnListChangedCallback(listChangedCallback)
            onListUpdated()
            resetSelection()
        }
    }
    val index = object : ObservableField<Int?>() {
        override fun set(value: Int?) {
            super.set(value)
            registry.notifyChange(this@ListViewModel, BR.canAdd)
        }
    }
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
    val selectionBehavior = object : ObservableField<Any?>() {
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

    @Bindable
    fun getCanAdd(): Boolean {
        val index = index.get() ?: return false
        return index <= items.get()?.size ?: 0
    }

    @Bindable
    fun getCanClear() = items.get()?.size ?: 0 > 0

    @Bindable
    fun getCanReplace() = items.get()?.size ?: 0 > 1

    private fun resetSelection() {
        val items = items.get() ?: return
        Selection.release(items)
        val behavior = (selectionBehavior.get() as? SelectionBehaviorItem)?.selectionBehavior
        selection = if (behavior == null) null else Selection.apply(items, behavior)
    }

    private fun createItem() = PersonItem.createNew(::remove)
    private fun createItems(): ObservableList<PersonItem> {
        val list = ObservableArrayList<PersonItem>()
        list.addAll((1..5).map { createItem() })
        return list
    }

    private fun onListUpdated() {
        index.set(items.get()?.size ?: 0)
        registry.notifyChange(this, BR.canClear)
        registry.notifyChange(this, BR.canReplace)
    }

    fun refresh() = items.set(createItems())
    fun clear() = items.set(null)
    fun replace() {
        val items = items.get()
        val size = items?.size ?: 0
        if (size == 0) return
        val index = (0 until size).random()
        items[index] = createItem()
    }

    fun insert() {
        var items = this.items.get()
        if (items == null) {
            items = ObservableArrayList<PersonItem>()
            this.items.set(items)
        }
        items.add(index.get()!!, createItem())
    }

    private fun remove(item: PersonItem) {
        items.get()?.remove(item)
    }

    /*Observable*/
    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) =
            registry.add(callback)

    override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) =
            registry.remove(callback)

    private class OnListChangedCallback(viewModel: ListViewModel) :
            ObservableList.OnListChangedCallback<ObservableList<PersonItem>>() {
        private val viewModelReference = WeakReference<ListViewModel>(viewModel)

        override fun onChanged(sender: ObservableList<PersonItem>) {
            viewModelReference.get()?.onListUpdated()
        }

        override fun onItemRangeRemoved(sender: ObservableList<PersonItem>, positionStart: Int, itemCount: Int) {
            viewModelReference.get()?.onListUpdated()
        }

        override fun onItemRangeMoved(sender: ObservableList<PersonItem>, fromPosition: Int, toPosition: Int, itemCount: Int) {
            viewModelReference.get()?.onListUpdated()
        }

        override fun onItemRangeInserted(sender: ObservableList<PersonItem>, positionStart: Int, itemCount: Int) {
            viewModelReference.get()?.onListUpdated()
        }

        override fun onItemRangeChanged(sender: ObservableList<PersonItem>, positionStart: Int, itemCount: Int) {
            viewModelReference.get()?.onListUpdated()
        }
    }

    private class SelectionBehaviorItem(val selectionBehavior: Selection.BehaviorType?) {
        override fun toString() = selectionBehavior?.toString() ?: "Selection Behavior"
    }
}
