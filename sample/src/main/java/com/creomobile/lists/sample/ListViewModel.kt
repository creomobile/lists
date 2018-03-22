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
    val items = object : ObservableField<ObservableList<Any>>() {
        override fun set(value: ObservableList<Any>?) {
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
            registry.notifyChange(this@ListViewModel, BR.canAddOrReplace)
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
    val replace = object : ObservableBoolean() {
        override fun set(value: Boolean) {
            val isDefaultIndex = index.get() == getDefaultIndex()
            super.set(value)
            if (isDefaultIndex) updateIndex()
            registry.notifyChange(this@ListViewModel, BR.canAddOrReplace)
        }
    }

    init {
        items.set(createItems())
    }

    @Bindable
    fun getCanAddOrReplace(): Boolean {
        val index = index.get() ?: return false
        val size = items.get()?.size ?: 0
        return index <= if (replace.get()) size - 1 else size
    }

    @Bindable
    fun getCanClear() = items.get()?.size ?: 0 > 0

    private fun resetSelection() {
        val items = items.get() ?: return
        Selection.release(items)
        val behavior = (selectionBehavior.get() as? SelectionBehaviorItem)?.selectionBehavior
        selection = if (behavior == null) null else Selection.apply(items, behavior)
    }

    private fun createPersonItem() = PersonItem.createNew(::remove)
    private fun createOrganizationItem() = OrganizationItem.createNew(::remove)
    private fun createSeparatorItem() = SeparatorItem.createNew()
    private fun createItems(): ObservableList<Any> {
        val list = ObservableArrayList<Any>()
        list.addAll((1..5).map { createPersonItem() })
        return list
    }

    private fun getDefaultIndex(): Int? {
        val size = items.get()?.size ?: 0
        return if (replace.get())
            if (size == 0) null else 0
        else
            size
    }

    private fun updateIndex() = this.index.set(getDefaultIndex())

    private fun onListUpdated() {
        updateIndex()
        registry.notifyChange(this, BR.canClear)
    }

    fun refresh() = items.set(createItems())
    fun clear() = items.set(null)

    private fun addOrReplaceItem(item: Any) {
        fun create(): ObservableArrayList<Any> {
            val result = ObservableArrayList<Any>()
            this.items.set(result)
            return result
        }

        val items = this.items.get() ?: create()
        val index = index.get()!!
        if (replace.get()) items[index] = item
        else items.add(index, item)
    }

    fun insertOrReplacePerson() = addOrReplaceItem(createPersonItem())
    fun insertOrReplaceOrganization() = addOrReplaceItem(createOrganizationItem())
    fun insertOrReplaceSeparator() = addOrReplaceItem(createSeparatorItem())

    private fun remove(item: Any) {
        items.get()?.remove(item)
    }

    /*Observable*/
    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) =
            registry.add(callback)

    override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) =
            registry.remove(callback)

    private class OnListChangedCallback(viewModel: ListViewModel) :
            ObservableList.OnListChangedCallback<ObservableList<Any>>() {
        private val viewModelReference = WeakReference<ListViewModel>(viewModel)

        override fun onChanged(sender: ObservableList<Any>) {
            viewModelReference.get()?.onListUpdated()
        }

        override fun onItemRangeRemoved(sender: ObservableList<Any>, positionStart: Int, itemCount: Int) {
            viewModelReference.get()?.onListUpdated()
        }

        override fun onItemRangeMoved(sender: ObservableList<Any>, fromPosition: Int, toPosition: Int, itemCount: Int) {
            viewModelReference.get()?.onListUpdated()
        }

        override fun onItemRangeInserted(sender: ObservableList<Any>, positionStart: Int, itemCount: Int) {
            viewModelReference.get()?.onListUpdated()
        }

        override fun onItemRangeChanged(sender: ObservableList<Any>, positionStart: Int, itemCount: Int) {
            viewModelReference.get()?.onListUpdated()
        }
    }

    private class SelectionBehaviorItem(val selectionBehavior: Selection.BehaviorType?) {
        override fun toString() = selectionBehavior?.toString() ?: "Behavior"
    }
}
