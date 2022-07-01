package com.example.model

class Player(val id:Long) {
    /**
     * 玩家状态类
     */
    enum class PlayerStatues{
        NotInGame, // 默认状态 未在游戏中,可能是中途退出了
        InRoom, // 在房间中，但游戏还未开始
        Waiting, // 等待其他人行动
        OnTerm, // 我的回合！(移动前)
        Moved, // 移动之后 (还有一步操作)
    }

    /**
     * 冻结剩余时间
     */
    var freezeLeft = 0

    /**
     * 玩家状态
     */
    var state = PlayerStatues.NotInGame

    /**
     * 现金
     */
    var cash = 0

    /**
     * 存款
     */
    var BankSaving = 0

    /**
     * 记录数组 玩家拥有的土地方格的下标
     */
    var landList:IntArray = IntArray(0)

    /**
     * 玩家地图位置下标
     */
    var position = 0

    /**
     * 玩家是否在房间中准备
     */
    fun isReady():Boolean{
        return  this.state == PlayerStatues.Waiting
    }

    /**
     * 玩家状态文本
     */
    override fun toString():String{
        val strBuilder:StringBuilder = StringBuilder("""
            状态：【状态】【是否房主】
            现金：￥${cash.toString()}
            存款：￥${BankSaving.toString()}
            位置：【位置】
        """.trimIndent())
        val strState:String = when(state){
                PlayerStatues.Waiting->{ "等待中" }
                PlayerStatues.OnTerm->{ "正在行动" }
                else->{ "未在游戏" }
            }
        return strBuilder.toString().replace("【状态】",strState)
    }

    /**
     * 为玩家加钱（现金）
     */
    fun addCash(c:Int){
        cash += c
    }

    /**
     * 加存款
     */
    fun addBankSaving(s:Int){
        BankSaving += s
    }

    /**
     * 存钱
     * @return 操作是否成功
     */
    fun save(c:Int):Boolean{
        return if(cash < c){
            false
        }else{
            cash -= c
            BankSaving += c
            true
        }
    }

    /**
     * @return 玩家是否在行动回合
     */
    fun isOnterm():Boolean{
        return state==PlayerStatues.OnTerm
    }

    /**
     * 将玩家状态置为Moved
     */
    fun toMovedState() {
        state = PlayerStatues.Moved
    }

    /**
     * 移动玩家位置到...
     */
    fun moveTo(pos:Int){
        position = pos
    }

    /**
     * 尝试唤醒玩家，将玩家从等待中唤醒
     */
    fun wake(){
        freezeLeft -= 1
        if (freezeLeft < 0){
            state = PlayerStatues.OnTerm
            freezeLeft = 0
        }
    }

    /**
     * 沉睡玩家
     */
    fun sleep(){
        state = PlayerStatues.Waiting
    }

    /**
     * 受否处于后移动状态
     */
    fun isMoved():Boolean{
        return state == PlayerStatues.Moved
    }

    /**
     * 购买土地(存款优先)
     * @return 操作是否成功
     */
    fun buyLand(landPos:Int, cost:Int):Boolean{
        val payingResult = payBankSavingFirst(cost)
        if (payingResult){ landList = landList.plus(landPos) }
        return payingResult
    }

    /**
     * 支出存款(存款优先)
     * @return 操作是否成功
     */
    fun payBankSavingFirst(cost:Int):Boolean{
        val all = getMoney()
        return if (all < cost){
            false
        }else{
            if (BankSaving >= cost)
                BankSaving -= cost
            else {
                cash -= (cost-BankSaving)
                BankSaving = 0
            }
            true
        }
    }

    /**
     * 向玩家支付过路费（this.cash->others.BankSaving）
     *
     * Money: 支付金额
     * @receiver 接收方
     * @return 是否破产
     */
    fun payToBankSaving(money:Int, receiver:Player):Boolean{
        receiver.addBankSaving(money)
        return payCashFirst(money)
    }

    /**
     * 从银行取钱
     * @return 操作是否成功
     */
    fun takeFromBank(money: Int):Boolean{
        return if (money <= BankSaving){
            BankSaving -= money
            addCash(money)
            true
        }else{ false }
    }

    /**
     * 付钱/损失钱（现金优先）
     * @return 操作是否成功
     */
    fun payCashFirst(money: Int):Boolean{
        val allMoney = getMoney()
        return if (allMoney < money){
            false
        }else{
            if(cash >= money){
                cash -= money
            }else{
                cash=0
                BankSaving = allMoney - money
            }
            true
        }
    }

    /**
     * 付钱给其他玩家（this.cash->others.cash）
     */
    fun payToCashFirst(otherPlayer:Player, money:Int):Boolean{
        otherPlayer.addCash(money)
        return payCashFirst(money)
    }

    /**
     * 返回玩家当前全部资金
     * @return 玩家全部资金
     */
    fun getMoney():Int{
        return BankSaving+cash
    }
}