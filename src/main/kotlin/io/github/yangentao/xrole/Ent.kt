package io.github.yangentao.xrole

import io.github.yangentao.hare.utils.SnowJS
import io.github.yangentao.hare.utils.StateVal
import io.github.yangentao.sql.ModelInsertResult
import io.github.yangentao.sql.clause.*
import io.github.yangentao.sql.filter
import io.github.yangentao.sql.list
import io.github.yangentao.sql.update
import io.github.yangentao.types.DateTime

@JvmInline
value class Ent(val idValue: Long) {

    fun resources(): List<XRole> {
        return XRole.filter(XRole::gid EQ idValue, XRole::aid EQ 0L, XRole::resid GT 0, XRole::restype GT 0).list()
    }

    fun addAccount(aid: Long, role: Int): ModelInsertResult<XRole> {
        return Acc(aid).addTo(idValue, role)
    }

    fun accounts(): List<XRole> {
        return XRole.filter(XRole::gid EQ idValue, XRole::aid GT 0, XRole::resid EQ 0, XRole::restype EQ 0).list()
    }

    fun depts(): List<XGroup> {
        return XGroup.filter(XGroup::eid EQ idValue).list()
    }

    // entity and depts
    fun groups(): List<XGroup> {
        return XGroup.filter(XGroup::eid EQ idValue OR (XGroup::id EQ idValue)).list()
    }

    /**
     * @param idValue 群组ID
     * @param includeCurrent 是否包含当前组[idValue]
     */
    fun parents(includeCurrent: Boolean): List<Long> {
        val ls = WITH_RECURSIVE_SELECT("rc", "id", "pid") {
            val a = SELECT(XGroup::id, XGroup::pid).FROM(XGroup).WHERE(XGroup::id EQ idValue, XGroup::eid EQ 0L)
            val b = SELECT(XGroup::id, XGroup::pid).FROM(XGroup JOIN "rc" ON (XGroup::id EQUAL "rc.pid"))
            a UNION b
        }.query<XGroup>().list { longValue(1) }
        if (includeCurrent) return ls
        return ls.filter { it != idValue }
    }
    /**
     * @param idValue 群组ID
     * @param includeCurrent 是否包含当前组[idValue]
     */
    fun children(includeCurrent: Boolean): List<Long> {
        val ls = WITH_RECURSIVE_SELECT("rc", "id", "pid") {
            val a = SELECT(XGroup::id, XGroup::pid).FROM(XGroup).WHERE(XGroup::id EQ idValue, XGroup::eid EQ 0L)
            val b = SELECT(XGroup::id, XGroup::pid).FROM(XGroup JOIN "rc" ON (XGroup::pid EQUAL "rc.id"))
            a UNION b
        }.query<XGroup>().list { longValue(1) }
        if (includeCurrent) return ls
        return ls.filter { it != idValue }
    }
    fun find(): XGroup? {
        val e = XGroup.oneByKey(idValue)
        if (e?.eid == 0L) return e
        return null
    }

    fun edit(ename: String? = null, subtype: Int? = null, state: Int? = null): XGroup? {
        val e = XGroup.oneByKey(idValue) ?: return null
        val n = e.update {
            if (ename != null && ename.isNotEmpty()) {
                it.name = ename
            }
            if (subtype != null) {
                it.type = subtype
            }
            if (state != null) {
                it.state = state
            }
        }
        return if (n > 0) e else null
    }

    fun delete(): Int {
        val w = XGroup::eid EQ idValue OR (XGroup::id EQ idValue)
        val idSet = XGroup.filter(w).select(XGroup::id).list { longValue() }.toSet()
        XGroup.delete(w)
        if (idSet.isNotEmpty()) {
            XRole.delete(XRole::gid IN idSet)
        }
        return idSet.size
    }

    /**
     * @param idValue 实体
     * @param gname 组名
     * @param parent 上级群组
     * @param subtype 组类型
     */
    fun createDept(gname: String, parent: Long, subtype: Int): XGroup? {
        val r = XGroup.insert {
            it.id = SnowJS.next()
            it.eid = this@Ent.idValue
            it.name = gname
            it.pid = parent
            it.type = subtype
            it.state = StateVal.NORMAL
            it.createDateTime = DateTime.now.formatDateTime()
        }
        return if (r.success) r.model else null
    }
}