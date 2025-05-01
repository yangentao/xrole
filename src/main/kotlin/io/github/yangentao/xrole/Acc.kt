package io.github.yangentao.xrole

import io.github.yangentao.sql.ModelInsertResult
import io.github.yangentao.sql.clause.*
import io.github.yangentao.sql.filter
import io.github.yangentao.sql.list
import io.github.yangentao.sql.listOrm
import io.github.yangentao.sql.update
import io.github.yangentao.xrole.XRole.Companion.RoleJoinGroup
import kotlin.math.min

/**
 * @param idValue Account ID
 */
@JvmInline
value class Acc(val idValue: Long) {
    val entityID: Long get() = entity.id
    val entity: GroupRole
        get() {
            val gr = GroupRole.one(GroupRole::aid EQ idValue, GroupRole::eid EQ 0L, Res.Companion.zero.whereGroupRole)
            assert(gr != null)
            return gr!!
        }

    fun isDevAdmin(res: Res): Boolean {
        return roleOnRes(res) == RoleValue.admin;
    }

    fun isGroupAdmin(gid: Long): Boolean {
        return roleOnGroup(gid) == RoleValue.admin;
    }

    fun roleOnGroup(gid: Long): Int? {
        if (isAdminRoot) return RoleValue.admin
        val group = XGroup.oneByKey(gid) ?: return null
        if (group.isEntity) {
            return XRole.filter(XRole::aid EQ idValue, XRole::gid EQ gid, Res.Companion.zero.whereRole).one()?.rolevalue
        }
        val eRole = XRole.filter(XRole::aid EQ idValue, XRole::gid EQ group.eid, Res.Companion.zero.whereRole).one()?.rolevalue
        val dRole = XRole.filter(XRole::aid EQ idValue, XRole::gid EQ gid, Res.Companion.zero.whereRole).one()?.rolevalue
        return maxRole(eRole, dRole)
    }

    fun roleOnRes(res: Res): Int? {
        if (isAdminRoot) return RoleValue.admin
        // 1. 指定了资源
        val sp: Int? = XRole.filter(XRole::aid EQ idValue, res.whereRole).list().maxOfOrNull { it.rolevalue }
        if (sp != null) return sp

        // 2. 所属组.  TODO 自连接
        val accGroups = groups()
        val resGroups = res.groups()
        val rList = ArrayList<Int>()
        for (a in accGroups) {
            val r = resGroups.firstOrNull { it.gid == a.gid } ?: continue
            rList += min(a.rolevalue, r.rolevalue)
        }
        rList.maxOrNull()?.also { return it }

        // entity 交集
        val accEntities = entities()
        val resEntities: Set<Long> = res.entities()
        val cross = accEntities.filter { it.gid in resEntities }
        if (cross.isNotEmpty()) return cross.maxBy { it.rolevalue }.rolevalue

        // 上级entity
        val superResEntities: HashSet<Long> = HashSet()
        for (eid in resEntities) {
            superResEntities.addAll(Ent(eid).parents(false))
        }
        if (superResEntities.isEmpty()) return null

        val accEs: List<XRole> = accEntities.filter { it.gid in superResEntities }
        val maxRole = accEs.maxOfOrNull { it.rolevalue } ?: return null
        // 只有管理员可操作下级实体的资源
        return if (maxRole == RoleValue.admin) RoleValue.admin else null
    }

    fun addTo(gid: Long, role: Int): ModelInsertResult<XRole> {
        return XRole.Companion.upsert {
            it.aid = this@Acc.idValue
            it.gid = gid
            it.res = Res.Companion.zero
            it.rolevalue = role
        }
    }

    fun entitiesDirect(): List<XRole> {
        return RoleJoinGroup.WHERE(XRole::aid EQ idValue, XRole::gid GT 0L, XGroup::eid EQ 0L, Res.Companion.zero.whereRole).query<XRole>().listOrm()
    }

    fun entities(): List<XRole> {
        return RoleJoinGroup.WHERE(XRole::aid EQ idValue, XRole::gid GT 0L, XGroup::eid EQ 0L, Res.Companion.zero.whereRole).query<XRole>().listOrm()
    }

    fun depts(eid: Long): List<XRole> {
        return RoleJoinGroup.WHERE(XRole::aid EQ idValue, XRole::gid GT 0L, XGroup::eid EQ eid, Res.Companion.zero.whereRole).query<XRole>().listOrm()
    }

    fun groups(): List<XRole> {
        return XRole.filter(XRole::aid EQ idValue, XRole::gid GT 0L, Res.Companion.zero.whereRole).list()
    }

    fun groupEntities(): List<GroupRole> {
        return GroupRole.list(
            Res.Companion.zero.whereGroupRole,
            GroupRole::gid GT 0L, GroupRole::eid EQ 0L,
            GroupRole::aid EQ this.idValue
        )
    }

    val rootEntity: GroupRole?
        get() {
            return this.groupEntities().firstOrNull { it.pid == 0L }
        }
    val isAdminRoot: Boolean
        get() {
            val a = GroupRole.filter(GroupRole::aid EQ idValue, GroupRole::gid GT 0L, GroupRole::pid EQ 0, GroupRole::eid EQ 0L, Res.Companion.zero.whereGroupRole).one()?.rolevalue
            return a == RoleValue.admin
        }
}
