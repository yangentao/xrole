@file:Suppress("unused")

package io.github.yangentao.xrole

/// 组(实体或部门)的字类型, 是否可以创建子组
@Suppress("unused", "ConstPropertyName")
object Subtype {
    const val none: Int = 0
    const val leaf: Int = 1 //叶子, 不可以再创建下级 组/实体
    const val nodeLeaf: Int = 2 //可以创建叶子 组/实体
    const val node: Int = 4 //可以创建LeafNode/Leaf
    const val root: Int = 8 //可以创建Node/LeafNode/Leaf
}

val Int.isValidSubtype: Boolean get() = this in 0..8


