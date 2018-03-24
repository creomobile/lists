package com.creomobile.lists

import android.content.res.Resources
import android.databinding.DataBindingUtil
import android.databinding.Observable
import android.databinding.ObservableList
import android.databinding.ViewDataBinding
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import java.lang.ref.WeakReference

abstract class RecycleAdapter protected constructor(
        private val viewMap: HashMap<Class<out Any>, ViewInfo>,
        private val scrollToInserted: Boolean) :
        RecyclerView.Adapter<RecycleAdapter.ViewHolder>(), View.OnClickListener {

    private val listChangedCallback = OnListChangedCallback(this)
    private var clickListener: ItemClickListener? = null
    private var inflater: LayoutInflater? = null
    private var recyclerView: RecyclerView? = null
    var items: List<Any>? = null
        set(value) {
            releaseItems()
            initItems(value)
            field = value
            notifyDataSetChanged()
        }

    private fun scrollToPosition(position: Int) {
        val recyclerView = recyclerView
        if (recyclerView?.scrollState == RecyclerView.SCROLL_STATE_IDLE)
            recyclerView.scrollToPosition(position)
    }

    fun setOnItemClickListener(listener: ItemClickListener?) {
        clickListener = listener
    }

    protected open fun initItems(items: List<Any>?) {
        (items as? ObservableList<Any>)?.addOnListChangedCallback(listChangedCallback)
    }

    protected open fun releaseItems() {
        (items as? ObservableList<Any>)?.removeOnListChangedCallback(listChangedCallback)
    }

    protected open fun onItemsChanged() = notifyDataSetChanged()

    protected open fun onItemsChanged(positionStart: Int, itemCount: Int) =
            notifyItemRangeChanged(positionStart, itemCount)

    protected open fun onItemsInserted(positionStart: Int, itemCount: Int) {
        notifyItemRangeInserted(positionStart, itemCount)
        if (scrollToInserted) {
            val size = items!!.size
            scrollToPosition(
                    if (itemCount == 1 || positionStart + itemCount != size) positionStart
                    else size - 1)
        }
    }

    protected open fun onItemsRemoved(positionStart: Int, itemCount: Int) =
            notifyItemRangeRemoved(positionStart, itemCount)

    protected open fun onItemsMoved(fromPosition: Int, toPosition: Int, itemCount: Int) =
            notifyItemMoved(fromPosition, toPosition)

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        releaseItems()
    }

    protected abstract fun getItem(position: Int): Any

    private fun getViewInfo(item: Any): ViewInfo = viewMap[item.javaClass]
            ?: throw IllegalStateException(
                    "View descriptor for the '${item.javaClass}' class is not found.")

    override fun getItemViewType(position: Int) = getViewInfo(getItem(position)).layoutId

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val info = getViewInfo(item)
        val binding = holder.binding
        if (info.bindingId != null && binding != null) {
            binding.setVariable(info.bindingId, item)
            binding.root.setTag(R.id.view_model, item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (inflater == null)
            inflater = LayoutInflater.from(parent.context)
        val view = inflater!!.inflate(viewType, parent, false)
        view.setOnClickListener(this)
        return ViewHolder(view)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onClick(view: View?) {
        val item = view?.getTag(R.id.view_model) ?: return
        clickListener?.onClick(view, item)
    }

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val binding: ViewDataBinding? = DataBindingUtil.bind(view)
    }

    private class OnListChangedCallback(adapter: RecycleAdapter) :
            ObservableList.OnListChangedCallback<ObservableList<Any>>() {
        private val adapterReference = WeakReference<RecycleAdapter>(adapter)

        override fun onChanged(sender: ObservableList<Any>) {
            adapterReference.get()?.onItemsChanged()
        }

        override fun onItemRangeRemoved(sender: ObservableList<Any>, positionStart: Int, itemCount: Int) {
            adapterReference.get()?.onItemsRemoved(positionStart, itemCount)
        }

        override fun onItemRangeMoved(sender: ObservableList<Any>, fromPosition: Int, toPosition: Int, itemCount: Int) {
            adapterReference.get()?.onItemsMoved(fromPosition, toPosition, itemCount)
        }

        override fun onItemRangeInserted(sender: ObservableList<Any>, positionStart: Int, itemCount: Int) {
            adapterReference.get()?.onItemsInserted(positionStart, itemCount)
        }

        override fun onItemRangeChanged(sender: ObservableList<Any>, positionStart: Int, itemCount: Int) {
            adapterReference.get()?.onItemsChanged(positionStart, itemCount)
        }
    }

    protected class ViewInfo(val layoutId: Int, val bindingId: Int?)

    protected class ViewMapHolder {
        val viewMap = HashMap<Class<out Any>, ViewInfo>(1)

        fun addView(clazz: Class<out Any>, layoutId: Int, bindingId: Int?) {
            if (viewMap.containsKey(clazz))
                throw IllegalArgumentException("Class '$clazz' is already added")

            viewMap[clazz] = ViewInfo(layoutId, bindingId)
        }

        fun checkViewMap() {
            if (viewMap.size == 0)
                throw IllegalStateException("There are no item views added")
        }
    }
}

