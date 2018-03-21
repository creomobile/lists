package com.creomobile.lists

import android.databinding.DataBindingUtil
import android.databinding.ObservableList
import android.databinding.ViewDataBinding
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import java.lang.ref.WeakReference

class RecycleListAdapter private constructor(
        private val viewMap: HashMap<Class<out Any>, ViewInfo>) :
        RecyclerView.Adapter<RecycleListAdapter.ViewHolder>(), View.OnClickListener {

    private var inflater: LayoutInflater? = null
    private val listChangedCallback = OnListChangedCallback(this)
    private var clickListener: ItemClickListener? = null

    var items: List<Any>? = null
        set(value) {
            initItems(field, value)
            field = value
        }

    fun setOnItemClickListener(listener: ItemClickListener?) {
        clickListener = listener
    }

    fun setOnItemClickListener(listener: (view: View, viewModel: Any) -> Unit) {
        setOnItemClickListener(object : ItemClickListener {
            override fun onClick(view: View, viewModel: Any) = listener(view, viewModel)
        })
    }

    fun setOnItemClickListener(listener: (viewModel: Any) -> Unit) {
        setOnItemClickListener(object : ItemClickListener {
            override fun onClick(view: View, viewModel: Any) = listener(viewModel)
        })
    }

    private fun initItems(old: List<Any>?, new: List<Any>?) {
        (old as? ObservableList<Any>)?.removeOnListChangedCallback(listChangedCallback)
        (new as? ObservableList<Any>)?.addOnListChangedCallback(listChangedCallback)
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
            binding.root.setTag(R.id.view_model, item)
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        (items as? ObservableList<Any>)?.removeOnListChangedCallback(listChangedCallback)
    }

    override fun onClick(view: View?) {
        val item = view?.getTag(R.id.view_model) ?: return
        (item as? Selectable)?.switch()
        clickListener?.onClick(view, item)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding: ViewDataBinding? = DataBindingUtil.bind(view)
    }

    private class OnListChangedCallback(adapter: RecycleListAdapter) :
            ObservableList.OnListChangedCallback<ObservableList<Any>>() {
        private val adapterReference = WeakReference<RecycleListAdapter>(adapter)

        override fun onChanged(sender: ObservableList<Any>) {
            adapterReference.get()?.notifyDataSetChanged()
        }

        override fun onItemRangeRemoved(sender: ObservableList<Any>, positionStart: Int, itemCount: Int) {
            adapterReference.get()?.notifyItemRangeRemoved(positionStart, itemCount)
        }

        override fun onItemRangeMoved(sender: ObservableList<Any>, fromPosition: Int, toPosition: Int, itemCount: Int) {
            adapterReference.get()?.notifyItemMoved(fromPosition, toPosition)
        }

        override fun onItemRangeInserted(sender: ObservableList<Any>, positionStart: Int, itemCount: Int) {
            adapterReference.get()?.notifyItemRangeInserted(positionStart, itemCount)
        }

        override fun onItemRangeChanged(sender: ObservableList<Any>, positionStart: Int, itemCount: Int) {
            adapterReference.get()?.notifyItemRangeChanged(positionStart, itemCount)
        }
    }

    private class ViewInfo(val layoutId: Int, val bindingId: Int?)

    class Builder {
        private val viewMap = HashMap<Class<out Any>, ViewInfo>(1)

        fun addView(clazz: Class<out Any>, layoutId: Int, bindingId: Int? = null): Builder {
            if (viewMap.containsKey(clazz))
                throw IllegalArgumentException("Class '$clazz' is already added")
            viewMap[clazz] = ViewInfo(layoutId, bindingId)
            return this
        }

        inline fun <reified T : Any> addView(layoutId: Int, bindingId: Int? = null) =
                addView(T::class.java, layoutId, bindingId)

        fun build(): RecycleListAdapter {
            if (viewMap.size == 0)
                throw IllegalStateException("There are no item views added")
            return RecycleListAdapter(viewMap)
        }
    }
}

