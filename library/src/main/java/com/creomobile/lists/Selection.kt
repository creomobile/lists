package com.creomobile.lists

import android.databinding.ObservableList
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import java.lang.ref.WeakReference

interface Selection<T : Selectable> {
    val selectedCount: Observable<Int>
    val currentSelectedCount: Int
    val selectedItems: Observable<Sequence<T>>
    val currentSelectedItems: Sequence<T>

    companion object {
        fun <T : Selectable> apply(items: Iterable<Any>, behaviorType: BehaviorType,
                                   itemClass: Class<*>): Selection<T> = when (behaviorType) {

            BehaviorType.Multi -> MultiSelectionBehavior(itemClass, items)
            BehaviorType.OneOrMore -> OneOrMoreSelectionBehavior(itemClass, false, items)
            BehaviorType.OneOrMoreInit -> OneOrMoreSelectionBehavior(itemClass, true, items)
            BehaviorType.SingleOrNone -> SingleSelection.apply(items, SingleSelection.BehaviorType.SingleOrNone, itemClass)
            BehaviorType.SingleOrNoneInit -> SingleSelection.apply(items, SingleSelection.BehaviorType.SingleOrNoneInit, itemClass)
            BehaviorType.Single -> SingleSelection.apply(items, SingleSelection.BehaviorType.Single, itemClass)
            BehaviorType.SingleInit -> SingleSelection.apply(items, SingleSelection.BehaviorType.SingleInit, itemClass)
        }

        inline fun <reified T : Selectable> apply(items: Iterable<Any>, behaviorType: BehaviorType) =
                apply<T>(items, behaviorType, T::class.java)

        fun release(items: Iterable<Any>) {
            var disconnected = false
            items.filterIsInstance<Selectable>().forEach {
                val controller = it.controller as? SelectionBehavior<*>
                if (controller != null) {
                    if (!disconnected) {
                        controller.disconnect()
                        disconnected = true
                    }
                    it.controller = null
                }
            }
        }
    }

    enum class BehaviorType {
        Multi,
        OneOrMore,
        OneOrMoreInit,
        SingleOrNone,
        SingleOrNoneInit,
        Single,
        SingleInit
    }
}

interface SingleSelection<T : Selectable> : Selection<T> {
    val selectedItem: Observable<Optional<T>>
    val currentSelectedItem: T?

    companion object {
        fun <T : Selectable> apply(items: Iterable<Any>, behaviorType: BehaviorType,
                                   itemClass: Class<*>): SingleSelection<T> = when (behaviorType) {
            BehaviorType.SingleOrNone -> SingleSelectionBehavior(itemClass, false, false, items)
            BehaviorType.SingleOrNoneInit -> SingleSelectionBehavior(itemClass, true, false, items)
            BehaviorType.Single -> SingleSelectionBehavior(itemClass, false, true, items)
            BehaviorType.SingleInit -> SingleSelectionBehavior(itemClass, true, true, items)
        }

        inline fun <reified T : Selectable> apply(items: Iterable<Any>, behaviorType: BehaviorType) =
                apply<T>(items, behaviorType, T::class.java)
    }

    class Optional<out T>(val value: T?) {
        fun isPresent() = value != null
    }

    enum class BehaviorType {
        SingleOrNone,
        SingleOrNoneInit,
        Single,
        SingleInit
    }
}

internal interface SelectionController {
    fun canSelect(item: Selectable): Boolean
    fun canDeselect(item: Selectable): Boolean
    fun onSelect(item: Selectable)
    fun onDeselect(item: Selectable)
}

