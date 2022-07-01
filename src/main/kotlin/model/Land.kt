package com.example.model

import com.beust.klaxon.Json

data class Land constructor(
    // 地名
    var name:String = "中国某地",
    // 类型
    var type:Type = Type.Land,
    // 地价
    var landPrice:Int = 0,
    // 建设花费
    var buildCost:Array<Int> = arrayOf<Int>(0, 0),
    // 过路费
    var passCost:Array<Int> = arrayOf<Int>(0, 0, 0),
    // 文字描述
    var description:String = ""
    ) {
    /**
     * 地块类型类
     */
    enum class Type{
        StartPoint, // 出发点
        Chance, // 随机事件
        Prison, // 监狱
        Land // 普通地块
    }
    /**
     * 地块等级类
     */
    enum class Levels{
        L1,
        L2,
        L3
    }

    /**
     * 土地所有者
     */
    var owner:Long = -1

    /**
     * 地块等级
     */
    var level:Levels = Levels.L1

    /**
     * 地块是否有主人
     */
    fun hasBeenOwned():Boolean{
        return owner != -1L
    }

    /**
     * 返回地块是否为玩家所有
     */
    fun isOwner(id:Long):Boolean{
        return id==owner
    }

    /**
     * 返回过路费
     */
    fun getPassCost():Int{
        return when(level){
            Levels.L1->passCost[0]
            Levels.L2->passCost[1]
            Levels.L3->passCost[2]
        }
    }

    /**
     * 返回升级需要的花费
     * @return -1代表已经升到顶级
     */
    fun getUpgradeCost():Int{
        return when(level){
            Levels.L1-> buildCost[0]
            Levels.L2-> buildCost[1]
            Levels.L3-> -1
        }
    }

    /**
     * 升级土地
     */
    fun upgrade(){
        level = when(level){
            Levels.L1->Levels.L2
            Levels.L2->Levels.L3
            Levels.L3->Levels.L3
        }
    }

    /**
     * 受否被抵押
     */
    var isMortgaged:Boolean = false

    /**
     * 抵押土地价格
     */
    val mortgagePrice:Int
        get() {
            return when(level){
                Levels.L1-> (landPrice * 0.8).toInt()
                Levels.L2-> ((landPrice + buildCost[0]) * 0.8).toInt()
                Levels.L3-> ((landPrice + buildCost[0]+buildCost[1]) * 0.7).toInt()
            }
        }

}