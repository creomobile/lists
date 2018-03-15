package com.creomobile.lists

import android.databinding.DataBindingUtil
import android.databinding.ObservableList
import android.databinding.ViewDataBinding
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import java.lang.ref.WeakReference

class ListAdapter private constructor(
        private val viewMap: HashMap<Class<out Any>, ViewInfo>,
        private val clickListener: OnClickListener<Any>?) :
        RecyclerView.Adapter<ListAdapter.ViewHolder>(), View.OnClickListener {

    private var inflater: LayoutInflater? = null
    private val listChangedCallback = OnListChangedCallback(this)

    var items: List<Any>? = null
        set(value) {
            initItems(field, value)
            field = value
        }

    private fun initItems(old: List<Any>?, new: List<Any>?) {
        (old as? ObservableList)?.removeOnListChangedCallback(listChangedCallback)
        (new as? ObservableList)?.addOnListChangedCallback(listChangedCallback)
        notifyDataSetChanged()
    }

    private fun getItem(position: Int) = items?.get(position) ?: throw IndexOutOfBoundsException()

    private fun getViewInfo(item: Any): ViewInfo = viewMap[item.javaClass]
            ?: throw IllegalStateException(
                    "View descriptor for the '${item.javaClass}' class is not found.")

    override fun getItemCount() = items?.size ?: 0

    override fun getItemViewType(position: Int) = getViewInfo(getItem(position)).layoutId

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (inflater == null)
            inflater = LayoutInflater.from(parent.context)
        val view = inflater!!.inflate(viewType, parent, false)
        view.setOnClickListener(this)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val info = getViewInfo(item)
        val binding = holder.binding
        if (info.bindingId != null && binding != null) {
            binding.setVariable(info.bindingId, item)
            binding.root.tag = item
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        (items as? ObservableList)?.removeOnListChangedCallback(listChangedCallback)
    }

    override fun onClick(view: View?) {
        val item = view?.tag ?: return
        when (item) {
            is Selectable -> item.switch()
            is View.OnClickListener -> item.onClick(view)
        }
        clickListener?.onClick(item)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding: ViewDataBinding? = DataBindingUtil.bind(view)
    }

    private class OnListChangedCallback(adapter: ListAdapter) :
            ObservableList.OnListChangedCallback<ObservableList<Any>>() {

        private val adapterReference = WeakReference<ListAdapter>(adapter)

        override fun onChanged(sender: ObservableList<Any>?) {
            adapterReference.get()?.notifyDataSetChanged()
        }

        override fun onItemRangeRemoved(sender: ObservableList<Any>?, positionStart: Int, itemCount: Int) {
            adapterReference.get()?.notifyItemRangeRemoved(positionStart, itemCount)
        }

        override fun onItemRangeMoved(sender: ObservableList<Any>?, fromPosition: Int, toPosition: Int, itemCount: Int) {
            adapterReference.get()?.notifyItemMoved(fromPosition, toPosition)
        }

        override fun onItemRangeInserted(sender: ObservableList<Any>?, positionStart: Int, itemCount: Int) {
            adapterReference.get()?.notifyItemRangeInserted(positionStart, itemCount)
        }

        override fun onItemRangeChanged(sender: ObservableList<Any>?, positionStart: Int, itemCount: Int) {
            adapterReference.get()?.notifyItemRangeChanged(positionStart, itemCount)
        }
    }

    private class ViewInfo(val layoutId: Int, val bindingId: Int?)

    interface OnClickListener<in T> {
        fun onClick(item: T)
    }

    class Builder() {
        private val viewMap = HashMap<Class<out Any>, ViewInfo>(1)
        private var clickListener: OnClickListener<Any>? = null

        fun addView(clazz: Class<out Any>, layoutId: Int, bindingId: Int? = null): Builder {
            if (viewMap.containsKey(clazz))
                throw IllegalArgumentException("Class '$clazz' is already added")
            viewMap[clazz] = ViewInfo(layoutId, bindingId)
            return this
        }

        fun setCommonClickListener(listener: OnClickListener<Any>) {
            clickListener = listener
        }

        fun setCommonClickListener(listener: (item: Any) -> Unit) {
            setCommonClickListener(object : OnClickListener<Any> {
                override fun onClick(item: Any) = listener(item)
            })
        }

        inline fun <reified T> setClickListener(listener: OnClickListener<T>) {
            setCommonClickListener({ item ->
                if (item is T)
                    listener.onClick(item)
            })
        }

        inline fun <reified T> setClickListener(crossinline listener: (item: T) -> Unit) {
            setCommonClickListener({ item ->
                if (item is T)
                    listener(item)
            })
        }

        fun build() = ListAdapter(viewMap, clickListener)
    }
}

