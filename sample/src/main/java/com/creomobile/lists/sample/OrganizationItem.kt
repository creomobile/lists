package com.creomobile.lists.sample

import com.creomobile.lists.Selectable

class OrganizationItem private constructor(
        val id: Int, val name: String, private val removeHandler: ((OrganizationItem) -> Unit)?)
    : Selectable() {

    fun remove() = removeHandler?.invoke(this)

    companion object {
        private var lastIndex = 0
        private val names = arrayOf(
                "Quantum",
                "Funk",
                "Hallow",
                "Grime",
                "Slayer Sisterhood",
                "Iron Clan",
                "Slayer Sisterhood",
                "Assembly of Chaos",
                "Brotherhood of Slayers",
                "Squad of Frost",
                "Zion",
                "Blight",
                "Arcane",
                "Psi",
                "Specter Posse",
                "Horror Posse",
                "Nano Clan",
                "Assembly of Blight",
                "Order of Diablo",
                "Clan of Death",
                "Sliver",
                "Nitro",
                "Dynamo",
                "Miasma",
                "Unseen Clan",
                "Echo Force",
                "Fiend Posse",
                "Order of Battle",
                "Tribe of the Sinner",
                "Posse of Frost",
                "Storm",
                "Chrome",
                "Blight",
                "Serpent",
                "Frost Squad",
                "Spite Posse",
                "Spectral Assembly",
                "Tribe of the Universe",
                "Force of the Virus",
                "Brotherhood of Steel",
                "Iron",
                "Shadow",
                "Taint",
                "Taunt",
                "Spectral Company",
                "Cataclysm Squad",
                "Venom Crew",
                "Force of the Infernal",
                "Clan of Spite",
                "Clan of Lightning"
        )

        fun createNew(removeHandler: ((OrganizationItem) -> Unit)? = null): OrganizationItem {
            var index = lastIndex++
            if (index > names.size - 1) {
                index = 0
                lastIndex = 1
            }

            return OrganizationItem(index + 1, names[index], removeHandler)
        }
    }
}


