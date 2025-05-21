package  io.github.yangentao.xrole

import io.github.yangentao.sql.clause.*
import io.github.yangentao.sql.filter

open class Resource(val resid: Long, val restype: Int = 0) {
    val isEmpty: Boolean = resid == 0L
    val where: Where = XRole::resid EQ resid AND (XRole::restype EQ restype)

    fun accRole(aid: Long): Int? {
        // 1. 指定了资源
        XRole.filter(XRole::aid EQ aid, where).list().maxOfOrNull { it.rolevalue }?.also { return it }

        return null
//        // 2. 所属组的默认权限
//        val gidList: List<Long> = XRole.filter(XRole::aid EQ aid, XRole::gid GT 0L, zero.whereRes).select(XRole::gid).list { longValue() }
//        val roleSet: List<Int> = XRole.filter(XRole::gid IN gidList.toSet(), XRole::aid EQ 0L, whereRes).select(XRole::rolevalue).list { intValue() }
//        roleSet.maxOrNull()?.also { return it }
//        return null

//        // entity 交集
//        val accEntities = entities()
//        val resEntities: Set<Long> = res.entities()
//        val cross = accEntities.filter { it.gid in resEntities }
//        if (cross.isNotEmpty()) return cross.maxBy { it.rolevalue }.rolevalue
//
//        // 上级entity
//        val superResEntities: HashSet<Long> = HashSet()
//        for (eid in resEntities) {
//            superResEntities.addAll(Ent(eid).parents(false))
//        }
//        if (superResEntities.isEmpty()) return null
//
//        val accEs: List<XRole> = accEntities.filter { it.gid in superResEntities }
//        val maxRole = accEs.maxOfOrNull { it.rolevalue } ?: return null
//        // 只有管理员可操作下级实体的资源
//        return if (maxRole == RoleValue.ADMIN) RoleValue.ADMIN else null
    }

    fun find(owner: Owner): XRole? {
        if (this.isEmpty) error("empty resource")
        return XRole.one(where, owner.where)
    }

    fun assign(owner: Owner, role: Int): Boolean {
        if (this.isEmpty) error("empty resource")
        return XRole.upsert(
            XRole::gid to owner.gid,
            XRole::aid to owner.aid,
            XRole::resid to resid,
            XRole::restype to restype,
            XRole::rolevalue to role
        ).success
    }

    fun revoke(owner: Owner): Int {
        if (this.isEmpty) error("empty resource")
        return XRole.filter(owner.where, where).update(XRole::rolevalue to RoleValue.NONE)
    }

    fun delete(owner: Owner): Int {
        if (this.isEmpty) error("empty resource")
        return XRole.filter(owner.where, where).delete()
    }

    override fun equals(other: Any?): Boolean {
        if (other is Resource) return this.resid == other.resid && this.restype == other.restype
        return false
    }

    override fun hashCode(): Int {
        return resid.hashCode() + restype.hashCode()
    }

    companion object {
        val zero: Resource = Resource(0L, 0)

        fun list(owner: Owner, restype: Int? = null, orderBy: String? = null, limit: Int? = null, offset: Int? = null): List<XRole> {
            val w: Where = if (restype == null) owner.where else owner.where AND (XRole::restype EQ restype)
            return XRole.filter(w AND (XRole::resid GT 0L)).list(orderBy ?: XRole::resid.ASC, limit = limit, offset = offset)
        }
    }

}