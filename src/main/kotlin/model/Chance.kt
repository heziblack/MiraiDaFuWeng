package com.example.model

class Chance(
    val tittle:String,
    val Condition:String,
    val host:HostCondition=HostCondition.Self,
    val guest:GuestCondition=GuestCondition.None,
    val action:Action = Action.Pay,
    val argNum:Double = 0.0,
) {
    enum class HostCondition{
        Self, // 自己
        MostCash, // 现金最多
        LeastCash, // 现金最少
        MostBuilding, // 建筑最多
        LeastBuilding, // 建筑最少
        MostSaving, // 存款最多
        LeastSaving, // 存款最少
        Others, // 除自己外
        All // 所有人
    }
    enum class GuestCondition{
        None, // 无客体
        MostCash, // 现金最多
        LeastCash, // 现金最少
        MostBuilding, // 建筑最多
        LeastBuilding, // 建筑最少
        MostSaving, // 存款最多
        LeastSaving, // 存款最少
        Others, // 除主体外的人
        All // 所有人
    }
    enum class Action{
        Pay, // 失去/支付
        Get, // 收取/索要
        Move, // 移动
        Upgrade, // 升级
        Freeze // 冻结
    }
}