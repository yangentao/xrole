package io.github.yangentao.xrole

import io.github.yangentao.anno.Label
import io.github.yangentao.anno.ModelField
import io.github.yangentao.anno.OptionList
import io.github.yangentao.anno.TempValue
import io.github.yangentao.sql.ViewModel
import io.github.yangentao.sql.ViewModelClass
import io.github.yangentao.sql.clause.*
import io.github.yangentao.sql.filter

class GroupRole : ViewModel() {

    // group id
    @Label("ID")
    @ModelField()
    var id: Long by model

    // 对部门, 指上级部门
    // 对实体, 指上级实体
    @Label("上级")
    @ModelField(index = true, defaultValue = "0")
    var pid: Long by model

    // owner entity
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

    /// subtype
    @Label("类型")
    @ModelField(defaultValue = "0")
    var type: Int by model

    @ModelField
    var createDateTime: String? by model

    //group id
    @ModelField(defaultValue = "0")
    var gid: Long by model

    //account id
    @ModelField(defaultValue = "0")
    var aid: Long by model

    @Label("资源")
    @ModelField(defaultValue = "0")
    var resid: Long by model

    @Label("资源类型")
    @ModelField(defaultValue = "0")
    var restype: Int by model

    @Label("角色")
    @ModelField(defaultValue = "0")
    var rolevalue: Int by model

    @TempValue
    var parentName: String? by model

    @TempValue
    var entityName: String? by model

    val isEntity: Boolean get() = eid == 0L
    val isDept: Boolean get() = eid > 0L

    companion object : ViewModelClass<GroupRole>() {
        override fun onCreateView(): SQLNode? {
            return SELECT(XGroup.ALL, XRole.ALL).FROM(XGroup JOIN XRole ON (XGroup::id EQUAL XRole::gid))
        }

        val UNGROUP_W: Where = NOT_EXISTS(
            SELECT("1").FROM(GroupRole AS "dg").WHERE("dg.eid" EQ GroupRole::id, "dg.aid" EQ GroupRole::aid, "dg.resid" EQ GroupRole::resid, "dg.restype" EQ GroupRole::restype).LIMIT(1)
        )

        fun ungrouped(eid: Long): SQLNode {
            val a = SELECT(GroupRole::resid, GroupRole::restype).FROM(GroupRole).WHERE(GroupRole::gid EQ eid)
            val b = SELECT(GroupRole::resid, GroupRole::restype).FROM(GroupRole).WHERE(GroupRole::eid EQ eid)
            return a.."EXCEPT"..b
        }


    }
}
