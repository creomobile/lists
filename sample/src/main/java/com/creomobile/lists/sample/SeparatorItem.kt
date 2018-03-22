package com.creomobile.lists.sample

import android.graphics.Color

class SeparatorItem(val color: Int) {
    companion object {
        private var lastIndex = 0

        private val colors = arrayOf(
                Color.parseColor("#ffd4e5"),
                Color.parseColor("#d4ffea"),
                Color.parseColor("#eecbff"),
                Color.parseColor("#feffa3"),
                Color.parseColor("#dbdcff")
        )

        fun createNew(): SeparatorItem {
            var index = lastIndex++
            if (index > colors.size - 1) {
                index = 0
                lastIndex = 1
            }

            return SeparatorItem(colors[index])
        }
    }
}