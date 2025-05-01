@file:Suppress("unused")

package io.github.yangentao.xrole.page

import io.github.yangentao.anno.userName
import io.github.yangentao.hare.Action
import io.github.yangentao.hare.HttpContext
import io.github.yangentao.hare.OnHttpContext
import io.github.yangentao.hare.offsetValue
import io.github.yangentao.hare.utils.BadValue
import io.github.yangentao.kson.JsonFailed
import io.github.yangentao.kson.JsonResult
import io.github.yangentao.kson.JsonSuccess
import io.github.yangentao.sql.clause.*
import io.github.yangentao.sql.jsonResult
import io.github.yangentao.sql.listOrm
import io.github.yangentao.sql.oneOrm
import io.github.yangentao.sql.update
import io.github.yangentao.xrole.Ent
import io.github.yangentao.xrole.XGroup

class XGroupPage(override val context: HttpContext) : OnHttpContext {
    val fixWhere: Where = XGroup::eid NE 0

    @Action
    fun searchName(name: String?, limit: Int = 100): JsonResult {
        val w: Where? = if (name == null || name.trim().isEmpty()) null else XGroup::name CONTAINS name.trim()

        val ls: List<String> = XGroup.listColumnValue(XGroup::name, fixWhere, w) {
            ORDER_BY(XGroup::name)
            LIMIT(limit)
        }
        return JsonSuccess(data = ls)
    }

    @Action
    fun delete(id: String): JsonResult {
        val ls = id.split(',').map { it.toLong() }
        if (ls.isEmpty()) return BadValue
        val n = XGroup.delete(fixWhere, XGroup::id IN ls)
        return JsonResult(ok = n > 0, data = n)
    }

    @Action
    fun update(id: Long, column: String, value: String): JsonResult {
        val r = XGroup.update(id, column, value, setOf("name", "type", "state"))
        if (!r.OK) return r
        val g: XGroup? = SELECT(XGroup.ALL, "p.name" AS XGroup::parentName.userName, "e.name" AS XGroup::entityName.userName).FROM(
            (XGroup LEFT_JOIN (XGroup AS "p") ON (XGroup::pid EQUAL "p.id"))
                    LEFT_JOIN (XGroup AS "e") ON (XGroup::eid EQUAL "e.id")
        ).WHERE(XGroup::id EQ id).LIMIT(1).query<XGroup>().oneOrm()
        return g?.jsonResult(includes = listOf(XGroup::parentName, XGroup::entityName)) ?: BadValue
    }

    @Action
    fun rename(id: Long, name: String): JsonResult {
        if (XGroup.exists(XGroup::name EQ name)) return JsonFailed("名称已经存在")

        val g = XGroup.oneByKey(id) ?: return BadValue
        g.update {
            it.name = name
        }
        return g.jsonResult()
    }

    @Action
    fun create(eid: Long, name: String, pid: Long, subtype: Int): JsonResult {
        if (eid <= 0) return BadValue
        val g = Ent(eid).createDept(name, pid, subtype) ?: return BadValue
        return g.jsonResult()
    }

    @Action
    fun listSimple(): JsonResult {
        val qw: Where? = XGroup.queryConditionsCTX()
        val w: Where? = fixWhere AND qw
        val ls = XGroup.list(w) {
            orderByCTX()
            limitByCTX()
        }
        val total = XGroup.countAll(w)
        return ls.jsonResult(total = total, offset = offsetValue)
    }

    @Action
    fun list(): JsonResult {
        val w: Where? = fixWhere AND XGroup.queryConditionsCTX()
        val ls: List<XGroup> = SELECT(XGroup.ALL, "p.name" AS XGroup::parentName.userName, "e.name" AS XGroup::entityName.userName).FROM(
            (XGroup LEFT_JOIN (XGroup AS "p") ON (XGroup::pid EQUAL "p.id")) LEFT_JOIN (XGroup AS "e") ON (XGroup::eid EQUAL "e.id")
        ).WHERE(w).orderByCTX().limitByCTX().query<XGroup>().listOrm()
        val total = XGroup.countAll(w)
        return ls.jsonResult(total = total, offset = offsetValue, includes = listOf(XGroup::parentName, XGroup::entityName))
    }
}