@Suppress("LeakingThis")
internal abstract class SelectionBehavior<T : Selectable>(protected val itemClass: Class<*>)
    : Selection<T>, SelectionController {

    private val listChangedCallback = OnListChangedCallback(this)
    var items: Iterable<Any>? = null
        set(value) {
            disconnect()
            field = value
            connect()
        }

    protected fun getSelectableItems(items: Iterable<Any>? = null) =
            (items ?: this.items)?.asSequence()?.filterIsInstance<Selectable>() ?: emptySequence()

    protected fun getSelectedItems() = getSelectableItems().filter { it.selected.get() }

    private val selectedCountSubject = BehaviorSubject.create<Int>()
    private val selectedItemsSubject = BehaviorSubject.create<Sequence<T>>()

    override val selectedCount: Observable<Int> = selectedCountSubject
    override var currentSelectedCount: Int
        get() = selectedCountSubject.value
        protected set(value) = selectedCountSubject.onNext(value)
    override val selectedItems: Observable<Sequence<T>> = selectedItemsSubject
    override var currentSelectedItems: Sequence<T>
        get() = selectedItemsSubject.value
        protected set(value) = selectedItemsSubject.onNext(value)

    @Suppress("UNCHECKED_CAST")
    private fun filterItemClass(items: Sequence<Selectable>) =
            items.filterIsInstance(itemClass).map { it as T }

    private fun updateSelectedAndCount(count: Int? = null) {
        if (count == null) {
            val selected = getSelectedItems().toList()
            currentSelectedItems = filterItemClass(selected.asSequence())
            currentSelectedCount = selected.size
        } else {
            currentSelectedItems = filterItemClass(getSelectedItems())
            currentSelectedCount = count
        }
    }

    private fun connect() {
        (items as? ObservableList<Any>)?.addOnListChangedCallback(listChangedCallback)
        connect(getSelectableItems().toList())
    }

    protected open fun connect(items: List<Selectable>) {
        items.forEach { it.controller = this }
        updateSelectedAndCount()
    }

    internal fun disconnect() =
            (items as? ObservableList<Any>)?.removeOnListChangedCallback(listChangedCallback)

    @Suppress("UNCHECKED_CAST")
    override fun onSelect(item: Selectable) {
        if (itemClass.isInstance(item))
            currentSelectedItems = filterItemClass(getSelectedItems())
        currentSelectedCount++
    }

    @Suppress("UNCHECKED_CAST")
    override fun onDeselect(item: Selectable) {
        if (itemClass.isInstance(item))
            currentSelectedItems = filterItemClass(getSelectedItems())
        currentSelectedCount--
    }

    override fun canSelect(item: Selectable) = true

    open fun onAdded(items: List<Selectable>) {
        var selectedCount = 0
        items.forEach {
            it.controller = this
            if (it.selected.get()) selectedCount++
        }
        if (selectedCount != 0)
            updateSelectedAndCount(currentSelectedCount + selectedCount)
    }

    open fun onRemoved() {
        val count = getSelectableItems().count { it.selected.get() }
        if (count != currentSelectedCount)
            updateSelectedAndCount(count)
    }

    open fun onReplaced(newItems: List<Any>) {
        getSelectableItems(newItems).forEach { it.controller = this }
        updateSelectedAndCount()
    }

    private class OnListChangedCallback<T : Selectable>(selection: SelectionBehavior<T>) :
            ObservableList.OnListChangedCallback<ObservableList<Any>>() {
        private val selectionReference = WeakReference<SelectionBehavior<T>>(selection)

        private fun getRange(selection: SelectionBehavior<T>, positionStart: Int, itemCount: Int) =
                selection.items?.asSequence()?.drop(positionStart)?.take(itemCount)
                        ?: emptySequence()

        override fun onChanged(sender: ObservableList<Any>) {
            val selection = selectionReference.get() ?: return
            selection.connect(selection.getSelectableItems().toList())
        }

        override fun onItemRangeRemoved(sender: ObservableList<Any>, positionStart: Int, itemCount: Int) {
            selectionReference.get()?.onRemoved()
        }

        override fun onItemRangeMoved(sender: ObservableList<Any>, fromPosition: Int, toPosition: Int, itemCount: Int) {}

        override fun onItemRangeInserted(sender: ObservableList<Any>, positionStart: Int, itemCount: Int) {
            val selection = selectionReference.get() ?: return
            val items = selection.getSelectableItems(
                    getRange(selection, positionStart, itemCount).asIterable()).toList()
            if (items.isNotEmpty()) selection.onAdded(items)
        }

        override fun onItemRangeChanged(sender: ObservableList<Any>, positionStart: Int, itemCount: Int) {
            val selection = selectionReference.get() ?: return
            selection.onReplaced(getRange(selection, positionStart, itemCount).toList())
        }
    }
}

internal class MultiSelectionBehavior<T : Selectable>(itemClass: Class<*>) : SelectionBehavior<T>(itemClass) {

    constructor(itemClass: Class<*>, items: Iterable<Any>) : this(itemClass) {
        this.items = items
    }

    override fun canDeselect(item: Selectable) = true
}

internal abstract class InitializationSelectionBehavior<T : Selectable>(
        itemClass: Class<*>,
        private val initialize: Boolean = false)
    : SelectionBehavior<T>(itemClass) {

    private val initializeSelectionNeeded get() = initialize && currentSelectedCount == 0

    override fun connect(items: List<Selectable>) {
        super.connect(items)
        if (initializeSelectionNeeded)
            items.firstOrNull()?.select()
    }

    override fun onAdded(items: List<Selectable>) {
        super.onAdded(items)
        if (initializeSelectionNeeded)
            items.firstOrNull()?.select()
    }

    override fun onRemoved() {
        super.onRemoved()
        if (initializeSelectionNeeded)
            getSelectableItems().firstOrNull()?.select()
    }

    override fun onReplaced(newItems: List<Any>) {
        super.onReplaced(newItems)
        if (initializeSelectionNeeded)
            (if (newItems.size > 1) getSelectableItems().firstOrNull()
            else newItems.single() as? Selectable)
                    ?.select()
    }

    override fun canDeselect(item: Selectable) = currentSelectedCount > 1
}

