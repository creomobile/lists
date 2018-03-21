package com.creomobile.lists

import android.databinding.BindingAdapter
import android.databinding.InverseBindingAdapter
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView

@BindingAdapter("android:items")
fun setItems(view: RecyclerView, items: List<Any>?) {
    if (view.getTag(R.id.items) == items) return
    view.setTag(R.id.items, items)
    (view.adapter as? RecycleListAdapter)?.items = items
}

@BindingAdapter("android:onItemClick")
fun bindOnItemClick(view: RecyclerView, listener: ItemClickListener) =
        (view.adapter as? RecycleListAdapter)?.setOnItemClickListener(listener)

@BindingAdapter("android:items")
fun bindItems(view: Spinner, items: Array<Any>?) {
    bindItems(view, items?.toList())
}

@Suppress("UNCHECKED_CAST")
private fun setSelectedItem(view: Spinner, item: Any?) {
    val adapter = view.adapter as? ArrayAdapter<Any> ?: return
    val index = adapter.getPosition(item)
    view.setSelection(index)
}

@BindingAdapter("android:items")
fun bindItems(view: Spinner, items: List<Any>?) {
    val adapter =
            if (items == null) null
            else ArrayAdapter(view.context, android.R.layout.simple_spinner_item, items)
    view.adapter = adapter
    val selectedItem = view.getTag(R.id.selected_item)
    if (selectedItem != null)
        setSelectedItem(view, items)
}

@BindingAdapter("android:selectedItem")
fun bindSelectedItem(view: Spinner, item: Any?) {
    if (view.selectedItem == item) return
    view.onItemSelectedListener
    view.setTag(R.id.selected_item, item)
    setSelectedItem(view, item)
}

@InverseBindingAdapter(
        attribute = "android:selectedItem",
        event = "android:selectedItemPositionAttrChanged")
fun bindSelectedItem(view: Spinner): Any? = view.selectedItem

@BindingAdapter("android:selectable")
fun bindSelectable(view: SelectableFrameLayout, selectable: Selectable) {
    view.selectable = selectable
}

private fun setIgnoreTag(view: View, value: Boolean) =
        view.setTag(R.id.ignore_selection, if (value) true else null)

@BindingAdapter("android:ignoreSelection")
fun bindIgnoreSelection(view: TextView, value: Boolean) = setIgnoreTag(view, value)

@BindingAdapter("android:ignoreSelection")
fun bindIgnoreSelection(view: ImageView, value: Boolean) = setIgnoreTag(view, value)
