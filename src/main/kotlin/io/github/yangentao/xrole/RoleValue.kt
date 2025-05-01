@file:Suppress("unused")

package io.github.yangentao.xrole

import kotlin.math.max

@Suppress("ConstPropertyName", "unused")
object RoleValue {
    const val none: Int = 0
    const val read: Int = 1
    const val write: Int = 2
    const val manage: Int = 4
    const val admin: Int = 8
}

val Int.isValidRole: Boolean get() = this in 0..8

fun maxRole(a: Int?, b: Int?): Int? {
    if (a == null) return b
    if (b == null) return a
    return max(a, b)
}