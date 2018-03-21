package com.creomobile.lists.sample

import android.databinding.BindingAdapter
import android.databinding.InverseBindingAdapter
import android.widget.TextView

@BindingAdapter("android:text")
fun setText(view: TextView, value: Int?) {
    val s = value?.toString()
    if (view.text.toString() != s)
        view.text = s
}

@InverseBindingAdapter(attribute = "android:text")
fun getText(view: TextView): Int? = view.text.toString().toIntOrNull()


