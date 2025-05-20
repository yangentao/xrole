@file:Suppress("unused")

package  io.github.yangentao.singleentity

import io.github.yangentao.sql.Conflicts
import io.github.yangentao.sql.clause.AND
import io.github.yangentao.sql.clause.ASC
import io.github.yangentao.sql.clause.EQ
import io.github.yangentao.sql.clause.Where
import io.github.yangentao.sql.filter
import io.github.yangentao.sql.utils.StateVal
import io.github.yangentao.xrole.*

object SingleEntity {
    private const val EID: Long = 1L
    val entity: Owner = Owner(EID, 0L)

    fun listDept(pid: Long? = null, orderBy: String? = null, limit: Int? = null, offset: Int? = null): List<XGroup> {
        val w: Where = if (pid == null) XGroup::eid EQ EID else XGroup::eid EQ EID AND (XGroup::pid EQ pid)
        return XGroup.filter(w).list(orderBy ?: XGroup::id.ASC, limit = limit, offset = offset)
    }

    fun createDept(name: String, pid: Long = 0L, type: Int = XGroup.T_NODE): XGroup? {
        var newType: Int = type
        val pg: XGroup? = if (pid == 0L) null else XGroup.oneByKey(pid)
        if (pg != null) {
            if (pg.eid != EID) error("错误的组, eid错误.")
            when (pg.type) {
                XGroup.T_NODE -> newType = type
                XGroup.T_NODE_LEAF -> newType = XGroup.T_LEAF
                XGroup.T_LEAF -> error("Can not create child group, this is a leaf group.")
            }
        }

        return XGroup.create(EID, pid, name, newType)
    }

    fun assign(res: Resource, owner: Owner, role: Int) {
        if (res.isEmpty) error("Resource is empty")
        res.assign(owner, role)
    }

    fun revoke(res: Resource, owner: Owner) {
        if (res.isEmpty) error("Resource is empty")
        res.delete(owner)
    }

    fun resourceAdd(res: Resource, owner: Owner) {
        if (res.isEmpty) error("Resource is empty")
        res.assign(owner, RoleValue.ADMIN)
    }

    fun resourceRemove(res: Resource, owner: Owner): Int {
        if (res.isEmpty) error("Resource is empty")
        return XGroup.filter(owner.whereOwner, res.whereRes).delete()
    }

    fun resourceList(owner: Owner, resType: Int? = null, orderBy: String? = null, limit: Int? = null, offset: Int? = null): List<XRole> {
        return Resource.list(owner, resType, orderBy, limit, offset)
    }

    fun memberList(gid: Long, orderBy: String?, limit: Int? = null, offset: Int? = null): List<XRole> {
        return MemberShip.list(gid, orderBy, limit, offset)
    }

    fun memberRemove(mem: MemberShip): Int {
        return mem.remove()
    }

    fun memberSave(mem: MemberShip, role: Int): Boolean {
        return mem.save(role)
    }

    fun member(mem: MemberShip): XRole? {
        return mem.find()
    }

    fun prepare(entityName: String) {
        XGroup.upsert(
            XGroup::id to EID,
            XGroup::name to entityName,
            XGroup::pid to 0L,
            XGroup::eid to 0L,
            XGroup::state to StateVal.NORMAL,
            XGroup::type to XGroup.T_LEAF,
            conflict = Conflicts.Ignore
        )
    }

}