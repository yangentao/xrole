package  io.github.yangentao.xrole

import io.github.yangentao.sql.clause.AND
import io.github.yangentao.sql.clause.EQ
import io.github.yangentao.sql.clause.Where

open class Owner(val gid: Long, val aid: Long) {
    val whereOwner: Where = XRole::gid EQ gid AND (XRole::aid EQ aid)
    val isEmpty: Boolean get() = gid == 0L && aid == 0L
    val isGroup: Boolean get() = aid == 0L && gid > 0L
    val isAccount: Boolean get() = gid == 0L && aid > 0L

    fun listResource(restype: Int? = null, orderBy: String? = null, limit: Int? = null, offset: Int = 0): List<XRole> {
        return Resource.list(this, restype, orderBy, limit, offset)
    }

    fun findResource(res: Resource): XRole? {
        return res.find(this)
    }

    fun saveResource(res: Resource, role: Int): Boolean {
        return res.assign(this, role)
    }

    fun removeResource(res: Resource): Int {
        return res.delete(this)
    }

    override fun equals(other: Any?): Boolean {
        if (other is Owner) return this.gid == other.gid && this.aid == other.aid
        return false
    }

    override fun hashCode(): Int {
        return this.gid.hashCode() + this.aid.hashCode()
    }

    companion object {
        val zero: Owner = Owner(0L, 0L)

        fun group(gid: Long): Owner = Owner(gid, 0L)
        fun account(aid: Long): Owner = Owner(0L, aid)
    }
}

class GroupOwner(gid: Long) : Owner(gid, 0L)
class AccOwner(aid: Long) : Owner(0L, aid)