class RecycleExpandableAdapter private constructor(
        viewMap: HashMap<Class<out Any>, ViewInfo>,
        scrollToInserted: Boolean,
        private val childrenMarginStart: Int)
    : RecycleAdapter(viewMap, scrollToInserted), View.OnClickListener {

    private val nodeSubscriber = NodeSubscriber(this)
    private var viewItemsCache: List<ItemDescriptor>? = null
    private val viewItems: List<ItemDescriptor>
        get() {
            var cache = viewItemsCache
            if (cache == null) {
                cache = items?.flatMap {
                    if (it is Node<*> && it.expanded.get())
                        listOf(ItemDescriptor(it)) + it.items.get().orEmpty().map { ItemDescriptor(it, true) }
                    else
                        listOf(ItemDescriptor(it))
                }.orEmpty()
                viewItemsCache = cache
            }
            return cache
        }
    private var previousViewSize = 0

    @Suppress("NOTHING_TO_INLINE")
    private inline fun clearCache() {
        viewItemsCache = null
    }

    private fun subscribeItems(items: List<Any>?) =
            items?.filterIsInstance<Node<Any>>()?.forEach { nodeSubscriber.subscribe(it) }

    override fun initItems(items: List<Any>?) {
        clearCache()
        super.initItems(items)
        subscribeItems(items)
    }

    override fun releaseItems() {
        super.releaseItems()
        nodeSubscriber.unSubscribeAll()
    }

    override fun getItem(position: Int) = viewItems[position].item
    override fun getItemCount(): Int {
        val size = viewItems.size
        previousViewSize = size
        return size
    }

    private fun getNodeViewPosition(listPosition: Int) =
            items!!.take(listPosition).sumBy {
                if (it is Node<*> && it.expanded.get()) (it.items.get()?.size ?: 0) + 1
                else 1
            }

    private fun getSubItemsViewPosition(node: Node<out Any>) =
            getNodeViewPosition(items!!.indexOf(node)) + 1

    private fun unSubscribeRemoved() =
            nodeSubscriber.unSubscribeRemoved(items?.filterIsInstance<Node<Any>>().orEmpty())

    private fun getItemsRange(positionStart: Int, itemCount: Int) =
            items!!.drop(positionStart).take(itemCount)

    private fun getExpandedItemsSize(items: List<Any>) = items.sumBy {
        if (it is Node<*> && it.expanded.get()) (it.items.get()?.size ?: 0) + 1
        else 1
    }

    override fun onItemsChanged() {
        clearCache()
        nodeSubscriber.unSubscribeAll()
        subscribeItems(items)
        super.onItemsChanged()
    }

    override fun onItemsChanged(positionStart: Int, itemCount: Int) {
        clearCache()
        unSubscribeRemoved()
        val items = getItemsRange(positionStart, itemCount)
        subscribeItems(items)

        val size = getExpandedItemsSize(items)
        val previousSize = size + previousViewSize - viewItems.size
        val viewPosition = getNodeViewPosition(positionStart)

        when {
            size > previousSize -> {
                super.onItemsChanged(viewPosition, previousSize)
                super.onItemsInserted(viewPosition + previousSize, size - previousSize)
            }
            size < previousSize -> {
                super.onItemsChanged(viewPosition, size)
                super.onItemsRemoved(viewPosition + size, previousSize - size)
            }
            else -> super.onItemsChanged(viewPosition, size)
        }
    }

    override fun onItemsInserted(positionStart: Int, itemCount: Int) {
        clearCache()
        val items = getItemsRange(positionStart, itemCount)
        subscribeItems(items)
        super.onItemsInserted(getNodeViewPosition(positionStart), getExpandedItemsSize(items))
    }

    override fun onItemsRemoved(positionStart: Int, itemCount: Int) {
        clearCache()
        unSubscribeRemoved()
        super.onItemsRemoved(getNodeViewPosition(positionStart), previousViewSize - viewItems.size)
    }

    override fun onItemsMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
        clearCache()
        super.onItemsMoved(getNodeViewPosition(fromPosition), getNodeViewPosition(toPosition),
                previousViewSize - viewItems.size)
    }

    private fun onSubItemsChanged(node: Node<out Any>, previousSize: Int) {
        if (!node.expanded.get()) return
        val size = node.items.get()?.size ?: 0
        if (size == 0 && previousSize == 0) return
        clearCache()
        val viewPosition = getSubItemsViewPosition(node)

        when {
            size > previousSize -> {
                super.onItemsChanged(viewPosition, previousSize)
                super.onItemsInserted(viewPosition + previousSize, size - previousSize)
            }
            size < previousSize -> {
                super.onItemsChanged(viewPosition, size)
                super.onItemsRemoved(viewPosition + size, previousSize - size)
            }
            else -> super.onItemsChanged(viewPosition, size)
        }
    }

    private fun onSubItemsChanged(node: Node<out Any>, positionStart: Int, itemCount: Int) {
        if (!node.expanded.get()) return
        clearCache()
        super.onItemsChanged(getSubItemsViewPosition(node) + positionStart, itemCount)
    }

    private fun onSubItemsInserted(node: Node<out Any>, positionStart: Int, itemCount: Int) {
        if (!node.expanded.get()) return
        clearCache()
        super.onItemsInserted(getSubItemsViewPosition(node) + positionStart, itemCount)
    }

    private fun onSubItemsRemoved(node: Node<out Any>, positionStart: Int, itemCount: Int) {
        if (!node.expanded.get()) return
        clearCache()
        super.onItemsRemoved(getSubItemsViewPosition(node) + positionStart, itemCount)
    }

    private fun onSubItemsMoved(node: Node<out Any>, fromPosition: Int, toPosition: Int, itemCount: Int) {
        if (!node.expanded.get()) return
        clearCache()
        val viewPosition = getSubItemsViewPosition(node)
        super.onItemsMoved(viewPosition + fromPosition,
                viewPosition + toPosition, itemCount)
    }

    private fun onNodeExpandedChanged(node: Node<out Any>) {
        val size = node.items.get()?.size ?: 0
        if (size == 0) return
        clearCache()
        val viewPosition = getSubItemsViewPosition(node)
        if (node.expanded.get())
            super.onItemsInserted(viewPosition, size)
        else
            super.onItemsRemoved(viewPosition, size)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        if (childrenMarginStart == 0 || !viewItems[position].isChild) return

        val view = holder.view
        val previous = view.layoutParams
        view.setTag(R.id.previous_layout_params, previous)
        val layoutParams = ViewGroup.MarginLayoutParams(previous)
        if (previous is ViewGroup.MarginLayoutParams) {
            layoutParams.topMargin = previous.topMargin
            layoutParams.bottomMargin = previous.bottomMargin
            layoutParams.leftMargin = previous.leftMargin
            layoutParams.rightMargin = previous.rightMargin
        }
        layoutParams.leftMargin += childrenMarginStart
        view.layoutParams = layoutParams
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)

        val view = holder.view
        val previousLayoutParams =
                view.getTag(R.id.previous_layout_params) as? ViewGroup.LayoutParams ?: return
        view.layoutParams = previousLayoutParams
    }

    private class ItemDescriptor(val item: Any, val isChild: Boolean = false)

    private class NodeSubscriber(private val adapter: RecycleExpandableAdapter) {

        private val subscriptionMap = HashMap<Node<out Any>, Subscription>(16)

        fun subscribe(node: Node<out Any>) {
            subscriptionMap[node] = Subscription(this, adapter, node)
        }

        fun unSubscribe(node: Node<out Any>) {
            val subscription = subscriptionMap[node] ?: throw IndexOutOfBoundsException()
            subscription.unSubscribe()
            subscriptionMap.remove(node)
        }

        fun unSubscribeAll() {
            subscriptionMap.values.forEach { it.unSubscribe() }
            subscriptionMap.clear()
        }

        fun unSubscribeRemoved(actualNodes: Collection<Node<out Any>>) {
            val hashSet = HashSet(actualNodes)
            subscriptionMap.values
                    .filter { !hashSet.contains(it.node) }
                    .map { it.node }
                    .forEach(::unSubscribe)
        }

        private class Subscription(
                private val subscriber: NodeSubscriber,
                private val adapter: RecycleExpandableAdapter,
                val node: Node<out Any>) {

            var previousSize: Int
            private fun updatePreviousSize() {
                previousSize = node.items.get()?.size ?: 0
            }

            init {
                previousSize = node.items.get()?.size ?: 0
            }

            private val itemsChangedCallback = object : Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable?, propertyId: Int) = onItemsChanged()
            }
            private val expandedChangedCallback = object : Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable?, propertyId: Int) =
                        adapter.onNodeExpandedChanged(node)
            }
            private var previousObservableList: ObservableList<Any>? = null
            private var listChangedCallback = OnListChangedCallback(this, adapter, node)

            private fun onItemsChanged() {
                previousObservableList?.removeOnListChangedCallback(listChangedCallback)
                previousObservableList = node.items.get() as? ObservableList<Any>
                previousObservableList?.addOnListChangedCallback(listChangedCallback)
                adapter.onSubItemsChanged(node, previousSize)
                updatePreviousSize()
            }

            init {
                node.items.addOnPropertyChangedCallback(itemsChangedCallback)
                node.expanded.addOnPropertyChangedCallback(expandedChangedCallback)
                onItemsChanged()
            }

            fun unSubscribe() {
                node.items.removeOnPropertyChangedCallback(itemsChangedCallback)
                node.expanded.removeOnPropertyChangedCallback(expandedChangedCallback)
                previousObservableList?.removeOnListChangedCallback(listChangedCallback)
            }

            private class OnListChangedCallback(
                    private val subscription: Subscription,
                    private val adapter: RecycleExpandableAdapter,
                    val node: Node<out Any>) :
                    ObservableList.OnListChangedCallback<ObservableList<Any>>() {

                override fun onChanged(sender: ObservableList<Any>) {
                    adapter.onSubItemsChanged(node, subscription.previousSize)
                    subscription.updatePreviousSize()
                }

                override fun onItemRangeRemoved(sender: ObservableList<Any>, positionStart: Int,
                                                itemCount: Int) {
                    adapter.onSubItemsRemoved(node, positionStart, itemCount)
                    subscription.updatePreviousSize()
                }

                override fun onItemRangeMoved(sender: ObservableList<Any>, fromPosition: Int,
                                              toPosition: Int, itemCount: Int) =
                        adapter.onSubItemsMoved(node, fromPosition, toPosition, itemCount)

                override fun onItemRangeInserted(sender: ObservableList<Any>, positionStart: Int,
                                                 itemCount: Int) {
                    adapter.onSubItemsInserted(node, positionStart, itemCount)
                    subscription.updatePreviousSize()
                }

                override fun onItemRangeChanged(sender: ObservableList<Any>, positionStart: Int,
                                                itemCount: Int) {
                    adapter.onSubItemsChanged(node, positionStart, itemCount)
                    subscription.updatePreviousSize()
                }
            }
        }
    }

    class Builder {
        private val viewMapHolder = ViewMapHolder()
        private var scrollToInserted: Boolean = false
        private var childrenMarginStart = 0

        fun addView(clazz: Class<out Any>, layoutId: Int, bindingId: Int? = null): Builder {
            viewMapHolder.addView(clazz, layoutId, bindingId)
            return this
        }

        inline fun <reified T : Any> addView(layoutId: Int, bindingId: Int? = null) =
                addView(T::class.java, layoutId, bindingId)

        fun scrollToInserted(): Builder {
            scrollToInserted = true
            return this
        }

        fun withChildrenMarginStart(marginStart: Int = 24): Builder {
            this.childrenMarginStart = Math.round(
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, marginStart.toFloat(),
                            Resources.getSystem().displayMetrics))
            return this
        }

        fun build(): RecycleExpandableAdapter {
            viewMapHolder.checkViewMap()
            return RecycleExpandableAdapter(viewMapHolder.viewMap, scrollToInserted, childrenMarginStart)
        }
    }
}

class RecycleListAdapter private constructor(
        viewMap: HashMap<Class<out Any>, ViewInfo>, scrollToInserted: Boolean) :
        RecycleAdapter(viewMap, scrollToInserted), View.OnClickListener {

    override fun getItem(position: Int) = items?.get(position) ?: throw IndexOutOfBoundsException()
    override fun getItemCount() = items?.size ?: 0

    class Builder {
        private val viewMapHolder = ViewMapHolder()
        private var scrollToInserted: Boolean = false

        fun addView(clazz: Class<out Any>, layoutId: Int, bindingId: Int? = null): Builder {
            viewMapHolder.addView(clazz, layoutId, bindingId)
            return this
        }

        inline fun <reified T : Any> addView(layoutId: Int, bindingId: Int? = null) =
                addView(T::class.java, layoutId, bindingId)

        fun scrollToInserted(): Builder {
            scrollToInserted = true
            return this
        }

        fun build(): RecycleListAdapter {
            viewMapHolder.checkViewMap()
            return RecycleListAdapter(viewMapHolder.viewMap, scrollToInserted)
        }
    }
}

