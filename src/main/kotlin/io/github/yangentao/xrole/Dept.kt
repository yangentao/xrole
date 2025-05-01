package io.github.yangentao.xrole

import io.github.yangentao.sql.ModelInsertResult
import io.github.yangentao.sql.clause.*
import io.github.yangentao.sql.filter
import io.github.yangentao.sql.list
import io.github.yangentao.sql.update

@JvmInline
value class Dept(val idValue: Long) {

    fun resources(): List<XRole> {
        return XRole.filter(XRole::gid EQ idValue, XRole::aid EQ 0L, XRole::resid GT 0, XRole::restype GT 0).list()
    }

    fun addAccount(aid: Long, role: Int): ModelInsertResult<XRole> {
        return Acc(aid).addTo(idValue, role)
    }

    fun accounts(): List<XRole> {
        return XRole.filter(XRole::gid EQ idValue, XRole::resid EQ 0, XRole::restype EQ 0, XRole::aid GT 0).list()
    }

    fun entity(): XGroup? {
        val g = XGroup.oneByKey(idValue) ?: return null
        return XGroup.oneByKey(g.eid)
    }

    /**
     * 限当前实体内查找
     * @param idValue 群组ID
     * @param includeCurrent 是否包含当前组(gid)
     */
    fun parents(includeCurrent: Boolean): List<Long> {
        val ls = WITH_RECURSIVE_SELECT("rc", "id", "pid") {
            val a = SELECT(XGroup::id, XGroup::pid).FROM(XGroup).WHERE(XGroup::id EQ idValue, XGroup::eid GT 0L)
            val b = SELECT(XGroup::id, XGroup::pid).FROM(XGroup JOIN "rc" ON (XGroup::id EQUAL "rc.pid"))
            a UNION b
        }.query<XGroup>().list { longValue(1) }
        if (includeCurrent) return ls
        return ls.filter { it != idValue }
    }

    fun find(): XGroup? {
        val g = XGroup.oneByKey(idValue) ?: return null
        if (g.eid == 0L) return null
        return g
    }

    fun edit(gname: String?, subtype: Int?, state: Int?): XGroup? {
        val g = find() ?: return null
        val n = g.update {
            if (gname != null && gname.isNotEmpty()) {
                it.name = gname
            }
            if (subtype != null) {
                it.type = subtype
            }
            if (state != null) {
                it.state = state
            }
        }
        return if (n > 0) g else null
    }

    fun delete(): Int {
        val g = find() ?: return 0
        if (!g.isDept) return 0
        XGroup.update(XGroup::pid EQ idValue, XGroup::pid to g.pid)
        XGroup.delete(XGroup::id EQ idValue)
        XRole.delete(XRole::gid EQ idValue)
        return 1
    }
}