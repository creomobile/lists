package com.creomobile.lists.sample

import android.arch.lifecycle.ViewModel
import android.databinding.*
import java.lang.ref.WeakReference

abstract class ViewModelBase : ViewModel(), Observable {

    private val listChangedCallback = OnListChangedCallback(this)
    private val registry = PropertyChangeRegistry()
    val items = object : ObservableField<ObservableList<Any>>() {
        override fun set(value: ObservableList<Any>?) {
            super.get()?.removeOnListChangedCallback(listChangedCallback)
            super.set(value)
            initList()
        }
    }
    val index = object : ObservableField<Int?>() {
        override fun set(value: Int?) {
            super.set(value)
            notifyChange(BR.canAddOrReplace)
        }
    }
    val replace = object : ObservableBoolean() {
        override fun set(value: Boolean) {
            val isDefaultIndex = index.get() == getDefaultIndex()
            super.set(value)
            if (isDefaultIndex) updateIndex()
            notifyChange(BR.canAddOrReplace)
        }
    }

    @Bindable
    open fun getCanClear() = items.get()?.size ?: 0 > 0

    @Bindable
    open fun getCanAddOrReplace(): Boolean = getCanAddOrReplace(items.get()?.size ?: 0)

    protected fun getCanAddOrReplace(listSize: Int): Boolean {
        val index = index.get() ?: return false
        return index <= if (replace.get()) listSize - 1 else listSize
    }

    protected fun notifyChange(propertyId: Int) = registry.notifyChange(this, propertyId)

    protected open fun initList() {
        items.get()?.addOnListChangedCallback(listChangedCallback)
        onListUpdated()
    }

    protected fun onListUpdated() {
        updateIndex()
        notifyChange(BR.canClear)
    }

    protected fun getDefaultIndex(listSize: Int) =
            if (replace.get()) (if (listSize == 0) null else 0) else listSize

    protected open fun getDefaultIndex(): Int? = getDefaultIndex(items.get()?.size ?: 0)


    protected fun updateIndex() = this.index.set(getDefaultIndex())

    protected fun createPersonItem() = PersonItem.createNew(::remove)
    protected fun createOrganizationItem() = OrganizationItem.createNew(::remove)
    protected fun createSeparatorItem() = SeparatorItem.createNew()

    protected abstract fun createItems(): ObservableList<Any>

    open fun remove(item: Any) {
        items.get()?.remove(item)
    }

    open fun refresh() = items.set(createItems())
    open fun clear() = items.set(null)

    protected open fun addOrReplaceItem(item: Any) {
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

    open fun insertOrReplacePerson() = addOrReplaceItem(createPersonItem())
    open fun insertOrReplaceOrganization() = addOrReplaceItem(createOrganizationItem())
    open fun insertOrReplaceSeparator() = addOrReplaceItem(createSeparatorItem())

    /*Observable*/
    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) =
            registry.add(callback)

    override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) =
            registry.remove(callback)

    private class OnListChangedCallback(viewModel: ViewModelBase) :
            ObservableList.OnListChangedCallback<ObservableList<Any>>() {
        private val viewModelReference = WeakReference<ViewModelBase>(viewModel)

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
}
