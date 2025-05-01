package io.github.yangentao.xrole

import io.github.yangentao.sql.ModelInsertResult
import io.github.yangentao.sql.clause.*
import io.github.yangentao.sql.filter

/**
 * @param id  Resource ID
 * @param type Resource Type
 */
open class Res(val id: Long, val type: Int) {
    val whereRole: Where get() = (XRole::resid EQ this.id) AND (XRole::restype EQ this.type)
    val whereGroupRole: Where get() = (GroupRole::resid EQ this.id) AND (GroupRole::restype EQ this.type)

    fun removeFromEntity(eid: Long) {
        val idList = Ent(eid).groups().map { it.id }
        if (idList.isEmpty()) return
        XRole.delete(this.whereRole, XRole::gid IN idList)
    }

    fun removeFromDept(gid: Long) {
        assert(gid > 0L)
        XRole.delete(this.whereRole, XRole::gid EQ gid)
    }

    fun listDept(): List<GroupRole> {
        return GroupRole.list(
            this.whereGroupRole,
            GroupRole::gid GT 0L, GroupRole::eid NE 0L, GroupRole::aid EQ 0L
        )
    }

    fun ownerEntity(): GroupRole? {
        return GroupRole.one(
            this.whereGroupRole,
            GroupRole::gid GT 0L, GroupRole::eid EQ 0L, GroupRole::aid EQ 0L
        )
    }

    fun delete() {
        XRole.delete(this.whereRole)
    }

    fun assignToAccount(gid: Long, aid: Long, role: Int): ModelInsertResult<XRole> {
        return XRole.upsert {
            it.gid = gid
            it.aid = aid
            it.res = this
            it.resid = this.id
            it.restype = this.type
            it.rolevalue = role
        }
    }

    fun assignToGroup(gid: Long, role: Int): ModelInsertResult<XRole> {
        return XRole.upsert {
            it.gid = gid
            it.aid = 0L
            it.resid = this.id
            it.restype = type
            it.rolevalue = role
        }
    }

    fun groups(): List<XRole> {
        return XRole.filter(
            XRole::gid GT 0L, XRole::aid EQ 0L,
            XRole::resid EQ this.id, XRole::restype EQ type,
        ).list()
    }

    fun entities(): Set<Long> {
        val ls = GroupRole.filter(
            GroupRole::resid EQ this.id, GroupRole::restype EQ type,
            GroupRole::aid EQ 0L,
            GroupRole::gid GT 0L,
        ).list()
        val idSet = HashSet<Long>()
        for (g in ls) {
            if (g.isEntity) {
                idSet.add(g.id)
            } else {
                idSet.add(g.eid)
            }
        }
        return idSet
    }

    companion object {
        val zero: Res = Res(0L, 0)
    }
}
