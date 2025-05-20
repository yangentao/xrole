package io.github.yangentao.xrole

import kotlin.math.max

fun maxRole(a: Int?, b: Int?): Int? {
    if (a == null) return b
    if (b == null) return a
    return max(a, b)
}