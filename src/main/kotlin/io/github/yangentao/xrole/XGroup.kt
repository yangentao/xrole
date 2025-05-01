package io.github.yangentao.xrole

import io.github.yangentao.anno.*
import io.github.yangentao.hare.utils.SnowJS
import io.github.yangentao.hare.utils.StateVal
import io.github.yangentao.sql.TableModel
import io.github.yangentao.sql.TableModelClass
import io.github.yangentao.sql.clause.*
import io.github.yangentao.sql.list
import io.github.yangentao.types.DateTime

// 组织结构, 实体和群组都是用此数据结构
// eid == 0 表示是实体
// eid != 0 表示是群组
// eid 是群组所属的实体ID
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
    @OptionList("0:正常", "1:禁用")
    @ModelField(defaultValue = "0")
    var state: Int by model

    /**
     * @See  [SubType]
     */
    @Label("类型")
    @ModelField(defaultValue = "0")
    var type: Int by model

    @ModelField
    var createDateTime: String? by model

    @SerialMe
    @TempValue
    var parentName: String? by model

    @SerialMe
    @TempValue
    var entityName: String? by model

    @SerialMe
    @TempValue
    var rolevalue: Int = RoleValue.none

    val isEntity: Boolean get() = eid == 0L
    val isDept: Boolean get() = eid > 0L

    val isLeaf: Boolean get() = type == Subtype.leaf

    val ent: Ent
        get() {
            assert(isEntity)
            return Ent(id)
        }
    val dept: Dept
        get() {
            assert(isDept)
            return Dept(id)
        }

    companion object : TableModelClass<XGroup>() {
        /**
         * @param ename 实体名
         * @param parent 上级实体
         * @param subtype 类型[SubType]
         */
        fun createEntity(ename: String, parent: Long, subtype: Int): XGroup? {
            val r = XGroup.insert {
                it.id = SnowJS.next()
                it.eid = 0L
                it.name = ename
                it.pid = parent
                it.type = subtype
                it.state = StateVal.NORMAL
                it.createDateTime = DateTime.now.formatDateTime()
            }
            return if (r.success) r.model else null
        }

        fun rootEntity(): XGroup? {
            return XGroup.one(XGroup::eid EQ 0L, XGroup::pid EQ 0L)
        }

        /**
         * @param gid 群组ID
         * @param includeCurrent 是否包含当前组[gid]
         */
        fun parents(gid: Long, includeCurrent: Boolean): List<Long> {
            val ls = WITH_RECURSIVE_SELECT("rc", "id", "pid") {
                val a = SELECT(XGroup::id, XGroup::pid).FROM(XGroup).WHERE(XGroup::id EQ gid, XGroup::eid EQ 0L)
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
                val a = SELECT(XGroup::id, XGroup::pid).FROM(XGroup).WHERE(XGroup::id EQ gid, XGroup::eid EQ 0L)
                val b = SELECT(XGroup::id, XGroup::pid).FROM(XGroup JOIN "rc" ON (XGroup::pid EQUAL "rc.id"))
                a UNION b
            }.query<XGroup>().list { longValue(1) }
            if (includeCurrent) return ls
            return ls.filter { it != gid }
        }
    }
}
