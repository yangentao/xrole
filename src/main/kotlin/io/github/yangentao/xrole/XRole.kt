package io.github.yangentao.xrole

import io.github.yangentao.anno.Label
import io.github.yangentao.anno.ModelField
import io.github.yangentao.sql.*
import io.github.yangentao.sql.clause.*

/**
 * 对资源的控制权限
 * 当 [resid] 是0的时候, 表示账号[aid]在群组[gid]中的角色
 * 当 [aid] 是0的时候, 表示群组[gid]对资源[resid] 的访问权限
 * 当 [gid] 是0的时候, 表示[aid] 单独对资源[resid] 的访问权限
 * 当 [aid] 和 [gid] 都是 0 的时候, 表示 [resid] 的默认访问权限
 */
class XRole : TableModel() {
    //group id
    @ModelField(primaryKey = true, defaultValue = "0")
    var gid: Long by model

    //account id
    @ModelField(primaryKey = true, defaultValue = "0")
    var aid: Long by model

    @Label("资源")
    @ModelField(primaryKey = true, defaultValue = "0")
    var resid: Long by model

    @Label("资源类型")
    @ModelField(primaryKey = true, defaultValue = "0")
    var restype: Int by model

    @Label("角色")
    @ModelField(defaultValue = "0")
    var rolevalue: Int by model

    var res: Res
        get() = Res(this.resid, this.restype)
        set(value) {
            this.resid = value.id
            this.restype = value.type
        }

    companion object : TableModelClass<XRole>() {
        val RoleJoinGroup: SQLNode get() = SELECT(XRole.ALL).FROM(XRole::class JOIN XGroup::class ON (XRole::gid EQUAL XGroup::id))
        fun byKey(gid: Long, aid: Long, resid: Long, restype: Int): XRole? {
            return XRole.one(XRole::gid EQ gid, XRole::aid EQ aid, XRole::resid EQ resid, XRole::restype EQ restype)
        }

        fun delete(res: Res, gid: Long?, aid: Long?): Int {
            var w: Where = res.whereRole
            if (gid != null) {
                w = w AND (XRole::gid EQ gid)
            }
            if (aid != null) {
                w = w AND (XRole::aid EQ aid)
            }
            return XRole.delete(w)
        }

        fun upsert(res: Res, gid: Long, aid: Long, role: Int, conflict: Conflicts = Conflicts.Update): InsertResult {
            return upsert(XRole::gid to gid, XRole::aid to aid, XRole::resid to res.id, XRole::restype to res.type, XRole::rolevalue to role, conflict = conflict)
        }

        fun listRes(gid: Long, resType: Int): List<XRole> {
            return filter(XRole::gid EQ gid, XRole::aid EQ 0L, XRole::restype EQ resType).list()
        }

    }

}