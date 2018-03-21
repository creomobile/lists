package com.creomobile.lists

import android.content.Context
import android.databinding.Observable
import android.graphics.ColorFilter
import android.graphics.drawable.ColorDrawable
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class SelectableFrameLayout : FrameLayout {

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int)
            : super(context, attrs, defStyle) {
        init(attrs, defStyle)
    }

    private lateinit var selectionView: View
    private val selectedChangedCallback = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) = updateSelected()
    }
    internal var selectable: Selectable? = null
        set(value) {
            field?.selected?.removeOnPropertyChangedCallback(selectedChangedCallback)
            field = value
            value?.selected?.addOnPropertyChangedCallback(selectedChangedCallback)
            updateSelected()
        }
    private var saveSelected = false
    private val foregroundItems: Iterable<ForegroundItem> by lazy {
        searchForegroundItems(children).toList()
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        selectionView = View(context).apply {
            visibility = View.INVISIBLE
            layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT)
        }

        var a = context.obtainStyledAttributes(
                attrs, R.styleable.SelectableFrameLayout, defStyle, 0)
        val res = a.getResourceId(R.styleable.SelectableFrameLayout_selectionBackground, -1)
        a.recycle()

        if (res == -1) {
            val typedValue = TypedValue()
            a = context.obtainStyledAttributes(typedValue.data, intArrayOf(R.attr.colorAccent))
            val color = a.getColor(0, 0)
            a.recycle()
            selectionView.background = ColorDrawable(color)
        } else {
            selectionView.background = ContextCompat.getDrawable(context, res)!!
        }

        addView(selectionView)

        val value = TypedValue()
        context.theme.resolveAttribute(R.attr.selectableItemBackground, value, true)
        val activeView = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT)
            setBackgroundResource(value.resourceId)
            setOnClickListener { selectable?.switch() }
        }

        addView(activeView)
    }

    private inline val ViewGroup.children get() = (0 until childCount).map { getChildAt(it) }

    private fun searchForegroundItems(items: Iterable<View>): Iterable<ForegroundItem> =
            items.flatMap {
                if (it is ViewGroup) {
                    searchForegroundItems(it.children)
                } else {
                    val item = ForegroundItem.createFromView(it)
                    if (item == null) emptyList() else listOf(item)
                }
            }

    private fun updateSelected() {

        val animation: Animation

        val value = selectable?.selected?.get() == true
        if (value == saveSelected) return
        saveSelected = value

        if (value) {
            foregroundItems.forEach {
                it.setSelectedForeground(ContextCompat.getColor(context, R.color.selectedTextColor))
            }
            selectionView.visibility = View.VISIBLE
            animation = AlphaAnimation(0F, 1F).apply {
                interpolator = DecelerateInterpolator()
                duration = 100
                fillAfter = true
            }
        } else {
            foregroundItems.forEach { it.restore() }
            animation = AlphaAnimation(0.7F, 0F).apply {
                interpolator = DecelerateInterpolator()
                duration = 100
            }
            animation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationRepeat(p0: Animation?) {}
                override fun onAnimationStart(p0: Animation?) {}
                override fun onAnimationEnd(p0: Animation?) {
                    selectionView.visibility = View.INVISIBLE
                }
            })
        }

        selectionView.startAnimation(animation)
    }

    private interface ForegroundItem {
        fun setSelectedForeground(color: Int)
        fun restore()

        companion object {
            fun createFromView(view: View): ForegroundItem? {
                fun shouldIgnore() = view.getTag(R.id.ignore_selection) == true
                return when (view) {
                    is TextView -> if (shouldIgnore()) null else TextViewForeground(view)
                    is ImageView -> if (shouldIgnore()) null else ImageViewForeground(view)
                    else -> null
                }
            }
        }

        private class TextViewForeground(private val textView: TextView) : ForegroundItem {
            private var previousTextColor: Int? = null
            override fun setSelectedForeground(color: Int) {
                previousTextColor = textView.currentTextColor
                textView.setTextColor(color)
            }

            override fun restore() {
                textView.setTextColor(previousTextColor ?: return)
            }
        }

        private class ImageViewForeground(private val imageView: ImageView) : ForegroundItem {
            private var previousColorFilter: ColorFilter? = null
            override fun setSelectedForeground(color: Int) {
                previousColorFilter = imageView.colorFilter
                imageView.setColorFilter(color)
            }

            override fun restore() {
                imageView.colorFilter = previousColorFilter
            }
        }
    }
}