internal class OneOrMoreSelectionBehavior<T : Selectable>(itemClass: Class<*>, initialize: Boolean)
    : InitializationSelectionBehavior<T>(itemClass, initialize) {

    constructor(itemClass: Class<*>, initialize: Boolean, items: Iterable<Any>)
            : this(itemClass, initialize) {
        this.items = items
    }
}

internal class SingleSelectionBehavior<T : Selectable>(
        itemClass: Class<*>,
        initialize: Boolean,
        private val required: Boolean)
    : InitializationSelectionBehavior<T>(itemClass, initialize), SingleSelection<T> {

    constructor(itemClass: Class<*>, initialize: Boolean, required: Boolean, items: Iterable<Any>)
            : this(itemClass, initialize, required) {
        this.items = items
    }

    private val selectedItemSubject = BehaviorSubject.create<SingleSelection.Optional<T>>()
    override val selectedItem: Observable<SingleSelection.Optional<T>> = selectedItemSubject
    override var currentSelectedItem: T?
        get() = selectedItemSubject.value?.value
        private set (value) = selectedItemSubject.onNext(SingleSelection.Optional(value))

    override fun connect(items: List<Selectable>) {
        super.connect(items)
        if (currentSelectedCount == 0) return
        if (currentSelectedCount == 1) {
            val item = currentSelectedItems.firstOrNull()
            if (item != null)
                currentSelectedItem = item
        } else {
            val selectedItems = getSelectedItems().toList()
            selectedItems.drop(1).forEach { it.forceDeselectWithoutEvent() }
            val item = selectedItems.first()
            if (itemClass.isInstance(item)) {
                @Suppress("UNCHECKED_CAST")
                val valid = item as T
                currentSelectedItems = sequenceOf(valid)
                currentSelectedCount = 1
                currentSelectedItem = valid
            } else {
                currentSelectedItems = emptySequence()
                currentSelectedCount = 1
                currentSelectedItem = null
            }
        }
    }

    override fun onSelect(item: Selectable) {
        var previous = currentSelectedItem
        if (previous != null && items?.contains(previous) != true)
            previous = null
        @Suppress("UNCHECKED_CAST")
        val current = if (itemClass.isInstance(item)) item as T else null
        previous?.forceDeselectWithoutEvent()

        if (current == null) {
            if (previous != null) {
                currentSelectedItems = emptySequence()
                currentSelectedCount = 0
            }
        } else {
            currentSelectedItems = sequenceOf(current)
            if (previous == null)
                currentSelectedCount = 1
        }

        currentSelectedItem = current
    }

    override fun onDeselect(item: Selectable) {
        currentSelectedItems = emptySequence()
        currentSelectedCount = 0
        currentSelectedItem = null
    }

    override fun canDeselect(item: Selectable): Boolean = !required || super.canDeselect(item)

    override fun onAdded(items: List<Selectable>) {
        items.forEach { it.cleanControllerAndDeselect() }
        super.onAdded(items)
    }

    override fun onRemoved() {
        super.onRemoved()
        if (currentSelectedCount == 0)
            currentSelectedItem = null
    }

    override fun onReplaced(newItems: List<Any>) {
        if (newItems.size == 1) {
            val selectable = newItems.single() as? Selectable
            selectable?.cleanControllerAndDeselect()
            val current = currentSelectedItem
            if (current == null || getSelectableItems().contains(current)) {
                selectable?.controller = this
                return
            }

            if (selectable == null) {
                currentSelectedItems = emptySequence()
                currentSelectedCount = 0
                currentSelectedItem = null
            } else {
                selectable.select()
                selectable.controller = this

                @Suppress("UNCHECKED_CAST")
                val item = if (itemClass.isInstance(selectable)) selectable as T else null

                if (item == null) {
                    currentSelectedItems = emptySequence()
                    currentSelectedCount = 0
                    currentSelectedItem = null
                } else {
                    currentSelectedItems = sequenceOf(item)
                    currentSelectedItem = item
                }
            }
        } else {
            getSelectableItems(newItems).forEach { it.cleanControllerAndDeselect() }
            super.onReplaced(newItems)
        }
    }
}
