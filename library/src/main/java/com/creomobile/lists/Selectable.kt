package com.creomobile.lists

import android.arch.lifecycle.ViewModel
import android.databinding.ObservableBoolean

open class Selectable : ViewModel() {
    var selected = ObservableBoolean(false)

    fun select() = selected.set(true)
    fun switch() = selected.set(!selected.get())
}
