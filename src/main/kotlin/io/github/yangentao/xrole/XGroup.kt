@file:Suppress("unused")

package io.github.yangentao.xrole

import io.github.yangentao.anno.*
import io.github.yangentao.sql.*
import io.github.yangentao.sql.clause.*
import io.github.yangentao.sql.utils.SnowJS
import io.github.yangentao.sql.utils.StateVal
import io.github.yangentao.types.DateTime
import java.sql.Timestamp

/**
 * Entity(company), department
 * 组织结构, 实体和群组都是用此数据结构
 * eid == 0 表示是实体
 * eid != 0 表示是群组
 * eid 是群组所属的实体ID
 */
@Label("群组")
class XGroup : TableModel() {

    @Label("ID")
    @ModelField(primaryKey = true)
    var id: Long by model

    // 对部门, 指上级部门
    // 对实体, 指上级实体
    @Label("上级")
    @ModelField(index = true, defaultValue = "0")
    var pid: Long by model

    // 对于部门, 是部门所属的实体
    // 对于实体, 是 0
    @Label("实体")
    @ModelField(index = true, defaultValue = "0")
    var eid: Long by model

    @Label("名称")
    @ModelField(unique = true)
    var name: String by model

    @Label("状态")
    @OptionList("0:正常", "1:停用")
    @ModelField(defaultValue = "0")
    var state: Int by model

    @Label("类型")
    @ModelField(defaultValue = "0")
    var type: Int by model

    @ModelField
    var createDateTime: Timestamp? by model

    @SerialMe
    @TempValue
    var parentName: String? by model

    @SerialMe
    @TempValue
    var entityName: String? by model

    @SerialMe
    @TempValue
    var rolevalue: Int = RoleValue.NONE

    val isEntity: Boolean get() = eid == 0L
    val isDept: Boolean get() = eid > 0L

    val parent: XGroup? get() = XGroup.oneByKey(this.pid)
    val entity: XGroup? get() = if (eid > 0) this else XGroup.oneByKey(this.eid)

    companion object : TableModelClass<XGroup>() {
        const val T_LEAF: Int = 0 //叶子, 不可再创建下级
        const val T_NODE_LEAF: Int = 1 //只可创建叶子
        const val T_NODE: Int = 2 //可以创建 TNODE/TNODE_LEAF/TLEAF

        fun topEntity(): XGroup? {
            return XGroup.one(XGroup::eid EQ 0L, XGroup::pid EQ 0L)
        }

        fun topDept(eid: Long): XGroup? {
            return XGroup.one(XGroup::eid EQ eid, XGroup::pid EQ 0L)
        }

        /**
         * @param name 实体名
         * @param grouptype
         */
        fun addEntity(parentEid: Long, name: String, grouptype: Int = XGroup.T_LEAF): XGroup? {
            val r = XGroup.insert {
                it.id = SnowJS.next()
                it.eid = 0L
                it.pid = parentEid
                it.name = name
                it.type = grouptype
                it.state = StateVal.NORMAL
                it.createDateTime = DateTime.now.timestamp
            }
            return if (r.success) r.model else null
        }

        fun edit(gid: Long, newName: String? = null, subtype: Int? = null, state: Int? = null): XGroup? {
            if (newName == null && subtype == null && state == null) return null
            val e = XGroup.oneByKey(gid) ?: return null
            val n = e.update {
                if (newName != null && newName.isNotEmpty()) {
                    it.name = newName
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

        fun deleteDept(gid: Long): Int {
            if (gid == 0L) error("can not delete gid==0")
            val g = XGroup.oneByKey(gid) ?: return 0
            if (!g.isDept) error("Can not delete entity.")
            XGroup.update(XGroup::pid EQ gid, XGroup::pid to g.pid)
            XGroup.delete(XGroup::id EQ gid)
            XRole.delete(XRole::gid EQ gid)
            return 1
        }

        fun deleteEntity(eid: Long): Int {
            val e = XGroup.oneByKey(eid) ?: return 0
            if (!e.isEntity) error("NOT an entity")
            val w = XGroup::eid EQ eid OR (XGroup::id EQ eid)
            val idSet = XGroup.filter(w).select(XGroup::id).list { longValue() }.toSet()
            XGroup.delete(w)
            if (idSet.isNotEmpty()) {
                XRole.delete(XRole::gid IN idSet)
            }
            return idSet.size
        }

        /**
         * @param gid 群组ID
         * @param includeCurrent 是否包含当前组[gid]
         */
        fun parents(gid: Long, includeCurrent: Boolean): List<Long> {
            val ls = WITH_RECURSIVE_SELECT("rc", "id", "pid") {
                val a = SELECT(XGroup::id, XGroup::pid).FROM(XGroup).WHERE(XGroup::id EQ gid)
                val b = SELECT(XGroup::id, XGroup::pid).FROM(XGroup JOIN "rc" ON (XGroup::id EQUAL "rc.pid"))
                a UNION b
            }.query<XGroup>().list { longValue(1) }
            if (includeCurrent) return ls
            return ls.filter { it != gid }
        }

        /**
         * @param gid 群组ID
         * @param includeCurrent 是否包含当前组[gid]
         */
        fun children(gid: Long, includeCurrent: Boolean): List<Long> {
            val ls = WITH_RECURSIVE_SELECT("rc", "id", "pid") {
                val a = SELECT(XGroup::id, XGroup::pid).FROM(XGroup).WHERE(XGroup::id EQ gid)
                val b = SELECT(XGroup::id, XGroup::pid).FROM(XGroup JOIN "rc" ON (XGroup::pid EQUAL "rc.id"))
                a UNION b
            }.query<XGroup>().list { longValue(1) }
            if (includeCurrent) return ls
            return ls.filter { it != gid }
        }
    }
}
