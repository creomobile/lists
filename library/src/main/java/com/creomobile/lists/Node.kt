package com.creomobile.lists

import android.databinding.ObservableBoolean
import android.databinding.ObservableField

interface Node<T: Any> {
    val items: ObservableField<List<T>?>
    val expanded: ObservableBoolean
}
