@file:Suppress("unused")

package  io.github.yangentao.singlegroup

import io.github.yangentao.sql.Conflicts
import io.github.yangentao.sql.filter
import io.github.yangentao.sql.utils.StateVal
import io.github.yangentao.xrole.*

object SingleGroup {
    private const val GROUP_ID: Long = 1L
    private val theGroup = Owner(GROUP_ID, 0L)

    fun assign(res: Resource, aid: Long, role: Int) {
        if (res.isEmpty) error("Resource is empty")
        res.assign(Owner(GROUP_ID, aid), role)
    }

    fun revoke(res: Resource, aid: Long) {
        if (res.isEmpty) error("Resource is empty")
        res.delete(Owner(GROUP_ID, aid))
    }

    fun resourceAdd(res: Resource) {
        if (res.isEmpty) error("Resource is empty")
        res.assign(theGroup, RoleValue.ADMIN)
    }

    fun resourceRemove(res: Resource): Int {
        if (res.isEmpty) error("Resource is empty")
        return XGroup.filter(theGroup.whereOwner, res.whereRes).delete()
    }

    fun resourceList(resType: Int? = null, orderBy: String? = null, limit: Int? = null, offset: Int? = null): List<XRole> {
        return Resource.list(theGroup, resType, orderBy = orderBy, limit = limit, offset = offset)
    }

    fun memberList(orderBy: String?, limit: Int? = null, offset: Int? = null): List<XRole> {
        return MemberShip.list(GROUP_ID, orderBy, limit, offset)
    }

    fun memberRemove(aid: Long): Int {
        return MemberShip(GROUP_ID, aid).remove()
    }

    fun memberSave(aid: Long, role: Int): Boolean {
        return MemberShip(GROUP_ID, aid).save(role)
    }

    fun member(aid: Long): XRole? {
        return MemberShip(GROUP_ID, aid).find()
    }

    fun prepare(groupName: String) {
        XGroup.upsert(
            XGroup::id to GROUP_ID,
            XGroup::name to groupName,
            XGroup::pid to 0L,
            XGroup::eid to 0L,
            XGroup::state to StateVal.NORMAL,
            XGroup::type to XGroup.T_LEAF,
            conflict = Conflicts.Ignore
        )
    }
}