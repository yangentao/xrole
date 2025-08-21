@file:Suppress("unused")

package  io.github.yangentao.xrole

import kotlin.math.max

val Int.toRole: RoleValue get() = RoleValue(this)

data class RoleValue(val role: Int) {

    val canRead: Boolean get() = role and RoleBit.READ != 0
    val canWrite: Boolean get() = role and RoleBit.WRITE != 0
    val canCreate: Boolean get() = role and RoleBit.CREATE != 0
    val canDelete: Boolean get() = role and RoleBit.DELETE != 0
    val canAssign: Boolean get() = role and RoleBit.ASSIGN != 0

    companion object {
        const val NONE: Int = 0
        const val READ: Int = 1  // [ READ]
        const val WRITE: Int = 3 // [ READ,  WRITE]
        const val MANAGE: Int = 15 // [ READ,  WRITE,  CREATE,  DELETE]
        const val ADMIN: Int = 31 // [ READ,  WRITE,  CREATE,  DELETE,  ROLE]
    }
}

object RoleBit {
    const val READ: Int = 1
    const val WRITE: Int = 2
    const val CREATE: Int = 4
    const val DELETE: Int = 8
    const val ASSIGN: Int = 16
}

fun maxRole(a: Int?, b: Int?): Int? {
    if (a == null) return b
    if (b == null) return a
    return max(a, b)
}