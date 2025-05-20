package io.github.yangentao.xrole

import io.github.yangentao.anno.Label
import io.github.yangentao.anno.ModelField
import io.github.yangentao.sql.TableModel
import io.github.yangentao.sql.TableModelClass

/**
 * 对资源的控制权限
 * 当 [resid] 是0的时候, 表示账号[aid]在群组[gid]中的角色
 * 当 [aid] 是0的时候, 表示群组[gid]对资源[resid] 的访问权限
 * 当 [gid] 是0的时候, 表示[aid] 单独对资源[resid] 的访问权限
 * 当 [aid] 和 [gid] 都是 0 的时候, 表示 [resid] 的默认访问权限
 */
class XRole : TableModel() {
    //group id
    @ModelField(primaryKey = true, autoInc = 100)
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

    companion object : TableModelClass<XRole>()

}

