@file:Suppress("ConstPropertyName")

package io.github.yangentao.xrole

import io.github.yangentao.sql.Conflicts
import io.github.yangentao.sql.clause.EQ
import io.github.yangentao.sql.utils.StateVal

const val EID_ROOT = 1L

object RootEntity {
    const val EID: Long = EID_ROOT

    val entity: XGroup? get() = XGroup.oneByKey(EID)

    fun prepare(entityName: String = "RootEntity", etype: Int = XGroup.T_NODE) {
        XGroup.upsert(
            XGroup::id to EID,
            XGroup::name to entityName,
            XGroup::pid to 0L,
            XGroup::eid to 0L,
            XGroup::state to StateVal.NORMAL,
            XGroup::type to etype,
            conflict = Conflicts.Ignore
        )
    }

    fun rename(newName: String): Boolean {
        return XGroup.update(XGroup::id EQ EID, XGroup::name to newName) == 1
    }
}