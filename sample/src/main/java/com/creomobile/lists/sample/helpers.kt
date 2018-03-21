package com.creomobile.lists.sample

import java.util.concurrent.ThreadLocalRandom

fun ClosedRange<Int>.random() =
        ThreadLocalRandom.current().nextInt(start, endInclusive + 1)
