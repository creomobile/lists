package com.creomobile.lists

import android.arch.lifecycle.ViewModel
import android.databinding.ObservableBoolean

open class Selectable : ViewModel() {
    val selected = object : ObservableBoolean(false) {
        override fun set(value: Boolean) {
            controller?.apply {
                if (value == get() ||
                        (value && !canSelect(this@Selectable)) ||
                        (!value && !canDeselect(this@Selectable))) return@set
            }

            super.set(value)

            controller?.apply {
                if (value)
                    onSelect(this@Selectable)
                else
                    onDeselect(this@Selectable)
            }

            onSelectedChanged()
        }
    }

    fun select() = selected.set(true)
    fun switch() = selected.set(!selected.get())

    internal fun cleanControllerAndDeselect() {
        this.controller = null
        selected.set(false)
    }

    internal fun forceDeselectWithoutEvent() {
        val controller = this.controller
        cleanControllerAndDeselect()
        this.controller = controller
    }

    fun forceDeselect() {
        forceDeselectWithoutEvent()
        controller?.onDeselect(this)
    }

    protected open fun onSelectedChanged() = Unit

    internal var controller: SelectionController? = null
}
