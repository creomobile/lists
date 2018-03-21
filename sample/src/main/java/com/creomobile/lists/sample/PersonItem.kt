package com.creomobile.lists.sample

import android.graphics.Color
import com.creomobile.lists.Selectable

class PersonItem private constructor(
        val id: Int,
        val name: String,
        val color: Int,
        private val removeHandler: ((PersonItem) -> Unit)?)
    : Selectable() {

    fun remove() = removeHandler?.invoke(this)

    companion object {
        private var lastIndex = 0
        private val names = arrayOf(
                "Rochel Treaster",
                "Twanna Trundy",
                "Harmony Bullen",
                "Tyson Burger",
                "Johnnie Blanding",
                "Kina Charest",
                "Inge Parnell",
                "Garnet Lietz",
                "Jere Billman",
                "Kathern Luebbers",
                "Candie Quintal",
                "Janessa Raglin",
                "Adrianna Stearn",
                "Arnita Manser",
                "Terri Shanklin",
                "Tiny Boettger",
                "Lisa Ogawa",
                "Amalia Whitton",
                "Jolene Firestone",
                "Skye Mclawhorn",
                "Abraham Sumrall",
                "Pedro Correia",
                "Newton Grieb",
                "Simona Vandiver",
                "Ronda Breslin",
                "Luther Dewitt",
                "Lorean Bassler",
                "Charmain Hibbard",
                "Sid Quick",
                "Wei Burr",
                "Ollie Sullins",
                "Lasandra Armijo",
                "Charlie Bagley",
                "Gilda Witkowski",
                "Glennie Mcniel",
                "Catrina Hamman",
                "Ivory Champion",
                "Shirley Oconner",
                "Meg Madruga",
                "Venessa Follansbee",
                "Felipa Swindell",
                "Lashon Vallejo",
                "Marguerita Weisser",
                "Pia Summitt",
                "Darleen Bonk",
                "Jillian Gruver",
                "Daniel Branson",
                "Zoe Filippi",
                "Horacio Oberman",
                "Jeanne Leong"
        )

        private val colors = arrayOf(
                Color.parseColor("#ffd4e5"),
                Color.parseColor("#d4ffea"),
                Color.parseColor("#eecbff"),
                Color.parseColor("#feffa3"),
                Color.parseColor("#dbdcff")
        )

        fun createNew(removeHandler: ((PersonItem) -> Unit)? = null): PersonItem {
            var index = lastIndex++
            if (index > names.size - 1) {
                index = 0
                lastIndex = 1
            }

            return PersonItem(index + 1, names[index], colors[index.rem(colors.size)],
                    removeHandler)
        }
    }
}
