package  io.github.yangentao.xrole

import io.github.yangentao.sql.clause.*
import io.github.yangentao.sql.filter

open class MemberShip(val gid: Long, val aid: Long) {
    private val where: Where = XRole::gid EQ gid AND (XRole::aid EQ aid) AND Resource.zero.where
    val isEmpty: Boolean = gid == 0L && aid == 0L

    fun roleValue(): Int? {
        val group = XGroup.oneByKey(gid) ?: return null
        val r = MemberShip(gid, aid).find()?.rolevalue
        if (group.isEntity) {
            return r
        }
        val eRole = MemberShip(group.eid, aid).find()?.rolevalue
        return maxRole(r, eRole)// TODO merge bits?
    }

    fun exist(): Boolean {
        if (isEmpty) error("Empty memeber")
        return XRole.filter(where).exists()
    }

    fun find(): XRole? {
        if (isEmpty) error("Empty memeber")
        return XRole.filter(where).one()
    }

    fun remove(): Int {
        if (isEmpty) error("Empty memeber")
        return XRole.filter(where).delete()
    }

    fun save(role: Int): Boolean {
        if (isEmpty) error("Empty memeber")
        val r = XRole.upsert(XRole::gid to gid, XRole::aid to aid, XRole::resid to 0L, XRole::restype to 0, XRole::rolevalue to role)
        return r.success
    }

    companion object {

        fun list(gid: Long, orderBy: String?, limit: Int? = null, offset: Int? = null): List<XRole> {
            return XRole.filter(XRole::gid EQ gid, XRole::aid GT 0L, Resource.zero.where).list(orderBy ?: XRole::aid.ASC, limit = limit, offset = offset)
        }
    }
}